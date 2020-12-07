package eu.europa.ec.itb.validation.commons.config;

import org.apache.commons.configuration2.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class WebDomainConfigCache <T extends WebDomainConfig<?>> extends DomainConfigCache<T> {

    @Override
    public T getConfigForDomainName(String domainName) {
        return super.getConfigForDomainName(domainName);
    }

    @Override
    public T getConfigForDomain(String domain) {
        return super.getConfigForDomain(domain);
    }

    @Override
    public List<T> getAllDomainConfigurations() {
        return super.getAllDomainConfigurations();
    }

    @Override
    protected void addDomainConfiguration(T domainConfig, Configuration config) {
        super.addDomainConfiguration(domainConfig, config);
        domainConfig.setUploadTitle(config.getString("validator.uploadTitle", "Validator"));
        domainConfig.setReportTitle(config.getString("validator.reportTitle", "Validation report"));
        domainConfig.setWebServiceId(config.getString("validator.webServiceId", "ValidatorService"));
        // Parse labels for types and options per type (taking into account defaults). Start -
        Map<String, String> typeLabels = parseMap("validator.typeLabel", config, domainConfig.getDeclaredType());
        for (String type: domainConfig.getDeclaredType()) {
            if (!typeLabels.containsKey(type)) {
                typeLabels.put(type, type);
            }
        }
        Map<String, String> defaultOptionLabels = parseMap("validator.optionLabel", config);
        Map<String, Map<String, String>> typeOptionLabelMap = new HashMap<>();
        for (Map.Entry<String, List<String>> typeOptionEntry: domainConfig.getValidationTypeOptions().entrySet()) {
            String type = typeOptionEntry.getKey();
            Map<String, String> optionLabelMap = new HashMap<>();
            for (String option: typeOptionEntry.getValue()) {
                String optionLabel = config.getString("validator.typeOptionLabel."+type+"."+option);
                if (optionLabel == null) {
                    optionLabel = defaultOptionLabels.get(option);
                }
                if (optionLabel == null) {
                    optionLabel = option;
                }
                optionLabelMap.put(option, optionLabel);
                // Add also as an available type label the complete type plus option label.
                typeLabels.put(
                        type +"."+option,
                        config.getString("validator.completeTypeOptionLabel."+type+"."+option, typeLabels.get(type) + " - " + optionLabel)
                );
            }
            typeOptionLabelMap.put(type, optionLabelMap);
        }
        domainConfig.setTypeLabel(typeLabels);
        domainConfig.setTypeOptionLabel(typeOptionLabelMap);
        // - end.
        domainConfig.setWebServiceDescription(parseMap("validator.webServiceDescription", config));
        domainConfig.setShowAbout(config.getBoolean("validator.showAbout", true));
        domainConfig.setSupportMinimalUserInterface(config.getBoolean("validator.supportMinimalUserInterface", false));
        domainConfig.setHtmlBanner(config.getString("validator.bannerHtml", ""));
        domainConfig.setHtmlFooter(config.getString("validator.footerHtml", ""));
        setLabels(domainConfig, config);
    }

    protected void setLabels(T domainConfig, Configuration config) {
        domainConfig.getLabel().setResultSectionTitle(config.getString("validator.label.resultSectionTitle", "Validation result"));
        domainConfig.getLabel().setFileInputLabel(config.getString("validator.label.fileInputLabel", "Content to validate"));
        domainConfig.getLabel().setFileInputPlaceholder(config.getString("validator.label.fileInputPlaceholder", "Select file..."));
        domainConfig.getLabel().setTypeLabel(config.getString("validator.label.typeLabel", "Validate as"));
        domainConfig.getLabel().setOptionLabel(config.getString("validator.label.optionLabel", "Option"));
        domainConfig.getLabel().setUploadButton(config.getString("validator.label.uploadButton", "Validate"));
        domainConfig.getLabel().setResultSubSectionOverviewTitle(config.getString("validator.label.resultSubSectionOverviewTitle", "Overview"));
        domainConfig.getLabel().setResultDateLabel(config.getString("validator.label.resultDateLabel", "Date:"));
        domainConfig.getLabel().setResultFileNameLabel(config.getString("validator.label.resultFileNameLabel", "File name:"));
        domainConfig.getLabel().setResultResultLabel(config.getString("validator.label.resultResultLabel", "Result:"));
        domainConfig.getLabel().setResultErrorsLabel(config.getString("validator.label.resultErrorsLabel", "Errors:"));
        domainConfig.getLabel().setResultWarningsLabel(config.getString("validator.label.resultWarningsLabel", "Warnings:"));
        domainConfig.getLabel().setResultMessagesLabel(config.getString("validator.label.resultMessagesLabel", "Messages:"));
        domainConfig.getLabel().setViewAnnotatedInputButton(config.getString("validator.label.viewAnnotatedInputButton", "View annotated input"));
        domainConfig.getLabel().setDownloadXMLReportButton(config.getString("validator.label.downloadXMLReportButton", "Download XML report"));
        domainConfig.getLabel().setDownloadPDFReportButton(config.getString("validator.label.downloadPDFReportButton", "Download PDF report"));
        domainConfig.getLabel().setResultSubSectionDetailsTitle(config.getString("validator.label.resultSubSectionDetailsTitle", "Details"));
        domainConfig.getLabel().setResultTestLabel(config.getString("validator.label.resultTestLabel", "Test:"));
        domainConfig.getLabel().setResultLocationLabel(config.getString("validator.label.resultLocationLabel", "Location:"));
        domainConfig.getLabel().setPopupTitle(config.getString("validator.label.popupTitle", "Validated content"));
        domainConfig.getLabel().setPopupCloseButton(config.getString("validator.label.popupCloseButton", "Close"));
        domainConfig.getLabel().setOptionContentFile(config.getString("validator.label.optionContentFile", "File"));
        domainConfig.getLabel().setOptionContentURI(config.getString("validator.label.optionContentURI", "URI"));
        domainConfig.getLabel().setOptionContentDirectInput(config.getString("validator.label.optionContentDirectInput", "Direct input"));
        domainConfig.getLabel().setResultValidationTypeLabel(config.getString("validator.label.resultValidationTypeLabel", "Validation type:"));
        domainConfig.getLabel().setIncludeExternalArtefacts(config.getString("validator.label.includeExternalArtefacts", "Include external artefacts"));
        domainConfig.getLabel().setExternalArtefactsTooltip(config.getString("validator.label.externalArtefactsTooltip", "Additional artefacts that will be considered for the validation"));
        domainConfig.getLabel().setMaximumReportsExceededForDetailedOutputMessage(config.getString("validator.label.maximumReportsExceededForDetailedOutputMessage", "Findings are not listed here due to their large number. Download the validation report to view further details."));
        domainConfig.getLabel().setMaximumReportsExceededForXmlOutputMessage(config.getString("validator.label.maximumReportsExceededForXmlOutputMessage", String.format("The validation report is limited to include the first %s items.", domainConfig.getMaximumReportsForXmlOutput())));
    }

}
