/*
 * Copyright (C) 2026 European Union
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

package eu.europa.ec.itb.validation.commons;

import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Class for the loading of internationalized UI messages.
 */
public class LocalisationHelper {

    private static final Logger logger = LoggerFactory.getLogger(LocalisationHelper.class);

    private final Locale locale;
    private final DomainConfig config;

    /**
     * Constructor (to ignore domain configuration).
     *
     * @param locale The Locale of the messages that should be fetched.
     */
    public LocalisationHelper(Locale locale) {
        this(null, locale);
    }

    /**
     * Constructor (to use the domain's default locale).
     *
     * @param config The DomainConfig object that contains the domain configuration.
     */
    public LocalisationHelper(DomainConfig config) {
        this(config, null);
    }

    /**
     * Constructor.
     * 
     * @param config The DomainConfig object that contains the domain configuration.
     * @param locale The Locale of the messages that should be fetched. If null this will be the domain's default locale.
     *               If that is null as well, then the overall default is English.
     */
    public LocalisationHelper(DomainConfig config, Locale locale) {
        this.config = config;
        if (locale == null) {
            if (config == null) {
                this.locale = Locale.ENGLISH;
            } else {
                this.locale = config.getDefaultLocale();
            }
        } else {
            this.locale = locale;
        }
    }

    /**
     * Method that localises a given property.
     * 
     * @param property The label to resolve.
     * @return The value of the property for a given Locale.
     */
    public String localise(String property) {
        return localise(property, (Object[]) null);
    }

    /**
     * Method that localises a parameterised message.
     *
     * @param property The property to be localised.
     * @param param The parameter to substitute.
     * @return The resulting message.
     */
    public String localise(String property, Object ... param) {
        String labelValue = findTranslation(property);
        if (labelValue == null) {
            logger.warn("Value for label {} and locale {} not found.", property, this.locale);
            labelValue = "[" + property + "]";
        } else if (param != null) {
            labelValue = MessageFormat.format(labelValue, param);
        }
        return labelValue;
    }

    /**
     * Get the locale that applies to this instance.
     *
     * @return The locale.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Method that finds the translation for the given property.
     * 
     * @param property The property for the translation to be found.
     * @return the translation
     */
    private String findTranslation(String property) {
        if (this.config != null) {
            URLClassLoader translationsLoader = this.config.getLocaleTranslationsLoader();
            if (translationsLoader != null) {
                try {
                    ResourceBundle translationsBundle = ResourceBundle.getBundle(config.getLocaleTranslationsBundle(),
                            this.locale, translationsLoader);
                    if (translationsBundle.containsKey(property)) {
                        return translationsBundle.getString(property);
                    }
                } catch (MissingResourceException e) {
                    // Ignore to continue looking up the resource bundle.
                }
            }
            if (config.getDomainProperties().containsKey(property)) {
                return config.getDomainProperties().get(property);
            }
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

}
