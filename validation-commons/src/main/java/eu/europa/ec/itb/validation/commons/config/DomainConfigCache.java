package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.*;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.plugin.PluginInfo;
import org.apache.commons.configuration2.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Superclass for the loading and storing of domain configuration instances.
 *
 * @param <T> The type of domain configuration class.
 */
public abstract class DomainConfigCache <T extends DomainConfig> {

    private static final String DEFAULT_FILE_ENCODING = "UTF-8";
    private static final Logger logger = LoggerFactory.getLogger(DomainConfigCache.class);

    @Autowired
    ApplicationConfig appConfig = null;

    private final ConcurrentHashMap<String, T> domainConfigs = new ConcurrentHashMap<>();
    private final T undefinedDomainConfig;
    private final ExtensionFilter propertyFilter = new ExtensionFilter(".properties");

    /**
     * Constructor.
     */
    public DomainConfigCache() {
        undefinedDomainConfig = newDomainConfig();
        undefinedDomainConfig.setDefined(false);
    }

    /**
     * @return A newly created and empty domain configuration instance.
     */
    protected abstract T newDomainConfig();

    /**
     * @return The set of supported channels for this validator.
     */
    protected abstract ValidatorChannel[] getSupportedChannels();

    /**
     * Initialisation method to load all domain configurations.
     *
     * Additional customisations can be included by overriding the init() method.
     */
    @PostConstruct
    public void initBase() {
        getAllDomainConfigurations();
        init();
    }

    /**
     * Extension point (by default empty) to do additional configuration if needed.
     */
    protected void init() {
    }

    /**
     * @return The default set of supported channels for this type of validator.
     */
    protected ValidatorChannel[] getDefaultChannels() {
        return getSupportedChannels();
    }

    /**
     * @return A text representation of the supported validator channels.
     */
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

    /**
     * Initialise (once) and return all domain configurations for this validator.
     *
     * @return The domain configurations.
     */
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

    /**
     * Get a domain configuration by its public name.
     *
     * @param domainName The domain's name.
     * @return The domain configuration.
     */
    public T getConfigForDomainName(String domainName) {
        T config = getConfigForDomain(appConfig.getDomainNameToDomainId().getOrDefault(domainName, ""));
        if (config == null) {
            logger.warn("Invalid domain name ["+domainName+"].");
        }
        return config;
    }

