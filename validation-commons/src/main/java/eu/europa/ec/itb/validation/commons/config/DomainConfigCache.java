package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.*;
import eu.europa.ec.itb.validation.plugin.PluginInfo;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class DomainConfigCache <T extends DomainConfig> {

    private static Logger logger = LoggerFactory.getLogger(DomainConfigCache.class);

    @Autowired
    private ApplicationConfig appConfig = null;

    private ConcurrentHashMap<String, T> domainConfigs = new ConcurrentHashMap<>();
    private final T undefinedDomainConfig;
    private ExtensionFilter propertyFilter = new ExtensionFilter(".properties");

    public DomainConfigCache() {
        undefinedDomainConfig = newDomainConfig();
        undefinedDomainConfig.setDefined(false);
    }

    protected abstract T newDomainConfig();
    protected abstract ValidatorChannel[] getSupportedChannels();

    @PostConstruct
    public void initBase() {
        getAllDomainConfigurations();
        init();
    }

    protected void init() {
    }

    protected ValidatorChannel[] getDefaultChannels() {
        return getSupportedChannels();
    }

    private String getDefaultChannelsStr() {
        StringBuilder str = new StringBuilder();
        ValidatorChannel[] defaultChannels = getDefaultChannels();
        if (defaultChannels != null && defaultChannels.length > 0) {
            for (ValidatorChannel channel: defaultChannels) {
                str.append(channel.getName()).append(',');
            }
            str.deleteCharAt(str.length() - 1);
        }
        return str.toString();
    }

    public List<T> getAllDomainConfigurations() {
        List<T> configs = new ArrayList<>();
        for (String domain: appConfig.getDomain()) {
            T domainConfig = getConfigForDomain(domain);
            if (domainConfig != null && domainConfig.isDefined()) {
                configs.add(domainConfig);
            }
        }
        return configs;
    }

    public T getConfigForDomainName(String domainName) {
        T config = getConfigForDomain(appConfig.getDomainNameToDomainId().getOrDefault(domainName, ""));
        if (config == null) {
            logger.warn("Invalid domain name ["+domainName+"].");
        }
        return config;
    }

    public T getConfigForDomain(String domain) {
        T domainConfig = domainConfigs.get(domain);
        if (domainConfig == null) {
            String[] files = Paths.get(appConfig.getResourceRoot(), domain).toFile().list(propertyFilter);
            if (files == null || files.length == 0) {
                domainConfig = undefinedDomainConfig;
            } else {
                try {
                    CompositeConfiguration config = new CompositeConfiguration();
                    for (String file: files) {
                        Parameters params = new Parameters();
                        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                                        .configure(params.properties().setFile(Paths.get(appConfig.getResourceRoot(), domain, file).toFile()));
                        try {
                            config.addConfiguration(builder.getConfiguration());
                        } catch (ConfigurationException e) {
                            throw new IllegalStateException("Unable to load property file ["+file+"]", e);
                        }
                    }
                    domainConfig = newDomainConfig();
                    domainConfig.setDefined(true);
                    domainConfig.setDomain(domain);
                    domainConfig.setDomainName(appConfig.getDomainIdToDomainName().get(domain));
                    domainConfig.setType(Arrays.stream(StringUtils.split(config.getString("validator.type"), ',')).map(String::trim).collect(Collectors.toList()));
                    Set<ValidatorChannel> supportedChannels = new HashSet<>(Arrays.asList(getSupportedChannels()));
                    domainConfig.setChannels(Arrays.stream(StringUtils.split(config.getString("validator.channels", getDefaultChannelsStr()), ',')).map(String::trim).map((name) -> toValidatorChannel(supportedChannels, name)).collect(Collectors.toSet()));
                    // Parse plugins - start
                    Path domainRootPath = Paths.get(appConfig.getResourceRoot(), domainConfig.getDomain());
                    Function<Map<String, String>, PluginInfo> pluginConfigMapper = (Map<String, String> values) -> {
                        if (!values.containsKey("jar") || !values.containsKey("class")) {
                            throw new IllegalStateException("Invalid plugin configuration. Each element must include [jar] and [class] properties");
                        }
                        PluginInfo info = new PluginInfo();
                        info.setJarPath(domainRootPath.resolve(values.get("jar")));
                        info.setPluginClasses(Arrays.asList(Arrays.stream(StringUtils.split(values.get("class"), ",")).map(String::trim).toArray(String[]::new)));
                        return info;
                    };
                    domainConfig.setPluginDefaultConfig(parseValueList("validator.defaultPlugins", config, pluginConfigMapper));
                    domainConfig.setPluginPerTypeConfig(parseTypedValueList("validator.plugins", domainConfig.getType(), config, pluginConfigMapper));
                    // Parse plugins - end
                    // Allow subclasses to extend the configuration as needed.
                    addDomainConfiguration(domainConfig, config);
                    completeValidationArtifactConfig(domainConfig);
                    logger.info("Loaded configuration for domain ["+domain+"]");
                } catch (Exception e) {
                    // Make sure a domain's invalid configuration never fails the overall startup of the validator.
                    logger.warn("Failed to initialise configuration for domain ["+domain+"]", e);
                    domainConfig = null;
                } finally {
                    if (domainConfig == null) {
                        domainConfig = undefinedDomainConfig;
                    }
                    domainConfigs.put(domain, domainConfig);
                }
            }
        }
        return domainConfig;
    }

    protected void completeValidationArtifactConfig(T domainConfig) {
        if (domainConfig.getArtifactInfo() != null) {
            for (String validationType: domainConfig.getType()) {
                TypedValidationArtifactInfo artifactInfo = domainConfig.getArtifactInfo().get(validationType);
                if (!artifactInfo.hasPreconfiguredArtifacts() && !domainConfig.hasPlugins(validationType)) {
                    int artifactTypeCount = artifactInfo.getTypes().size();
                    if (artifactTypeCount == 1) {
                        if (artifactInfo.get().getExternalArtifactSupport() != ExternalArtifactSupport.REQUIRED) {
                            logger.warn("Domain ["+domainConfig.getDomainName()+"] defines no preconfigured validation artifacts for validation type ["+validationType+"] but also doesn't require externally provided artifacts. Forcing external artifacts as required.");
                            artifactInfo.get().setExternalArtifactSupport(ExternalArtifactSupport.REQUIRED);
                        }
                    } else if (artifactTypeCount > 1) {
                        if (artifactInfo.getOverallExternalArtifactSupport() == ExternalArtifactSupport.NONE) {
                            logger.warn("Domain ["+domainConfig.getDomainName()+"] defines no preconfigured validation artifacts for validation type ["+validationType+"] but also doesn't expect externally provided artifacts. At least one of the defined artifact types should be set with optional external artifacts.");
                        }
                    }
                }
            }
        }
    }

    protected void addDomainConfiguration(T domainConfig, Configuration config) {
        // Do nothing by default.
    }

    protected ValidatorChannel toValidatorChannel(Set<ValidatorChannel> supportedChannels, String channelName) {
        ValidatorChannel channel = ValidatorChannel.byName(channelName);
        if (!supportedChannels.contains(channel)) {
            throw new IllegalStateException("Unsupported validator channel ["+channelName+"]");
        }
        return channel;
    }

    protected <R> List<R> parseValueList(String key, Configuration config, Function<Map<String, String>, R> fnMapper) {
        List<R> values = new ArrayList<>();
        Iterator<String> it = config.getKeys(key);
        Set<String> processedIndexes = new HashSet<>();
        while (it.hasNext()) {
            String typedKey = it.next();
            String index = typedKey.replaceAll("(" + key + ".)([0-9]{1,})(.[a-zA-Z]*)", "$2");
            if (!processedIndexes.contains(index)) {
                processedIndexes.add(index);
                Map<String, String> propertiesToMap = new HashMap<>();
                Iterator<String> it2 = config.getKeys(key + "." + index);
                while (it2.hasNext()) {
                    String specificProperty = it2.next();
                    String configToMap = specificProperty.substring(specificProperty.lastIndexOf('.')+1);
                    propertiesToMap.put(configToMap, config.getString(specificProperty));
                }
                values.add(fnMapper.apply(propertiesToMap));
            }
        }
        return values;
    }

    protected <R> Map<String, List<R>> parseTypedValueList(String key, List<String> types, Configuration config, Function<Map<String, String>, R> fnMapper) {
        Map<String, List<R>> configValues = new HashMap<>();
        for (String type: types) {
            configValues.put(type, parseValueList(key + "." + type, config, fnMapper));
        }
        return configValues;
    }

    protected Map<String, Boolean> parseBooleanMap(String key, Configuration config, List<String> types) {
        Map<String, Boolean> map = new HashMap<>();
        for (String type: types) {
            boolean value = false;

            try {
                value = config.getBoolean(key+"."+type);
            }catch(Exception e){
                value = false;
            }
            finally {
                map.put(type, value);
            }
        }
        return map;
    }

    protected <R extends Enum<R>> Map<String, R> parseEnumMap(String key, Class<R> enumType, R defaultValue, Configuration config, List<String> types) {
        Map<String, R> map = new HashMap<>();
        for (String type: types) {
            map.put(type, R.valueOf(enumType, config.getString(key+"."+type, defaultValue.name())));
        }
        return map;
    }

    protected Map<String, String> parseMap(String commonKey, Configuration config) {
        Map<String, String> map = new HashMap<>();
        Iterator<String> mapKeys = config.getKeys(commonKey);
        while (mapKeys.hasNext()) {
            String fullKey = mapKeys.next();
            String extensionKey = StringUtils.substringAfter(fullKey, commonKey+".");
            map.put(extensionKey, StringUtils.defaultString(config.getString(fullKey)).trim());
        }
        return map;
    }

    protected void addMissingDefaultValues(Map<String, String> map, Map<String, String> defaultValues) {
        if (defaultValues != null) {
            for (Map.Entry<String, String> defaultEntry: defaultValues.entrySet()) {
                if (!map.containsKey(defaultEntry.getKey())) {
                    map.put(defaultEntry.getKey(), defaultEntry.getValue());
                }
            }
        }
    }

    protected Map<String, String> parseMap(String commonKey, Configuration config, Map<String, String> defaultValues) {
        Map<String, String> map = new HashMap<>();
        Iterator<String> mapKeys = config.getKeys(commonKey);
        while (mapKeys.hasNext()) {
            String fullKey = mapKeys.next();
            String extensionKey = StringUtils.substringAfter(fullKey, commonKey+".");
            map.put(extensionKey, StringUtils.defaultString(config.getString(fullKey)).trim());
        }
        // Add any missing default.
        addMissingDefaultValues(map, defaultValues);
        return map;
    }

    protected Map<String, String> parseMap(String key, Configuration config, List<String> subKeys) {
        Map<String, String> map = new HashMap<>();
        for (String subKey: subKeys) {
            String val = config.getString(key+"."+subKey, null);
            if (val != null) {
                map.put(subKey, config.getString(key+"."+subKey).trim());
            }
        }
        return map;
    }

    protected void addValidationArtifactInfoForType(String artifactType, String rootKey, String externalRootKey, String externalCombinationKey, DomainConfig domainConfig, Configuration config) {
        if (domainConfig.getArtifactInfo() == null) {
            domainConfig.setArtifactInfo(new HashMap<>());
        }
        for (String validationType: domainConfig.getType()) {
            String typeKey = rootKey + "." + validationType;
            ValidationArtifactInfo info = new ValidationArtifactInfo();
            // Lookup local artifact info.
            info.setLocalPath(StringUtils.trim(config.getString(typeKey)));
            info.setType(StringUtils.trim(config.getString(typeKey+".type")));
            info.setPreProcessorPath(StringUtils.trim(config.getString(typeKey+".preprocessor")));
            info.setPreProcessorPath(StringUtils.trim(config.getString(typeKey+".preprocessor.output")));
            info.setRemoteArtifacts(new ArrayList<>());
            // Lookup remote artifact info.
            int remoteCounter = 0;
            while (config.containsKey(typeKey+".remote."+remoteCounter+".url")) {
                RemoteValidationArtifactInfo remoteInfo = new RemoteValidationArtifactInfo();
                remoteInfo.setUrl(config.getString(StringUtils.trim(typeKey+".remote."+remoteCounter+".url")));
                remoteInfo.setType(StringUtils.trim(config.getString(typeKey+".remote."+remoteCounter+".type")));
                remoteInfo.setPreProcessorPath(StringUtils.trim(config.getString(typeKey+".remote."+remoteCounter+".preprocessor")));
                remoteInfo.setPreProcessorOutputExtension(StringUtils.trim(config.getString(typeKey+".remote."+remoteCounter+".output")));
                info.getRemoteArtifacts().add(remoteInfo);
                remoteCounter += 1;
            }
            // Lookup external (user-provided) artifact support.
            if (externalRootKey == null) {
                info.setExternalArtifactSupport(ExternalArtifactSupport.NONE);
                info.setExternalArtifactCombinationApproach(ValidationArtifactCombinationApproach.ALL);
            } else {
                String externalArtifactConfigValue = StringUtils.trim(config.getString(externalRootKey+"."+validationType, ExternalArtifactSupport.NONE.getName()));
                // Handle boolean-based config.
                if (externalArtifactConfigValue.equalsIgnoreCase("true")) {
                    info.setExternalArtifactSupport(ExternalArtifactSupport.OPTIONAL);
                } else if (externalArtifactConfigValue.equalsIgnoreCase("false")) {
                    info.setExternalArtifactSupport(ExternalArtifactSupport.NONE);
                } else {
                    info.setExternalArtifactSupport(ExternalArtifactSupport.byName(externalArtifactConfigValue));
                }
                info.setExternalArtifactPreProcessorPath(StringUtils.trim(config.getString(externalRootKey+"."+validationType+".preprocessor")));
                info.setExternalArtifactPreProcessorOutputExtension(StringUtils.trim(config.getString(externalRootKey+"."+validationType+".preprocessor.output")));
            }
            // Artifact combination approach.
            info.setArtifactCombinationApproach(ValidationArtifactCombinationApproach.byName(StringUtils.trim(config.getString(typeKey+".combinationApproach", ValidationArtifactCombinationApproach.ALL.getName()))));
            if (externalCombinationKey == null || externalRootKey == null) {
                info.setExternalArtifactCombinationApproach(ValidationArtifactCombinationApproach.ALL);
            } else {
                info.setExternalArtifactCombinationApproach(ValidationArtifactCombinationApproach.byName(StringUtils.trim(config.getString(externalCombinationKey+"."+validationType, ValidationArtifactCombinationApproach.ALL.getName()))));
            }
            if (!domainConfig.getArtifactInfo().containsKey(validationType)) {
                domainConfig.getArtifactInfo().put(validationType, new TypedValidationArtifactInfo());
            }
            domainConfig.getArtifactInfo().get(validationType).add(StringUtils.defaultString(artifactType, TypedValidationArtifactInfo.DEFAULT_TYPE), info);
        }
    }

    protected void addValidationArtifactInfo(String rootKey, String externalRootKey, String externalCombinationKey, DomainConfig domainConfig, Configuration config) {
        addValidationArtifactInfoForType(null, rootKey, externalRootKey, externalCombinationKey, domainConfig, config);
    }

    private static class ExtensionFilter implements FilenameFilter {

        private String ext;

        ExtensionFilter(String ext) {
            this.ext = ext;
        }

        public boolean accept(File dir, String name) {
            return (name.endsWith(ext));
        }
    }

}
