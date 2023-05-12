package eu.europa.ec.itb.validation.commons.config;

import com.gitb.tr.TAR;
import com.gitb.tr.ValidationOverview;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.plugin.PluginInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLClassLoader;
import java.util.*;

/**
 * Configuration for a given validation domain.
 */
public class DomainConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DomainConfig.class);

    private boolean isDefined;
    private boolean reportsOrdered;
    private String domain;
    private String domainName;
    private List<String> type;
    private List<String> declaredType;
    private String defaultType;
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
    private Map<String, String> inputPreprocessorPerType;
    // Plugin configuration.
    private List<PluginInfo> pluginDefaultConfig;
    private Map<String, List<PluginInfo>> pluginPerTypeConfig;
    private final Map<String, Boolean> remoteArtefactStatus = new HashMap<>();
    private Map<String, ErrorResponseTypeEnum> remoteArtifactLoadErrorResponse;

    private String reportId;
    private String reportName;
    private Map<String, String> reportProfileIds;
    private String reportProfileIdDefault;
    private Map<String, String> reportCustomisationIds;
    private String reportCustomisationIdDefault;
    private String validationServiceName;
    private String validationServiceVersion;

    /**
     * Apply configured metadata to the TAR report.
     *
     * @param report The report to enrich.
     * @param appliedValidationType The (full) validation type that was applied to produce the report.
     */
    public void applyMetadata(TAR report, String appliedValidationType) {
        if (report != null && appliedValidationType != null) {
            if (report.getOverview() == null) {
                report.setOverview(new ValidationOverview());
            }
            // Report ID.
            if (reportId != null) {
                report.setId(reportId);
            }
            // Report name.
            if (reportName != null) {
                report.setName(reportName);
            }
            // Validation service name.
            if (validationServiceName != null) {
                report.getOverview().setValidationServiceName(validationServiceName);
            }
            // Validation service version.
            if (validationServiceVersion != null) {
                report.getOverview().setValidationServiceVersion(validationServiceVersion);
            }
            // Profile ID.
            String profileIdToApply = null;
            if (reportProfileIds != null) {
                profileIdToApply = reportProfileIds.get(appliedValidationType);
            }
            if (profileIdToApply == null) {
                profileIdToApply = reportProfileIdDefault;
                if (profileIdToApply == null) {
                    profileIdToApply = appliedValidationType;
                }
            }
            report.getOverview().setProfileID(profileIdToApply);
            // Customisation ID.
            String customisationIdToApply = null;
            if (reportCustomisationIds != null) {
                customisationIdToApply = reportCustomisationIds.get(appliedValidationType);
            }
            if (customisationIdToApply == null) {
                customisationIdToApply = reportCustomisationIdDefault;
            }
            if (customisationIdToApply != null) {
                report.getOverview().setCustomizationID(customisationIdToApply);
            }
        }
    }

    /**
     * Check to see if the provided validation type has encountered problems loading its remote
     * (preconfigured) validation artefacts.
     *
     * @param validationType The validation type to check for.
     * @return The check result. Will be returned as 'true' if the domain contains no such artefacts.
     */
    public boolean checkRemoteArtefactStatus(String validationType) {
        if (validationType == null) {
            validationType = defaultType;
        }
        if (validationType == null) {
            return true;
        } else {
            return Boolean.TRUE.equals(remoteArtefactStatus.computeIfAbsent(validationType, key -> Boolean.TRUE));
        }
    }

    /**
     * Set the status for remotely loaded (preconfigured) validation artefacts for a given validation type.
     *
     * @param validationType The validation type to set the status for.
     * @param success Whether all remote artefacts were loaded successfully.
     */
    public void setRemoteArtefactStatus(String validationType, boolean success) {
        var currentStatus = remoteArtefactStatus.get(validationType);
        if (Boolean.FALSE.equals(currentStatus) && success) {
            LOG.info("Remote validation artefacts for validation type [{}] of domain [{}] have been restored.", validationType, getDomain());
        } else if (!success) {
            LOG.warn("Remote validation artefacts for validation type [{}] of domain [{}] failed to be loaded.", validationType, getDomain());
        }
        remoteArtefactStatus.put(validationType, success);
    }

    /**
     * Check how to react to a failure when loading remote preconfigured artefacts.
     *
     * @param validationType The validation type to check for.
     * @return The reaction type.
     */
    public ErrorResponseTypeEnum getResponseForRemoteArtefactLoadFailure(String validationType) {
        if (validationType == null) {
            validationType = defaultType;
        }
        if (validationType == null) {
            return ErrorResponseTypeEnum.LOG;
        } else {
            return remoteArtifactLoadErrorResponse.computeIfAbsent(validationType, key -> ErrorResponseTypeEnum.LOG);
        }
    }

    /**
     * @param remoteArtifactLoadErrorResponse The map of (full) validation types to response types for remote
     *                                        artefact load errors.
     */
    public void setRemoteArtifactLoadErrorResponse(Map<String, ErrorResponseTypeEnum> remoteArtifactLoadErrorResponse) {
        this.remoteArtifactLoadErrorResponse = remoteArtifactLoadErrorResponse;
    }

    /**
     * @return The preprocessing expression per (full) validation type.
     */
    public Map<String, String> getInputPreprocessorPerType() {
        return inputPreprocessorPerType;
    }

    /**
     * @param inputPreprocessorPerType The preprocessing expression per (full) validation type.
     */
    public void setInputPreprocessorPerType(Map<String, String> inputPreprocessorPerType) {
        this.inputPreprocessorPerType = inputPreprocessorPerType;
    }

    /**
     * @return True if validation reports should be ordered.
     */
    public boolean isReportsOrdered() {
        return reportsOrdered;
    }

    /**
     * @param reportsOrdered True if validation reports should be ordered.
     */
    public void setReportsOrdered(boolean reportsOrdered) {
        this.reportsOrdered = reportsOrdered;
    }

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
     * @return The default validation type.
     */
    public String getDefaultType() {
        return defaultType;
    }

    /**
     * @param defaultType The default validation type.
     */
    public void setDefaultType(String defaultType) {
        this.defaultType = defaultType;
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
        /*
         * validation type -> artifact type -> support type
         * We only return entries that are different to "none".
         */
        Map<String, Map<String, String>> artifactInfoMap = new HashMap<>();
        if (getArtifactInfo() != null) {
            for (Map.Entry<String, TypedValidationArtifactInfo> entry: getArtifactInfo().entrySet()) {
                Map<String, String> artifactTypeMap = new HashMap<>();
                for (String artifactType: entry.getValue().getTypes()) {
                    if (entry.getValue().get(artifactType).getExternalArtifactSupport() != ExternalArtifactSupport.NONE) {
                        String supportType = entry.getValue().get(artifactType).getExternalArtifactSupport().getName();
                        artifactTypeMap.put(artifactType, supportType);
                    }
                }
                if (!artifactTypeMap.isEmpty()) {
                    artifactInfoMap.put(entry.getKey(), artifactTypeMap);
                }
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

    /**
     * @return The identifier to include in the TAR report (/TestStepReport/id).
     */
    public String getReportId() {
        return reportId;
    }

    /**
     * @param reportId The identifier to include in the TAR report (/TestStepReport/id).
     */
    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    /**
     * @return The name to include in the TAR report (/TestStepReport/name).
     */
    public String getReportName() {
        return reportName;
    }

    /**
     * @param reportName The name to include in the TAR report (/TestStepReport/name).
     */
    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    /**
     * @return The map (based on the full validation type) of profile IDs to include as overrides in TAR reports (/TestStepReport/overview/profileID).
     */
    public Map<String, String> getReportProfileIds() {
        return reportProfileIds;
    }

    /**
     * @param reportProfileIds The map (based on the full validation type) of profile IDs to include as overrides of the validation types in TAR reports (/TestStepReport/overview/profileID).
     */
    public void setReportProfileIds(Map<String, String> reportProfileIds) {
        this.reportProfileIds = reportProfileIds;
    }

    /**
     * @return The map (based on the full validation type) of customisation IDs to include as overrides of the validation type options in TAR reports (/TestStepReport/overview/customizationID).
     */
    public Map<String, String> getReportCustomisationIds() {
        return reportCustomisationIds;
    }

    /**
     * @param reportCustomisationIds The map (based on the full validation type) of customisation IDs to include as overrides of the validation type options in TAR reports (/TestStepReport/overview/customizationID).
     */
    public void setReportCustomisationIds(Map<String, String> reportCustomisationIds) {
        this.reportCustomisationIds = reportCustomisationIds;
    }

    /**
     * @return The validation service name to include in TAR reports (/TestStepReport/overview/validationServiceName).
     */
    public String getValidationServiceName() {
        return validationServiceName;
    }

    /**
     * @param validationServiceName The validation service name to include in TAR reports (/TestStepReport/overview/validationServiceName).
     */
    public void setValidationServiceName(String validationServiceName) {
        this.validationServiceName = validationServiceName;
    }

    /**
     * @return The validation service version to include in TAR reports (/TestStepReport/overview/validationServiceVersion).
     */
    public String getValidationServiceVersion() {
        return validationServiceVersion;
    }

    /**
     * @param validationServiceVersion The validation service version to include in TAR reports (/TestStepReport/overview/validationServiceVersion).
     */
    public void setValidationServiceVersion(String validationServiceVersion) {
        this.validationServiceVersion = validationServiceVersion;
    }

    /**
     * @return The default value to apply for the TAR report profile ID (/TestStepReport/overview/profileID) if no type-specific value is configured.
     */
    public String getReportProfileIdDefault() {
        return reportProfileIdDefault;
    }

    /**
     * @param reportProfileIdDefault The default value to apply for the TAR report profile ID (/TestStepReport/overview/profileID) if no type-specific value is configured.
     */
    public void setReportProfileIdDefault(String reportProfileIdDefault) {
        this.reportProfileIdDefault = reportProfileIdDefault;
    }

    /**
     * @return The default value to apply for the TAR report customisation ID (/TestStepReport/overview/customizationID) if no type-specific value is configured.
     */
    public String getReportCustomisationIdDefault() {
        return reportCustomisationIdDefault;
    }

    /**
     * @param reportCustomisationIdDefault The default value to apply for the TAR report customisation ID (/TestStepReport/overview/customizationID) if no type-specific value is configured.
     */
    public void setReportCustomisationIdDefault(String reportCustomisationIdDefault) {
        this.reportCustomisationIdDefault = reportCustomisationIdDefault;
    }
}
