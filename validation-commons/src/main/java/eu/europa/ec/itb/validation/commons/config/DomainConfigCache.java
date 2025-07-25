/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.*;
import eu.europa.ec.itb.validation.plugin.PluginInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.configuration2.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Superclass for the loading and storing of domain configuration instances.
 *
 * @param <T> The type of domain configuration class.
 */
public abstract class DomainConfigCache <T extends DomainConfig> {

    private static final String DEFAULT_FILE_ENCODING = "UTF-8";
    private static final String PROPERTY_SUFFIX = ".properties";
    static final long DEFAULT_MAXIMUM_REPORTS_FOR_DETAILS_OUTPUT = 5000L;
    static final long DEFAULT_MAXIMUM_REPORTS_FOR_XML_OUTPUT = 50000L;
    private static final Logger logger = LoggerFactory.getLogger(DomainConfigCache.class);

    @Autowired
    ApplicationConfig appConfig = null;

    private final ConcurrentHashMap<String, T> domainConfigs = new ConcurrentHashMap<>();
    private final T undefinedDomainConfig;
    private final ExtensionFilter propertyFilter = new ExtensionFilter(PROPERTY_SUFFIX);

    /**
     * Constructor.
     */
    protected DomainConfigCache() {
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
     * <p>
     * Additional customisations can be included by overriding the init() method.
     */
    @PostConstruct
    public void initBase() {
        // Initialise all domain configurations at startup.
        List<T> configs = getAllDomainConfigurations();
        // Validate common cross-domain configurations.
        validateDomainAliases(configs);
        // Add extension point for validator-specific configurations.
        init();
    }

    /**
     * Called once all domains have been loaded to make sure that any cross-domain aliases are correct.
     *
     * @param configs The domain configurations to validate.
     */
    private void validateDomainAliases(List<T> configs) {
        for (var config: configs) {
            if (config.getDomainAlias() != null) {
                var otherConfig = getConfigForDomainName(config.getDomainAlias(), false, true);
                if (otherConfig == null) {
                    // We could not find the referenced domain.
                    logger.warn("Domain [{}] was configured as an alias for non-existent domain [{}]. Domain alias configurations will be ignored.", config.getDomainName(), config.getDomainAlias());
                    config.setDomainAlias(null);
                    config.getDomainTypeAlias().clear();
                } else {
                    /*
                     * Aliased domain exists. Make sure that all validation types of the current domain have their equivalents
                     * in the other domain:
                     * - Types with domain aliases point to existing types of the other domain.
                     * - Types without aliases exist with the exact same name in the other domain.
                     */
                    Set<String> validationTypesToCheck = new TreeSet<>(config.getType());
                    // First check the declared aliases to ensure they match types in the target domain.
                    Set<String> aliasesToRemove = new HashSet<>();
                    for (var aliasEntry: config.getDomainTypeAlias().entrySet()) {
                        if (!otherConfig.getType().contains(aliasEntry.getValue())) {
                            // Check also aliases in the other domain.
                            if (otherConfig.resolveAlias(aliasEntry.getValue()) == null) {
                                logger.warn("Domain [{}] defines for validation type [{}] an alias of [{}] that was not found in domain [{}]. The alias will be ignored.", config.getDomainName(), aliasEntry.getKey(), aliasEntry.getValue(), otherConfig.getDomainName());
                                aliasesToRemove.add(aliasEntry.getKey());
                            } else {
                                // No need to check this validation type as the defined alias was valid.
                                validationTypesToCheck.remove(aliasEntry.getKey());
                            }
                        } else {
                            // No need to check this validation type as the defined alias was valid.
                            validationTypesToCheck.remove(aliasEntry.getKey());
                        }
                    }
                    aliasesToRemove.forEach(config.getDomainTypeAlias()::remove);
                    // Now check the remaining validation types for aliases. These will be the ones that did not have a (valid) alias.
                    boolean hasAliasProblems = false;
                    for (var validationType: validationTypesToCheck) {
                        if (!otherConfig.getType().contains(validationType)) {
                            // Check also aliases in the other domain.
                            if (otherConfig.resolveAlias(validationType) == null) {
                                logger.warn("Domain [{}] is an alias of [{}] but no corresponding validation type could be found for [{}]. You need to either define a valid alias for this type or ensure that the same type exists in domain [{}].", config.getDomain(), otherConfig.getDomainName(), validationType, otherConfig.getDomainName());
                                hasAliasProblems = true;
                            }
                        }
                    }
                    if (hasAliasProblems) {
                        logger.warn("Domain [{}] is an alias of [{}] but does not correctly alias all its validation types. The domain alias configuration will ignored.", config.getDomainName(), otherConfig.getDomainName());
                        config.setDomainAlias(null);
                        config.getDomainTypeAlias().clear();
                    }
                }
            }
        }
    }

    /**
     * Actions to be performed before destroying the instance.
     */
    @PreDestroy
    public void close() {
        for(T config: domainConfigs.values()) {
            try{
                URLClassLoader loader = config.getLocaleTranslationsLoader();
                if(loader != null) {
                    loader.close();
                }
            } catch (Exception ex) {
                // Do nothing. Only try to prevent the method from crashing if an exception is thrown.
            }
        }
    }

    /**
     * Extension point (by default empty) to do additional configuration if needed.
     */
    protected void init() {
        // Empty by default.
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
     * @param warnIfNotFound Log a warning if the domain is not found.
     * @param resolveAlias Whether a domain alias will be resolved (if defined).
     * @return The domain configuration.
     */
    public T getConfigForDomainName(String domainName, boolean warnIfNotFound, boolean resolveAlias) {
        T config = getConfigForDomain(appConfig.getDomainNameToDomainId().getOrDefault(domainName, ""));
        if (config == null) {
            if (warnIfNotFound) {
                logger.warn("Invalid domain name [{}].", domainName);
            }
            return null;
        } else {
            if (resolveAlias && config.getDomainAlias() != null) {
                // Domain is an alias for another one.
                return getConfigForDomainName(config.getDomainAlias(), warnIfNotFound, true);
            } else {
                return config;
            }
        }
    }

    /**
     * Get a domain configuration by its public name logging a warning if it is not found and resolving aliases.
     *
     * @param domainName The domain's name.
     * @return The domain configuration.
     */
    public T getConfigForDomainName(String domainName) {
        return getConfigForDomainName(domainName, true, true);
    }

    /**
     * Get a domain configuration by its identifier (folder name).
     *
     * @param domain The domain identifier.
     * @return The domain configuration.
     */
    protected T getConfigForDomain(String domain) {
        T domainConfigToReturn = domainConfigs.get(domain);
        if (domainConfigToReturn == null) {
            String[] files = Paths.get(appConfig.getResourceRoot(), domain).toFile().list(propertyFilter);
            if (files == null || files.length == 0) {
                domainConfigToReturn = undefinedDomainConfig;
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
                    
                    T domainConfig = newDomainConfig();
                    domainConfig.setDefined(true);
                    domainConfig.setDomain(domain);
                    domainConfig.setDomainName(appConfig.getDomainIdToDomainName().get(domain));
                    domainConfig.setDomainRoot(Paths.get(appConfig.getResourceRoot(), domain).toString());

                    List<String> declaredValidationTypes = Arrays.stream(Objects.requireNonNull(StringUtils.split(config.getString("validator.type"), ','), "No validation types were configured")).map(String::trim).toList();
                    Map<String, List<String>> validationTypeOptions = new HashMap<>();
                    // Initialise Aliases
                    Map<String, String> typeAliases = ParseUtils.parseMap("validator.typeAlias", config);
                    for (Map.Entry<String,String> entry: ParseUtils.parseMap("validator.typeOptions", config, declaredValidationTypes).entrySet()) {
                        validationTypeOptions.put(entry.getKey(), Arrays.stream(StringUtils.split(entry.getValue(), ',')).map(String::trim).toList());
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
                    processValidationTypeGroups(declaredValidationTypes, domainConfig, config);
                    domainConfig.setType(validationTypes);
                    domainConfig.setDeclaredType(declaredValidationTypes);
                    domainConfig.setValidationTypeOptions(validationTypeOptions);
                    domainConfig.setValidationTypeAlias(typeAliases);

                    // Validate validation type aliases.
                    if (domainConfig.getValidationTypeAlias() != null) {
                        for (String alias: domainConfig.getValidationTypeAlias().keySet()) {
                            if (domainConfig.resolveAlias(alias) == null && logger.isWarnEnabled()) {
                                logger.warn("Invalid configuration for domain [{}]. Alias [{}] points to missing (full) validation type [{}].", domain, alias, domainConfig.getValidationTypeAlias().get(alias));
                            }
                        }
                    }

                    // Domain aliases - start
                    domainConfig.setDomainAlias(config.getString("validator.domainAlias", null));
                    if (domainConfig.getDomainAlias() != null) {
                        Map<String, String> domainTypeAliases = ParseUtils.parseMap("validator.domainAlias", config);
                        // Check to see that the aliased types for the current domain are valid.
                        Set<String> aliasesToRemove = new HashSet<>();
                        for (var alias: domainTypeAliases.keySet()) {
                            if (!domainConfig.getType().contains(alias)) {
                                logger.warn("Invalid configuration for domain [{}]. Domain validation type alias [{}] is not an existing (full) validation type.", domain, alias);
                                aliasesToRemove.add(alias);
                            }
                        }
                        aliasesToRemove.forEach(domainTypeAliases::remove);
                        domainConfig.setDomainTypeAlias(domainTypeAliases);
                    }
                    // Domain aliases - end

                    // Add default validation type
                    String defaultType = config.getString("validator.defaultType");
                    if (defaultType != null && !defaultType.isBlank()) {
                        if (validationTypes.contains(defaultType)) {
                            domainConfig.setDefaultType(defaultType);
                        } else {
                            logger.warn("Invalid configuration for domain [{}]. Default type [{}] is not a (full) validation type.", domain, defaultType);
                        }
                    }
                    // if one validation type is provided and missing or invalid default validation type
                    if (domainConfig.getDefaultType() == null && validationTypes.size() == 1) {
                        defaultType = validationTypes.get(0);
                        logger.info("Setting default validation type for domain [{}] to only validation type provided [{}].", domain, defaultType);
                        domainConfig.setDefaultType(defaultType);
                    }
                    Set<ValidatorChannel> supportedChannels = new HashSet<>(Arrays.asList(getSupportedChannels()));
                    domainConfig.setChannels(Arrays.stream(StringUtils.split(config.getString("validator.channels", getDefaultChannelsStr()), ',')).map(String::trim).map(name -> toValidatorChannel(supportedChannels, name)).collect(Collectors.toSet()));
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
                    domainConfig.setPluginDefaultConfig(ParseUtils.parseValueList("validator.defaultPlugins", config, pluginConfigMapper));
                    domainConfig.setPluginPerTypeConfig(ParseUtils.parseTypedValueList("validator.plugins", domainConfig.getType(), config, pluginConfigMapper));
                    // Parse plugins - end
                    // Input preprocessing expressions - start
                    domainConfig.setInputPreprocessorPerType(ParseUtils.parseMap("validator.input.preprocessor", config, validationTypes));
                    // Input preprocessing expressions - end
                    // Maximum report thresholds - start
                    domainConfig.setMaximumReportsForDetailedOutput(config.getLong("validator.maximumReportsForDetailedOutput", DEFAULT_MAXIMUM_REPORTS_FOR_DETAILS_OUTPUT));
                    if (domainConfig.getMaximumReportsForDetailedOutput() < 0) {
                        domainConfig.setMaximumReportsForDetailedOutput(DEFAULT_MAXIMUM_REPORTS_FOR_DETAILS_OUTPUT);
                    }
                    domainConfig.setMaximumReportsForXmlOutput(config.getLong("validator.maximumReportsForXmlOutput", DEFAULT_MAXIMUM_REPORTS_FOR_XML_OUTPUT));
                    if (domainConfig.getMaximumReportsForXmlOutput() < 0) {
                        domainConfig.setMaximumReportsForXmlOutput(DEFAULT_MAXIMUM_REPORTS_FOR_XML_OUTPUT);
                    }
                    if (domainConfig.getMaximumReportsForXmlOutput() < domainConfig.getMaximumReportsForDetailedOutput()) {
                        domainConfig.setMaximumReportsForXmlOutput(domainConfig.getMaximumReportsForDetailedOutput());
                    }
                    // Maximum report thresholds - end
                    // CSV BOM configuration.
                    domainConfig.setAddBOMToCSVExports(config.getBoolean("validator.addBOMToCSVExports", Boolean.TRUE));
                    domainConfig.setReportsOrdered(config.getBoolean("validator.reportsOrdered", false));
                    // Check how to react to remote artefact load failures - start
                    var defaultResponseType = ErrorResponseTypeEnum.fromValue(config.getString("validator.remoteArtefactLoadErrors", "log"));
                    domainConfig.setRemoteArtifactLoadErrorResponse(ParseUtils.parseEnumMap("validator.remoteArtefactLoadErrors", defaultResponseType, config, domainConfig.getType(), ErrorResponseTypeEnum::fromValue));
                    // Check how to react to remote artefact load failures - end
                    // Load optional report metadata - start.
                    domainConfig.setValidationServiceName(config.getString("validator.report.validationServiceName"));
                    domainConfig.setValidationServiceVersion(config.getString("validator.report.validationServiceVersion"));
                    domainConfig.setReportId(config.getString("validator.report.id"));
                    domainConfig.setReportName(config.getString("validator.report.name"));
                    domainConfig.setReportProfileIdDefault(config.getString("validator.report.profileId"));
                    domainConfig.setReportProfileIds(ParseUtils.parseMap("validator.report.profileId", config, domainConfig.getType()));
                    domainConfig.setReportCustomisationIdDefault(config.getString("validator.report.customisationId"));
                    domainConfig.setReportCustomisationIds(ParseUtils.parseMap("validator.report.customisationId", config, domainConfig.getType()));
                    // Load optional report metadata - end.
                    domainConfig.setRichTextReports(config.getBoolean("validator.richTextReports", false));
                    // HTTP version - start.
                    var httpVersionString = config.getString("validator.httpVersion", "2");
                    if (httpVersionString.equals("1.1")) {
                        domainConfig.setHttpVersion(HttpClient.Version.HTTP_1_1);
                    } else {
                        domainConfig.setHttpVersion(HttpClient.Version.HTTP_2);
                    }
                    // HTTP version - end.
                    // Allow subclasses to extend the configuration as needed.
                    addDomainConfiguration(domainConfig, config);
                    completeValidationArtifactConfig(domainConfig);
                    // Add resource bundles to the domain configuration.
                    addResourceBundlesConfiguration(domainConfig, config);
                    logger.info("Loaded configuration for domain [{}]", domain);
                    domainConfigToReturn = domainConfig;
                } catch (Exception e) {
                    // Make sure a domain's invalid configuration never fails the overall startup of the validator.
                    logger.warn("Failed to initialise configuration for domain [{}]", domain, e);
                    domainConfigToReturn = undefinedDomainConfig;
                }
            }
            domainConfigs.put(domain, domainConfigToReturn);
        }
        return domainConfigToReturn;
    }

    /**
     * Process any configured validation type groups.
     *
     * @param declaredValidationTypes The declared validation types.
     * @param domainConfig The domain configuration.
     * @param config The property configuration.
     */
    private void processValidationTypeGroups(List<String> declaredValidationTypes, DomainConfig domainConfig, Configuration config) {
        Set<String> typesToCheck = new HashSet<>(declaredValidationTypes);
        Set<String> typesChecked = new HashSet<>();
        // Parse validation type groups while ignoring invalid entries
        Map<String, List<String>> validationTypeGroups = ParseUtils.parseListMap("validator.typeGroup", config, Optional.of(entry -> {
            if (typesToCheck.contains(entry.getValue())) {
                typesToCheck.remove(entry.getValue());
                typesChecked.add(entry.getValue());
                return true;
            } else if (typesChecked.contains(entry.getValue())) {
                logger.warn("Type [{}] is mapped to multiple groups. Only the first mapping will be used.", entry.getValue());
                return false;
            } else {
                logger.warn("Unknown type [{}] declared in group [{}] will be ignored.", entry.getValue(), entry.getKey());
                return false;
            }
        }));
        if (!validationTypeGroups.isEmpty()) {
            if (!typesToCheck.isEmpty()) {
                throw new IllegalStateException("When using validation type groups, all declared types must be mapped to groups. Types found not to be grouped were: %s".formatted(typesToCheck));
            }
            String declaredPresentationType = config.getString("validator.typeGroupPresentation", GroupPresentationEnum.INLINE.getIdentifier());
            GroupPresentationEnum.of(declaredPresentationType).ifPresentOrElse(
                    domainConfig::setGroupPresentation,
                    () -> {
                        logger.warn("Found unsupported group presentation type [{}] and will use [{}] instead.", declaredPresentationType, GroupPresentationEnum.INLINE.getIdentifier());
                        domainConfig.setGroupPresentation(GroupPresentationEnum.INLINE);
                    }
            );
            domainConfig.setValidationTypeGroups(validationTypeGroups);
        }
    }

    /**
     * Import any additional properties from other property files (if allowed).
     *
     * @param config The loaded configuration properties.
     * @param domain The current domain's identifier.
     */
    protected void importAdditionalProperties(CompositeConfiguration config, String domain) {
    	String importPropertiesStringPath = config.getString("validator.importProperties", null);
    	if(importPropertiesStringPath == null) {
    		return;
    	}
        Path importPropertiesPath;
        if ((importPropertiesPath = resolveFilePathForDomain(domain, importPropertiesStringPath)) != null) {
            addConfigurationFromFile(importPropertiesPath, config);
        } else {
            throw new IllegalStateException("Resources are restricted to domain. Their paths should be relative to domain folder.");
        }
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
        Path localFilePath = Path.of(localFile);
    	if (!localFilePath.isAbsolute()) {
            localFilePath = Paths.get(appConfig.getResourceRoot(), domain, localFile.trim());
        }
    	Path domainRootCanonicalPath;
    	Path localFileCanonicalPath;
		try {
			domainRootCanonicalPath = domainRootPath.toFile().getCanonicalFile().toPath();
			localFileCanonicalPath = localFilePath.toFile().getCanonicalFile().toPath();
			return localFileCanonicalPath.startsWith(domainRootCanonicalPath);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to find domain properties file" + localFile, e);
		}
    }

    /**
     * Check to see if the provided file path (relative or absolute) is within the given domain's folder and return the absolute path.
     *
     * @param domain The domain's identifier (folder name).
     * @param localFile The file path to check.
     * @return The absolute, canonical file path if it is allowed, null otherwise.
     */
    private Path resolveFilePathForDomain(String domain, String localFile) {
        Path localFilePath = Path.of(localFile);
        if (appConfig.isRestrictResourcesToDomain() && localFilePath.isAbsolute()) {
            return null;
        } else if (!localFilePath.isAbsolute()) {
            localFilePath = Paths.get(appConfig.getResourceRoot(), domain, localFile.trim());
        }        
    	Path localFileCanonicalPath;
        try {
            localFileCanonicalPath = localFilePath.toFile().getCanonicalFile().toPath();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to resolve file with path " + localFile, e);
        }
        return localFileCanonicalPath;
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
                            logger.warn("Domain [{}] defines no preconfigured validation artifacts for validation type [{}] but also doesn't require externally provided artifacts. Forcing external artifacts as required.", domainConfig.getDomainName(), validationType);
                            artifactInfo.get().setExternalArtifactSupport(ExternalArtifactSupport.REQUIRED);
                        }
                    } else if (artifactTypeCount > 1 && artifactInfo.getOverallExternalArtifactSupport() == ExternalArtifactSupport.NONE) {
                        logger.warn("Domain [{}] defines no preconfigured validation artifacts for validation type [{}] but also doesn't expect externally provided artifacts. At least one of the defined artifact types should be set with optional external artifacts.", domainConfig.getDomainName(), validationType);
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
     * Add to a domain configuration object information on configured translation resource bundles.
     *
     * @param domainConfig The domain configuration object to consider.
     * @param config The validator configuration.
     */
    protected void addResourceBundlesConfiguration(T domainConfig, Configuration config) {
        var defaultLocaleStr = config.getString("validator.locale.default", "");
        String[] availableLocales = config.getString("validator.locale.available", "").split(",");
        LinkedHashSet<Locale> availableLocalesSet = new LinkedHashSet<>();
        if (availableLocales.length > 0 && !availableLocales[0].isEmpty()) {
            availableLocalesSet.addAll(Arrays.stream(availableLocales).map(String::trim).map(LocaleUtils::toLocale).toList());
        }
        domainConfig.setAvailableLocales(availableLocalesSet);
        if (!domainConfig.getAvailableLocales().isEmpty()) {
            if (defaultLocaleStr.isBlank()) {
                domainConfig.setDefaultLocale(domainConfig.getAvailableLocales().iterator().next());
            } else {
                domainConfig.setDefaultLocale(LocaleUtils.toLocale(defaultLocaleStr));
                if (!domainConfig.getAvailableLocales().contains(domainConfig.getDefaultLocale())) {
                    throw new IllegalStateException("The default locale " + domainConfig.getDefaultLocale() + " is not among the available locales  for domain " + domainConfig.getDomainName());
                }
            }
        } else {
            if (defaultLocaleStr.isBlank()) {
                domainConfig.setDefaultLocale(Locale.ENGLISH);
            } else {
                domainConfig.setDefaultLocale(LocaleUtils.toLocale(defaultLocaleStr));
            }
            domainConfig.setAvailableLocales(Set.of(domainConfig.getDefaultLocale()));
        }
        var pathToTranslations = config.getString("validator.locale.translations", null);
        // loading the domain properties for localisation
        Map<String, String> domainProperties = new HashMap<>();
        Iterator<String> propsIterator = config.getKeys();
        while (propsIterator.hasNext()) {
            String key = propsIterator.next();
            String prop = config.getString(key);
            if (prop != null && !prop.isEmpty()) {
                domainProperties.put(key, prop);
            }
        }
        domainConfig.setDomainProperties(domainProperties);
        // load the local translations
        if (pathToTranslations != null && !pathToTranslations.isEmpty()) {
            // check if the property file folder is within the domain folder
            Path filePath = resolveFilePathForDomain(domainConfig.getDomain(), pathToTranslations);
            if (filePath != null) {
                // obtain the translations folder and bundle name 
                File file = filePath.toFile();
                File translationsFolder;
                String bundleName = null;
                try {
                    if (file.isDirectory()) {
                        translationsFolder = file;
                        var files = file.listFiles();
                        if (files != null) {
                            List<String> propFileNames = Arrays.stream(files).filter(File::isFile)
                                    .filter( f -> f.getName().contains(PROPERTY_SUFFIX))
                                    .map( f -> f.getName().substring(0, f.getName().lastIndexOf(PROPERTY_SUFFIX)))
                                    .toList();
                            bundleName = obtainBundleName(propFileNames);
                        }
                    } else {
                        translationsFolder = file.getParentFile();
                        String fileName =  file.getName();
                        if (fileName.endsWith(PROPERTY_SUFFIX)) {
                            bundleName = fileName.substring(0, fileName.lastIndexOf(PROPERTY_SUFFIX));
                        } else {
                            bundleName = fileName;
                        }
                    }
                    // create class loader and set up domainConfig
                    if (translationsFolder.exists()) {
                        domainConfig.setLocaleTranslationsBundle(bundleName);
                        domainConfig.setLocaleTranslationsLoader(new URLClassLoader(new URL[] { translationsFolder.toURI().toURL() }));
                    }
                } catch (MalformedURLException ex) {
                    throw new IllegalStateException("Unexpected error while processing configured translation files", ex);
                }
            } else {
                throw new IllegalStateException("Resources are restricted to domain. Their paths should be relative to domain folder. Unable to access translations file/directory [" + pathToTranslations+ "]");
            }
            
        }
    }

    /**
     * Method that obtains the common names of the groups of files. 
     * 
     * @param fileNames The list of file names.
     * @return The list of bundle names.
     */
    private String obtainBundleName(List<String> fileNames) {
        String commonName = StringUtils.getCommonPrefix(fileNames.toArray(new String[0]));
        if (commonName == null || commonName.isEmpty()) {
            return null;
        } else {
            return (commonName.endsWith("_")) ? commonName.substring(0, commonName.length() - 1) : commonName;
        }
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
            domainConfig.getArtifactInfo().get(validationType).add(Objects.toString(artifactType, TypedValidationArtifactInfo.DEFAULT_TYPE), info);
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
     *
     * @param ext The extension.
     */
    private record ExtensionFilter(String ext) implements FilenameFilter {

        /**
         * Check to see if the file matches.
         *
         * @param dir  The processed directory.
         * @param name The file name.
         * @return True for a match.
         */
        public boolean accept(File dir, String name) {
            return (name.endsWith(ext));
        }
    }

}
