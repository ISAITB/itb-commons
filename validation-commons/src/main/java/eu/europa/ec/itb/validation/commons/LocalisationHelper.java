package eu.europa.ec.itb.validation.commons;

import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ec.itb.validation.commons.config.DomainConfig;

/**
 * Class for the loading of internationalized UI messages.
 */
public class LocalisationHelper {

    private static final Logger logger = LoggerFactory.getLogger(LocalisationHelper.class);

    private final Locale locale;
    private final DomainConfig config;

    /**
     * Constructor.
     * 
     * @param config The DomainConfig object that contains the domain configuration
     *               and access to it.
     * @param locale The Locale of the messages that should be fetched.
     */
    public LocalisationHelper(DomainConfig config, Locale locale) {
        this.config = config;
        this.locale = locale;
    }

    /**
     * Method that localises a given property.
     * 
     * @param property The label to resolve.
     * @return The value of the property for a given Locale.
     */
    public String localise(String property) {
        String labelValue = findTranslation(property);
        if (labelValue == null) {
            logger.warn("Value for label " + property + " and locale " + this.locale + " not found.");
            labelValue = "[" + property + "]";
        }
        return labelValue;
    }

    /**
     * Method that localises a parameterised message.
     * 
     * @param property The property to be localised.
     * @param param The parameter to substitute.
     * @return The resulting message.
     */
    public String localiseParameterised(String property, Object ... param){
        String labelValue = findTranslation(property);
        if (labelValue == null) {
            logger.warn("Value for label " + property + " and locale " + this.locale + " not found.");
            labelValue = "[" + property + "]";
            return labelValue;
         } else {
            return MessageFormat.format(labelValue, param);
        }
    }

    /**
     * Method that finds the translation for the given property.
     * 
     * @param property The property for the translation to be found.
     * @return the translation
     */
    private String findTranslation(String property) {
        URLClassLoader translationsLoader = this.config.getLocaleTranslationsLoader();
        if (translationsLoader != null) {
            ResourceBundle translationsBundle = ResourceBundle.getBundle(config.getLocaleTranslationsBundle(),
                    this.locale, translationsLoader);
            if (translationsBundle.containsKey(property)) {
                return translationsBundle.getString(property);
            }
        }
        if (config.getDomainProperties().containsKey(property)) {
            return config.getDomainProperties().get(property);
        }
        ResourceBundle validatorBundle = ResourceBundle.getBundle("i18n.validator", this.locale);
        if (validatorBundle.containsKey(property)) {
            return validatorBundle.getString(property);
        }
        ResourceBundle commonBundle = ResourceBundle.getBundle("i18n.default", this.locale);
        if (commonBundle.containsKey(property)) {
            return commonBundle.getString(property);
        }
        return null;
    }

    /**
     * Method that checks if a given property exists for a given locale.
     * 
     * @param property The name of the property.
     * @return true if it is found, false otherwise.
     */
    public boolean propertyExists(String property) {
        return findTranslation(property) != null;
    }

    /**
     * Method that builds the string property name.
     * 
     * @param basename The base name of the property.
     * @param suffixes The specific subpart of the property.
     * @return The name of the property.
     */
    public String buildMessageName(String basename, String... suffixes) {
        String property = basename;
        for (int i = 0; i < suffixes.length; i++) {
            property = property + "." + suffixes[i];
        }
        return property;
    }
}