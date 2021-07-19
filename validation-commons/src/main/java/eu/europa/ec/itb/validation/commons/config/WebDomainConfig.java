package eu.europa.ec.itb.validation.commons.config;

import java.util.Map;

/**
 * Base domain configuration class for validators that are web applications.
 *
 * @param <T> The specific label configuration class.
 */
public abstract class WebDomainConfig<T extends LabelConfig> extends DomainConfig {

    private String uploadTitle = "Validator";
    private String webServiceId = "ValidationService";
    private Map<String, String> webServiceDescription;
    private Map<String, String> typeLabel;
    private Map<String, Map<String, String>> typeOptionLabel;
    private String htmlBanner;
    private String htmlFooter;
    private boolean supportMinimalUserInterface;
    private boolean showAbout;
    private T label;

    /**
     * Constructor.
     */
    public WebDomainConfig() {
        this.label = newLabelConfig();
    }

    /**
     * @return A new and empty label configuration class.
     */
    protected abstract T newLabelConfig();

    /**
     * @return A map of validation type to option to option label.
     */
    public Map<String, Map<String, String>> getTypeOptionLabel() {
        return typeOptionLabel;
    }

    /**
     * @param typeOptionLabel A map of validation type to option to option label.
     */
    public void setTypeOptionLabel(Map<String, Map<String, String>> typeOptionLabel) {
        this.typeOptionLabel = typeOptionLabel;
    }

    /**
     * @return The HTML title for the upload page.
     */
    public String getUploadTitle() {
        return uploadTitle;
    }

    /**
     * @param uploadTitle The HTML title for the upload page.
     */
    public void setUploadTitle(String uploadTitle) {
        this.uploadTitle = uploadTitle;
    }

    /**
     * @return The ID of the SOAP web service.
     */
    public String getWebServiceId() {
        return webServiceId;
    }

    /**
     * @param webServiceId The ID of the SOAP web service.
     */
    public void setWebServiceId(String webServiceId) {
        this.webServiceId = webServiceId;
    }

    /**
     * @return Map of web service input to description text.
     */
    public Map<String, String> getWebServiceDescription() {
        return webServiceDescription;
    }

    /**
     * @param webServiceDescription Map of web service input to description text
     */
    public void setWebServiceDescription(Map<String, String> webServiceDescription) {
        this.webServiceDescription = webServiceDescription;
    }

    /**
     * @return Map of validation type to label.
     */
    public Map<String, String> getTypeLabel() {
        return typeLabel;
    }

    /**
     * @param typeLabel Map of validation type to label.
     */
    public void setTypeLabel(Map<String, String> typeLabel) {
        this.typeLabel = typeLabel;
    }

    /**
     * @return The HTML content for the UI's banner.
     */
    public String getHtmlBanner() {
        return htmlBanner;
    }

    /**
     * @param htmlBanner The HTML content for the UI's banner.
     */
    public void setHtmlBanner(String htmlBanner) {
        this.htmlBanner = htmlBanner;
    }

    /**
     * @return The HTML content for the UI's footer.
     */
    public String getHtmlFooter() {
        return htmlFooter;
    }

    /**
     * @param htmlFooter The HTML content for the UI's footer.
     */
    public void setHtmlFooter(String htmlFooter) {
        this.htmlFooter = htmlFooter;
    }

    /**
     * @return True if a minimal UI is also supported.
     */
    public boolean isSupportMinimalUserInterface() {
        return supportMinimalUserInterface;
    }

    /**
     * @param supportMinimalUserInterface True if a minimal UI is also supported.
     */
    public void setSupportMinimalUserInterface(boolean supportMinimalUserInterface) {
        this.supportMinimalUserInterface = supportMinimalUserInterface;
    }

    /**
     * @return True if the about banner should be displayed on the UI.
     */
    public boolean isShowAbout() {
        return showAbout;
    }

    /**
     * @param showAbout True if the about banner should be displayed on the UI.
     */
    public void setShowAbout(boolean showAbout) {
        this.showAbout = showAbout;
    }

    /**
     * @return The label configuration.
     */
    public T getLabel() {
        return label;
    }

    /**
     * The option label for a given validation type and option combination.
     *
     * @param type The validation type.
     * @param option The option.
     * @return The option's label.
     */
    public String getValidationTypeOptionLabel(String type, String option) {
        if (typeOptionLabel.containsKey(type)) {
            return typeOptionLabel.get(type).get(option);
        }
        return null;
    }
}
