package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.LocalisationHelper;

import java.util.Map;

/**
 * Base domain configuration class for validators that are web applications.
 */
public class WebDomainConfig extends DomainConfig {

    /** Name of the request attribute under which the relevant domain config object is stored. */
    public static final String DOMAIN_CONFIG_REQUEST_ATTRIBUTE = "domainConfig";

    private String webServiceId = "ValidationService";
    private Map<String, String> webServiceDescription;
    private boolean supportMinimalUserInterface;
    private boolean showAbout;
    private boolean supportUserInterfaceEmbedding;

    /**
     * @return Whether the validator's user interface can be embedded in others.
     */
    public boolean isSupportUserInterfaceEmbedding() {
        return supportUserInterfaceEmbedding;
    }

    /**
     * @param supportUserInterfaceEmbedding Whether the validator's user interface can be embedded in others.
     */
    public void setSupportUserInterfaceEmbedding(boolean supportUserInterfaceEmbedding) {
        this.supportUserInterfaceEmbedding = supportUserInterfaceEmbedding;
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
     * The complete type and option label for a given validation type and option combination.
     *
     * @param typeOption The complete validation type (includes type and option as TYPE.OPTION).
     * @param helper The helper to lookup translations.
     * @return The option's label.
     */
    public String getCompleteTypeOptionLabel(String typeOption, LocalisationHelper helper) {
        String text = null;
        if (helper.propertyExists(String.format("validator.completeTypeOptionLabel.%s", typeOption))) {
            text = helper.localise(String.format("validator.completeTypeOptionLabel.%s", typeOption));
        } else {
            for (var type: getDeclaredType()) {
                var options = getValidationTypeOptions().get(type);
                if (options == null || options.isEmpty()) {
                    if (type.equals(typeOption)) {
                        // Return label for type.
                        text = getValidationTypeLabel(type, helper);
                    }
                } else {
                    for (var option: options) {
                        if (typeOption.equals(type+"."+option)) {
                            text = getValidationTypeLabel(type, helper) + " - " + getValidationTypeOptionLabel(type, option, helper);
                            break;
                        }
                    }
                }
                if (text != null) {
                    break;
                }
            }
            if (text == null) {
                throw new IllegalStateException(String.format("The validation type and option combination [%s] was invalid", typeOption));
            }
        }
        return text;
    }

    /**
     * The label for a validation type.
     *
     * @param type The validation type.
     * @param helper The helper to lookup translations.
     * @return The label.
     */
    public String getValidationTypeLabel(String type, LocalisationHelper helper) {
        String text;
        if (helper.propertyExists(String.format("validator.typeLabel.%s", type))) {
            text = helper.localise(String.format("validator.typeLabel.%s", type));
        } else {
            text = type;
        }
        return text;
    }

    /**
     * The label for an option of a validation type.
     *
     * @param type The validation type.
     * @param option The option.
     * @param helper The helper to lookup translations.
     * @return The label.
     */
    public String getValidationTypeOptionLabel(String type, String option, LocalisationHelper helper) {
        String text;
        if (helper.propertyExists(String.format("validator.typeOptionLabel.%s.%s", type, option))) {
            text = helper.localise(String.format("validator.typeOptionLabel.%s.%s", type, option));
        } else if (helper.propertyExists(String.format("validator.optionLabel.%s", option))) {
            text = helper.localise(String.format("validator.optionLabel.%s", option));
        } else {
            text = option;
        }
        return text;
    }

}