    /**
     * Get a domain configuration by its identifier (folder name).
     *
     * @param domain The domain identifier.
     * @return The domain configuration.
     */
    public T getConfigForDomain(String domain) {
        T domainConfig = domainConfigs.get(domain);
        if (domainConfig == null) {
            String[] files = Paths.get(appConfig.getResourceRoot(), domain).toFile().list(propertyFilter);
            if (files == null || files.length == 0) {
                domainConfig = undefinedDomainConfig;
            } else {
                try {
                    CompositeConfiguration config = new CompositeConfiguration();
                    // 1. Load from system properties.
                    config.addConfiguration(new SystemConfiguration());
                    // 2. Load from environment variables.
                    config.addConfiguration(new EnvironmentConfiguration());
                    // 3. Load from property file(s).
                    for (String file: files) {
                        addConfigurationFromFile(Paths.get(appConfig.getResourceRoot(), domain, file), config);
                    }
                    importAdditionalProperties(config, domain);
                    
                    domainConfig = newDomainConfig();
                    domainConfig.setDefined(true);
                    domainConfig.setDomain(domain);
                    domainConfig.setDomainName(appConfig.getDomainIdToDomainName().get(domain));

                    List<String> declaredValidationTypes = Arrays.stream(StringUtils.split(config.getString("validator.type"), ',')).map(String::trim).collect(Collectors.toList());
                    Map<String, List<String>> validationTypeOptions = new HashMap<>();
                    for (Map.Entry<String,String> entry: parseMap("validator.typeOptions", config, declaredValidationTypes).entrySet()) {
                        validationTypeOptions.put(entry.getKey(), Arrays.stream(StringUtils.split(entry.getValue(), ',')).map(String::trim).collect(Collectors.toList()));
                    }
                    List<String> validationTypes;
                    if (validationTypeOptions.isEmpty()) {
                        validationTypes = declaredValidationTypes;
                    } else {
                        validationTypes = new ArrayList<>();
                        for (String validationType: declaredValidationTypes) {
                            if (validationTypeOptions.containsKey(validationType)) {
                                for (String option: validationTypeOptions.get(validationType)) {
                                    validationTypes.add(validationType + "." + option);
                                }
                            } else {
                                validationTypes.add(validationType);
                            }
                        }
                    }
                    domainConfig.setType(validationTypes);
                    domainConfig.setDeclaredType(declaredValidationTypes);
                    domainConfig.setValidationTypeOptions(validationTypeOptions);
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
                    // Maximum report thresholds - start
                    long defaultMaximumReportsForDetailsOutput = 5000L;
                    long defaultMaximumReportsForXmlOutput = 50000L;
                    domainConfig.setMaximumReportsForDetailedOutput(config.getLong("validator.maximumReportsForDetailedOutput", defaultMaximumReportsForDetailsOutput));
                    if (domainConfig.getMaximumReportsForDetailedOutput() < 0) {
                        domainConfig.setMaximumReportsForDetailedOutput(defaultMaximumReportsForDetailsOutput);
                    }
                    domainConfig.setMaximumReportsForXmlOutput(config.getLong("validator.maximumReportsForXmlOutput", defaultMaximumReportsForXmlOutput));
                    if (domainConfig.getMaximumReportsForXmlOutput() < 0) {
                        domainConfig.setMaximumReportsForXmlOutput(defaultMaximumReportsForXmlOutput);
                    }
                    if (domainConfig.getMaximumReportsForXmlOutput() < domainConfig.getMaximumReportsForDetailedOutput()) {
                        domainConfig.setMaximumReportsForXmlOutput(domainConfig.getMaximumReportsForDetailedOutput());
                    }
                    // Maximum report thresholds - end
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

    /**
     * Import any additional properties from other property files (if allowed).
     *
     * @param config The loaded configuration properties.
     * @param domain The current domain's identifier.
     */
    protected void importAdditionalProperties(CompositeConfiguration config, String domain) {
    	String importPropertiesObject = config.getString("validator.importProperties", null);
    	if(importPropertiesObject == null) {
    		return;
    	}
    	String importPropertiesPath = importPropertiesObject;
    	if(appConfig.isRestrictResourcesToDomain() && ((new File(importPropertiesPath)).isAbsolute() || !isInDomainFolder(domain, importPropertiesPath))) {
   			throw new IllegalStateException("Resources are restricted to domain. Their paths should be relative to domain folder. Unable to load property file [" + importPropertiesPath + "]");
    	}else {
    		if(!(new File(importPropertiesPath)).isAbsolute()) {
    			importPropertiesPath = Paths.get(appConfig.getResourceRoot(), domain, importPropertiesPath).toAbsolutePath().toString();
    		}
    	}
        addConfigurationFromFile(Paths.get(importPropertiesPath), config);
    }

    /**
     * Load the configuration from the provided file.
     *
     * @param fileToAdd The file to load.
     * @param aggregateConfiguration The configuration to enrich.
     */
    private void addConfigurationFromFile(Path fileToAdd, CompositeConfiguration aggregateConfiguration) {
        PropertiesConfiguration propertiesConfig = new PropertiesConfiguration();
        FileHandler fileHandler = new FileHandler(propertiesConfig);
        fileHandler.setFile(fileToAdd.toFile());
        fileHandler.setEncoding(DEFAULT_FILE_ENCODING);
        try {
            fileHandler.load();
        } catch (ConfigurationException e) {
            throw new IllegalStateException("Unable to load property file [" + fileToAdd.toFile().getAbsolutePath() + "]", e);
        }
        aggregateConfiguration.addConfiguration(propertiesConfig);
    }

    /**
     * Check to see if the provided file path (relative or absolute) is within the given domain's folder.
     *
     * @param domain The domain's identifier (folder name).
     * @param localFile The file path to check.
     * @return True if the file is within the domain's folder (or one of its sub-folders).
     */
    public boolean isInDomainFolder(String domain, String localFile) {
    	Path domainRootPath = Paths.get(appConfig.getResourceRoot(), domain);
        Path localFilePath;
    	if (Path.of(localFile).isAbsolute()) {
            localFilePath = Path.of(localFile);
        } else {
            localFilePath = Paths.get(appConfig.getResourceRoot(), domain, localFile.trim());
        }
    	Path domainRootCanonicalPath;
    	Path localFileCanonicalPath;
		try {
			domainRootCanonicalPath = domainRootPath.toFile().getCanonicalFile().toPath();
			localFileCanonicalPath = localFilePath.toFile().getCanonicalFile().toPath();
			return localFileCanonicalPath.startsWith(domainRootCanonicalPath);
		} catch (IOException e) {
			throw new ValidatorException("Unable to find domain properties file" + localFile, e);
		}
    }

    /**
     * Complete the configuration of validation artifacts (aggregate flags) based on the loaded configuration.
     *
     * @param domainConfig The domain's configuration.
     */
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

    /**
     * Enrich the domain's configuration using the provided properties (does nothing by default).
     *
     * @param domainConfig The domain configuration to enrich.
     * @param config The configuration properties to consider.
     */
    protected void addDomainConfiguration(T domainConfig, Configuration config) {
        // Do nothing by default.
    }

    /**
     * Get the validator channel that corresponds to the provided name.
     *
     * @param supportedChannels The set of supported channels.
     * @param channelName The name of the channel to return.
     * @return The matched channel.
     * @throws IllegalStateException if no channel could be matched.
     */
    protected ValidatorChannel toValidatorChannel(Set<ValidatorChannel> supportedChannels, String channelName) {
        ValidatorChannel channel = ValidatorChannel.byName(channelName);
        if (!supportedChannels.contains(channel)) {
            throw new IllegalStateException("Unsupported validator channel ["+channelName+"]");
        }
        return channel;
    }

    /**
     * Parse a list of objects using a helper function.
     *
     * @param key The common key to consider.
     * @param config The configuration properties.
     * @param fnMapper The mapper function.
     * @param <R> The object type.
     * @return The list of objects.
     */
    protected <R> List<R> parseValueList(String key, Configuration config, Function<Map<String, String>, R> fnMapper) {
        List<R> values = new ArrayList<>();
        Iterator<String> it = config.getKeys(key);
        Set<String> processedIndexes = new HashSet<>();
        while (it.hasNext()) {
            String typedKey = it.next();
            String index = typedKey.replaceAll("(" + key + ".)([0-9]{1,})(.[a-zA-Z0-9]*)", "$2");
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

    /**
     * Parse a map of validation type to list of objects using a helper function.
     *
     * @param key The common property key.
     * @param types The validation types.
     * @param config The configuration properties.
     * @param fnMapper The mapper function.
     * @param <R> The type of object to return within the lists.
     * @return The map.
     */
    protected <R> Map<String, List<R>> parseTypedValueList(String key, List<String> types, Configuration config, Function<Map<String, String>, R> fnMapper) {
        Map<String, List<R>> configValues = new HashMap<>();
        for (String type: types) {
            configValues.put(type, parseValueList(key + "." + type, config, fnMapper));
        }
        return configValues;
    }

    /**
     * Parse a map of boolean values per validation type.
     *
     * @param key The common property key.
     * @param config The configuration properties.
     * @param types The validation types.
     * @param defaultIfMissing The default value for missing properties.
     * @return The map.
     */
    protected Map<String, Boolean> parseBooleanMap(String key, Configuration config, List<String> types, boolean defaultIfMissing) {
        Map<String, Boolean> map = new HashMap<>();
        for (String type: types) {
            boolean value;
            try {
                value = config.getBoolean(key+"."+type, defaultIfMissing);
            } catch (Exception e) {
                value = defaultIfMissing;
            }
            map.put(type, value);
        }
        return map;
    }

    /**
     * Parse a map of validation type to characters.
     *
     * @param key The common property key.
     * @param config The configuration properties.
     * @param types The validation types.
     * @param defaultIfMissing The default character for missing entries.
     * @return The map.
     */
    protected Map<String, Character> parseCharacterMap(String key, Configuration config, List<String> types, char defaultIfMissing) {
        Map<String, Character> map = new HashMap<>();
        for (String type: types) {
            String value;
            try {
                value = config.getString(key+"."+type, Character.valueOf(defaultIfMissing).toString());
            } catch (Exception e) {
                value = Character.valueOf(defaultIfMissing).toString();
            }
            map.put(type, value.toCharArray()[0]);
        }
        return map;
    }

    /**
     * Parse a map of validation type to boolean values.
     *
     * @param key The common property key.
     * @param config The configuration properties.
     * @param types The validation types.
     * @return The map (using false for missing entries).
     */
    protected Map<String, Boolean> parseBooleanMap(String key, Configuration config, List<String> types) {
        return parseBooleanMap(key, config, types, false);
    }

    /**
     * Parse a map of validation type to enum instance.
     *
     * @param key The common property key.
     * @param enumType The class of the enum.
     * @param defaultValue The default enum value for missing entries.
     * @param config The configuration properties.
     * @param types The validation types.
     * @param <R> The class of the enum.
     * @return The map.
     */
    protected <R extends Enum<R>> Map<String, R> parseEnumMap(String key, Class<R> enumType, R defaultValue, Configuration config, List<String> types) {
        Map<String, R> map = new HashMap<>();
        for (String type: types) {
            map.put(type, R.valueOf(enumType, config.getString(key+"."+type, defaultValue.name())));
        }
        return map;
    }

    /**
     * Parse a map of validation type to arbitrary objects.
     *
     * @param commonKey The common property key.
     * @param config The configuration properties.
     * @param fnObjectBuilder The mapper function to construct objects.
     * @param <R> The type of each object.
     * @return The map.
     */
    protected <R> Map<String, R> parseObjectMap(String commonKey, Configuration config, BiFunction<String, Map<String, String>, R> fnObjectBuilder) {
        Map<String, R> map = new HashMap<>();
        Iterator<String> mapKeys = config.getKeys(commonKey);
        Map<String, Map<String, String>> collectedData = new HashMap<>();
        while (mapKeys.hasNext()) {
            String fullKey = mapKeys.next();
            String[] partsAfterCommon = StringUtils.split(StringUtils.substringAfter(fullKey, commonKey+"."), '.');
            if (partsAfterCommon != null && partsAfterCommon.length == 2) {
                Map<String, String> instanceData = collectedData.computeIfAbsent(partsAfterCommon[0], (key) -> new HashMap<>());
                instanceData.put(partsAfterCommon[1], config.getString(fullKey));
            }
        }
        collectedData.forEach((key, value) -> {
            R obj = fnObjectBuilder.apply(key, value);
            if (obj != null) {
                map.put(key, obj);
            }
        });
        return map;
    }

    /**
     * Parse a map of validation type to map of enums.
     *
     * @param key The common property key.
     * @param defaultValue The default enum to use for missing entries.
     * @param config The configuration properties.
     * @param types The validation types.
     * @param fnEnumBuilder The function used to construct the map of enums. The key is the part following the validation type.
     * @param <R> The type of the enums.
     * @return The map.
     */
    protected <R extends Enum<R>> Map<String, R> parseEnumMap(String key, R defaultValue, Configuration config, List<String> types, Function<String,R> fnEnumBuilder) {
        Map<String, R> map = new HashMap<>();
        for (String type: types) {
            if (config.containsKey(key+"."+type)) {
                map.put(type, fnEnumBuilder.apply(config.getString(key+"."+type)));
            } else {
                map.put(type, defaultValue);
            }
        }
        return map;
    }

    /**
     * Parse a map of strings.
     *
     * @param commonKey The common key part. The remaining part will be the map's keys.
     * @param config The configuration properties.
     * @return The map.
     */
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

    /**
     * Add missing values to the provided map.
     *
     * @param map The map to complete.
     * @param defaultValues The map of default values to use.
     */
    protected void addMissingDefaultValues(Map<String, String> map, Map<String, String> defaultValues) {
        if (defaultValues != null) {
            for (Map.Entry<String, String> defaultEntry: defaultValues.entrySet()) {
                if (!map.containsKey(defaultEntry.getKey())) {
                    map.put(defaultEntry.getKey(), defaultEntry.getValue());
                }
            }
        }
    }

    /**
     * Parse a map of strings.
     *
     * @param commonKey The common property key.
     * @param config The configuration properties.
     * @param defaultValues The map of default values to consider for missing entries.
     * @return The map.
     */
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

    /**
     * Parse a map of strings.
     *
     * @param key The common key part.
     * @param config The configuration properties.
     * @param subKeys The set of sub-keys after the common part to be used as the map's keys.
     * @return The map.
     */
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

    /**
     * Parse all validation artifact information for the provided validation type.
     *
     * @param artifactType The artifact type.
     * @param rootKey The common key part.
     * @param externalRootKey The common key part linked to external artifacts' configuration.
     * @param externalCombinationKey The key part for the combination approach (default is combinationApproach).
     * @param domainConfig The domain configuration to enrich.
     * @param config The configuration properties.
     */
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
            info.setPreProcessorOutputExtension(StringUtils.trim(config.getString(typeKey+".preprocessor.output")));
            info.setRemoteArtifacts(new ArrayList<>());
            // Lookup remote artifact info.
            int remoteCounter = 0;
            while (config.containsKey(typeKey+".remote."+remoteCounter+".url")) {
                RemoteValidationArtifactInfo remoteInfo = new RemoteValidationArtifactInfo();
                remoteInfo.setUrl(config.getString(StringUtils.trim(typeKey+".remote."+remoteCounter+".url")));
                remoteInfo.setType(StringUtils.trim(config.getString(typeKey+".remote."+remoteCounter+".type")));
                remoteInfo.setPreProcessorPath(StringUtils.trim(config.getString(typeKey+".remote."+remoteCounter+".preprocessor")));
                remoteInfo.setPreProcessorOutputExtension(StringUtils.trim(config.getString(typeKey+".remote."+remoteCounter+".preprocessor.output")));
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

    /**
     * Parse all validation artifact information for the provided validation type and considering a default artifact type (if only one).
     *
     * @param rootKey The common key part.
     * @param externalRootKey The common key part linked to external artifacts' configuration.
     * @param externalCombinationKey The key part for the combination approach (default is combinationApproach).
     * @param domainConfig The domain configuration to enrich.
     * @param config The configuration properties.
     */
    protected void addValidationArtifactInfo(String rootKey, String externalRootKey, String externalCombinationKey, DomainConfig domainConfig, Configuration config) {
        addValidationArtifactInfoForType(null, rootKey, externalRootKey, externalCombinationKey, domainConfig, config);
    }

    /**
     * Filter to select files based on their extension.
     */
    private static class ExtensionFilter implements FilenameFilter {

        private String ext;

        /**
         * @param ext The extension.
         */
        ExtensionFilter(String ext) {
            this.ext = ext;
        }

        /**
         * Check to see if the file matches.
         *
         * @param dir The processed directory.
         * @param name The file name.
         * @return True for a match.
         */
        public boolean accept(File dir, String name) {
            return (name.endsWith(ext));
        }
    }

}
