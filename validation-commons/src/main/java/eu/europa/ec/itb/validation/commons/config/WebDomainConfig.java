package eu.europa.ec.itb.validation.commons.config;

import java.util.Map;

public abstract class WebDomainConfig<T extends LabelConfig> extends DomainConfig {

    private String uploadTitle = "Validator";
    private String webServiceId = "ValidationService";
    private String reportTitle = "Validation report";
    private Map<String, String> webServiceDescription;
    private Map<String, String> typeLabel;
    private Map<String, Map<String, String>> typeOptionLabel;
    private String htmlBanner;
    private String htmlFooter;
    private boolean supportMinimalUserInterface;
    private boolean showAbout;
    private T label;

    public WebDomainConfig() {
        this.label = newLabelConfig();
    }

    protected abstract T newLabelConfig();

    public Map<String, Map<String, String>> getTypeOptionLabel() {
        return typeOptionLabel;
    }

    public void setTypeOptionLabel(Map<String, Map<String, String>> typeOptionLabel) {
        this.typeOptionLabel = typeOptionLabel;
    }

    public String getUploadTitle() {
        return uploadTitle;
    }

    public void setUploadTitle(String uploadTitle) {
        this.uploadTitle = uploadTitle;
    }

    public String getWebServiceId() {
        return webServiceId;
    }

    public void setWebServiceId(String webServiceId) {
        this.webServiceId = webServiceId;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public Map<String, String> getWebServiceDescription() {
        return webServiceDescription;
    }

    public void setWebServiceDescription(Map<String, String> webServiceDescription) {
        this.webServiceDescription = webServiceDescription;
    }

    public Map<String, String> getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(Map<String, String> typeLabel) {
        this.typeLabel = typeLabel;
    }

    public String getHtmlBanner() {
        return htmlBanner;
    }

    public void setHtmlBanner(String htmlBanner) {
        this.htmlBanner = htmlBanner;
    }

    public String getHtmlFooter() {
        return htmlFooter;
    }

    public void setHtmlFooter(String htmlFooter) {
        this.htmlFooter = htmlFooter;
    }

    public boolean isSupportMinimalUserInterface() {
        return supportMinimalUserInterface;
    }

    public void setSupportMinimalUserInterface(boolean supportMinimalUserInterface) {
        this.supportMinimalUserInterface = supportMinimalUserInterface;
    }

    public boolean isShowAbout() {
        return showAbout;
    }

    public void setShowAbout(boolean showAbout) {
        this.showAbout = showAbout;
    }

    public T getLabel() {
        return label;
    }

    public String getValidationTypeOptionLabel(String type, String option) {
        if (typeOptionLabel.containsKey(type)) {
            return typeOptionLabel.get(type).get(option);
        }
        return null;
    }
}
