package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.plugin.PluginInfo;

import java.net.URLClassLoader;
import java.util.*;

/**
 * Configuration for a given validation domain.
 */
public class DomainConfig {

    private boolean isDefined;
    private String domain;
    private String domainName;
    private List<String> type;
    private List<String> declaredType;
    private Map<String, List<String>> validationTypeOptions;
    private Set<ValidatorChannel> channels = new HashSet<>();
    private Map<String, TypedValidationArtifactInfo> artifactInfo;
    private Long maximumReportsForDetailedOutput;
    private Long maximumReportsForXmlOutput;
    private Locale defaultLocale;
    private Set<Locale> availableLocales;
    private URLClassLoader localeTranslationsLoader;
    private String localeTranslationsBundle;
    private Map<String, String> domainProperties;
    private String domainRoot;
    private boolean addBOMToCSVExports = true;
    // Plugin configuration.
    private List<PluginInfo> pluginDefaultConfig;
    private Map<String, List<PluginInfo>> pluginPerTypeConfig;

    /**
     * @return Whether to add a BOM to the CSV exports.
     */
    public boolean isAddBOMToCSVExports() {
        return addBOMToCSVExports;
    }

    /**
     * @param addBOMToCSVExports Whether to add a BOM to the CSV exports.
     */
    public void setAddBOMToCSVExports(boolean addBOMToCSVExports) {
        this.addBOMToCSVExports = addBOMToCSVExports;
    }

    /**
     * @return The maximum number of items to include in an XML validation report.
     */
    public Long getMaximumReportsForXmlOutput() {
        return maximumReportsForXmlOutput;
    }

    /**
     * @param maximumReportsForXmlOutput The maximum number of items to include in an XML validation report.
     */
    public void setMaximumReportsForXmlOutput(Long maximumReportsForXmlOutput) {
        this.maximumReportsForXmlOutput = maximumReportsForXmlOutput;
    }

    /**
     * @return The number of items over which a detailed report should not be rendered.
     */
    public Long getMaximumReportsForDetailedOutput() {
        return maximumReportsForDetailedOutput;
    }

    /**
     * @param maximumReportsForDetailedOutput The number of items over which a detailed report should not be rendered.
     */
    public void setMaximumReportsForDetailedOutput(Long maximumReportsForDetailedOutput) {
        this.maximumReportsForDetailedOutput = maximumReportsForDetailedOutput;
    }

    /**
     * @return The list of validation types as declared in the configuration.
     */
    public List<String> getDeclaredType() {
        return declaredType;
    }

    /**
     * @param declaredType The list of validation types as declared in the configuration.
     */
    public void setDeclaredType(List<String> declaredType) {
        this.declaredType = declaredType;
    }

    /**
     * @return Map of validation type to the list of its options.
     */
    public Map<String, List<String>> getValidationTypeOptions() {
        return validationTypeOptions;
    }

    /**
     * @param validationTypeOptions Map of validation type to the list of its options.
     */
    public void setValidationTypeOptions(Map<String, List<String>> validationTypeOptions) {
        this.validationTypeOptions = validationTypeOptions;
    }

    /**
     * @return The validation artifact information for all validation types.
     */
    public Map<String, TypedValidationArtifactInfo> getArtifactInfo() {
        return artifactInfo;
    }

    /**
     * @param artifactInfo The validation artifact information for all validation types.
     */
    public void setArtifactInfo(Map<String, TypedValidationArtifactInfo> artifactInfo) {
        this.artifactInfo = artifactInfo;
    }

    /**
     * @return The list of default validator plugins to consider for all validation types.
     */
    public List<PluginInfo> getPluginDefaultConfig() {
        return pluginDefaultConfig;
    }

    /**
     * @param pluginDefaultConfig The list of default validator plugins to consider for all validation types.
     */
    public void setPluginDefaultConfig(List<PluginInfo> pluginDefaultConfig) {
        this.pluginDefaultConfig = pluginDefaultConfig;
    }

    /**
     * @return Map of the validation type to its list of validation plugins.
     */
    public Map<String, List<PluginInfo>> getPluginPerTypeConfig() {
        return pluginPerTypeConfig;
    }

    /**
     * @param pluginPerTypeConfig Map of the validation type to its list of validation plugins.
     */
    public void setPluginPerTypeConfig(Map<String, List<PluginInfo>> pluginPerTypeConfig) {
        this.pluginPerTypeConfig = pluginPerTypeConfig;
    }

    /**
     * Check if the provided validation type defined validator plugins (specific and/or default).
     *
     * @param validationType The validation type.
     * @return True if plugins are defined.
     */
    public boolean hasPlugins(String validationType) {
        return (pluginDefaultConfig != null && !pluginDefaultConfig.isEmpty()) || (pluginPerTypeConfig != null && pluginPerTypeConfig.containsKey(validationType) && !pluginPerTypeConfig.get(validationType).isEmpty());
    }

    /**
     * @return The identifier (folder name) of the domain.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @param domain The identifier (folder name) of the domain.
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * @return True if the domain defines multiple validation types.
     */
    public boolean hasMultipleValidationTypes() {
        return type != null && type.size() > 1;
    }

    /**
     * @return The list of complete types (type plus option) defined in this domain.
     */
    public List<String> getType() {
        return type;
    }

    /**
     * @param type The list of complete types (type plus option) defined in this domain.
     */
    public void setType(List<String> type) {
        this.type = type;
    }

    /**
     * @return The validator channels supported by this domain.
     */
    public Set<ValidatorChannel> getChannels() {
        return channels;
    }

    /**
     * @param channels The validator channels supported by this domain.
     */
    public void setChannels(Set<ValidatorChannel> channels) {
        this.channels = channels;
    }

    /**
     * @return True if this domain is correctly defined and active.
     */
    public boolean isDefined() {
        return isDefined;
    }

    /**
     * @param defined True if this domain is correctly defined and active.
     */
    public void setDefined(boolean defined) {
        isDefined = defined;
    }

    /**
     * @return The public name for this domain.
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * @param domainName The public name for this domain.
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /**
     * @return The absolute path to the domain root folder.
     */
    public String getDomainRoot() {
        return this.domainRoot;
    }

    /**
     * @param domainRoot The absolute path to the domain root folder.
     */
    public void setDomainRoot(String domainRoot) {
        this.domainRoot = domainRoot;
    }

    /**
     * @return The default locale.
     */
    public Locale getDefaultLocale() {
        return this.defaultLocale;
    }

    /**
     * @param defaultLocale The default locale.
     */
    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    /**
     * @param availableLocales The available locales. 
     */
    public void setAvailableLocales(Set<Locale> availableLocales) {
        this.availableLocales = availableLocales;
    }

    /**
     * @return The available locales.
     */
    public Set<Locale> getAvailableLocales() {
        return this.availableLocales;
    }

    /**
     * @return The URLClassLoader instance to load the locale translations.
     */
    public URLClassLoader getLocaleTranslationsLoader() {
        return this.localeTranslationsLoader;
    }

    /**
     * @param localeTranslationsLoader The locale translations loader.
     */
    public void setLocaleTranslationsLoader(URLClassLoader localeTranslationsLoader) {
        this.localeTranslationsLoader = localeTranslationsLoader;
    }

    /**
     * @return The bundle name for the locale translations.
     */
    public String getLocaleTranslationsBundle() {
        return this.localeTranslationsBundle;
    }

    /**
     * @param localeTranslationsBundle The locale translations bundle name.
     */
    public void setLocaleTranslationsBundle(String localeTranslationsBundle) {
        this.localeTranslationsBundle = localeTranslationsBundle;
    }

    /**
     * @return The domain properties map.
     */
    public Map<String, String> getDomainProperties() {
        return this.domainProperties;
    }

    /**
     * @param domainProperties The domain properties map.
     */
    public void setDomainProperties(Map<String, String> domainProperties) {
        this.domainProperties = domainProperties;
    }

    /**
     * @return The map of validation type to artifact type to external artifact support type.
     */
    public Map<String, Map<String, String>> getExternalArtifactInfoMap() {
        // validation type -> artifact type -> support type
        Map<String, Map<String, String>> artifactInfoMap = new HashMap<>();
        if (getArtifactInfo() != null) {
            for (Map.Entry<String, TypedValidationArtifactInfo> entry: getArtifactInfo().entrySet()) {
                Map<String, String> artifactTypeMap = new HashMap<>();
                for (String artifactType: entry.getValue().getTypes()) {
                    String supportType = entry.getValue().get(artifactType).getExternalArtifactSupport().getName();
                    artifactTypeMap.put(artifactType, supportType);
                }
                artifactInfoMap.put(entry.getKey(), artifactTypeMap);
            }
        }
        return artifactInfoMap;
    }

    /**
     * @return True if this domain defines options for its validation types.
     */
    public boolean hasValidationTypeOptions() {
        return !validationTypeOptions.isEmpty();
    }

}
