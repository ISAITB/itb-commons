package eu.europa.ec.itb.validation.commons.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.config.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.web.dto.UploadResult;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static eu.europa.ec.itb.validation.commons.web.Constants.IS_MINIMAL;

/**
 * Abstract super class of all controllers for validator UI forms.
 */
public abstract class BaseUploadController <X extends WebDomainConfig, Y extends DomainConfigCache<X>> {

    /** The content type value in case of a file upload. */
    public static final String CONTENT_TYPE_FILE = "fileType" ;
    /** The content type value in case of a provided URI. */
    public static final String CONTENT_TYPE_URI = "uriType" ;
    /** The content type value in case of text from an editor. */
    public static final String CONTENT_TYPE_STRING = "stringType" ;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get the request's relevant domain configuration object.
     *
     * @param request The request.
     * @return The configuration.
     */
    public X getDomainConfig(HttpServletRequest request) {
        var domain = (X)request.getAttribute(WebDomainConfig.DOMAIN_CONFIG_REQUEST_ATTRIBUTE);
        if (domain == null) {
            throw new NotFoundException();
        }
        return domain;
    }

    /**
     * Write the validation result to a string.
     *
     * @param result The upload result.
     * @return The serialised string.
     */
    public String writeResultToString(UploadResult<?> result) {
        try {
            var writer = new StringWriter();
            objectMapper.writeValue(writer, result);
            return writer.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Error while serialising validation result", e);
        }
    }

    /**
     * Check and/or determine the input submission type based on the received information.
     * This will fail if the content type could not be determined.
     *
     * @param contentType The declared content type.
     * @param file The received file input (or null).
     * @param uri The received URI input (or null).
     * @param string The received text input (or null).
     * @return The content type value to consider.
     */
    public String checkInputType(String contentType, MultipartFile file, String uri, String string) {
        return checkInputType(contentType, file, uri, string, true);
    }

    /**
     * Check and/or determine the input submission type based on the received information.
     *
     * @param contentType The declared content type.
     * @param file The received file input (or null).
     * @param uri The received URI input (or null).
     * @param string The received text input (or null).
     * @param failIfUndetermined Raise an error if the content type could not be determined.
     * @return The content type value to consider.
     */
    public String checkInputType(String contentType, MultipartFile file, String uri, String string, boolean failIfUndetermined) {
        if (StringUtils.isEmpty(contentType)) {
            var fileProvided = file != null;
            var uriProvided = StringUtils.isNotEmpty(uri);
            var textProvided = StringUtils.isNotEmpty(string);
            if (fileProvided && !uriProvided && !textProvided) {
                contentType = CONTENT_TYPE_FILE;
            } else if (!fileProvided && uriProvided && !textProvided) {
                contentType = CONTENT_TYPE_URI;
            } else if (!fileProvided && !uriProvided && textProvided) {
                contentType = CONTENT_TYPE_STRING;
            } else if (failIfUndetermined) {
                throw new IllegalArgumentException("No explicit content type was declared and determining it from the provided inputs was not possible.");
            }
        }
        return contentType;
    }

    /**
     * Check to see if the UI should be displayed as minimal.
     *
     * @see eu.europa.ec.itb.validation.commons.web.filters.DomainCheckFilter
     *
     * @param request The current request.
     * @return The check result.
     */
    public boolean isMinimalUI(HttpServletRequest request) {
        return Objects.requireNonNullElse((Boolean) request.getAttribute(IS_MINIMAL), Boolean.FALSE);
    }

    /**
     * Get a JSON string containing the configuration for labels that are type and/or type and option sensitive.
     * <p>
     * The typeRelated and typeAndOptionRelated lists are pairs of Strings where the left is the property prefix from the
     * domain configuration file (before the type postfix), and the right is the JSON property name to use.
     *
     * @param localisationHelper The helper used for localisation.
     * @param config The domain configuration.
     * @param typeRelated The label properties that are sensitive to the selected type.
     * @param typeAndOptionRelated The label properties that are sensitive to the selected type and option.
     * @return The JSON text to inject in the HTML as configuration.
     */
    public String getDynamicLabelConfiguration(LocalisationHelper localisationHelper, X config, List<Pair<String, String>> typeRelated, List<Pair<String, String>> typeAndOptionRelated) {
        var translations = new LinkedHashMap<String, String>();
        if (config.hasValidationTypeOptions()) {
            addTypeTranslations(translations, localisationHelper, config, "validator.label.optionLabel", "option");
        }
        // Process label translations.
        typeRelated.forEach(entry -> addTypeTranslations(translations, localisationHelper, config, entry.getLeft(), entry.getRight()));
        typeAndOptionRelated.forEach(entry -> addTypeAndOptionTranslations(translations, localisationHelper, config, entry.getLeft(), entry.getRight()));
        try {
            return objectMapper.writeValueAsString(translations);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialise label configuration", e);
        }
    }

    /**
     * Add translations for type-sensitive labels.
     *
     * @param translations The current translations.
     * @param localisationHelper The localisation helper.
     * @param config The domain configuration.
     * @param configPropertyName The configuration property name.
     * @param jsonPropertyName The JSON property name.
     */
    private void addTypeTranslations(Map<String, String> translations, LocalisationHelper localisationHelper, X config, String configPropertyName, String jsonPropertyName) {
        translations.put(jsonPropertyName, localisationHelper.localise(configPropertyName));
        for (var validationType: config.getDeclaredType()) {
            addTranslation(translations, localisationHelper, configPropertyName, jsonPropertyName, validationType);
        }
    }

    /**
     * Add translations for type/option-sensitive labels.
     *
     * @param translations The current translations.
     * @param localisationHelper The localisation helper.
     * @param config The domain configuration.
     * @param configPropertyName The configuration property name.
     * @param jsonPropertyName The JSON property name.
     */
    private void addTypeAndOptionTranslations(Map<String, String> translations, LocalisationHelper localisationHelper, X config, String configPropertyName, String jsonPropertyName) {
        addTypeTranslations(translations, localisationHelper, config, configPropertyName, jsonPropertyName);
        for (var validationType: config.getValidationTypeOptions().entrySet()) {
            for (var option: validationType.getValue()) {
                addTranslation(translations, localisationHelper, configPropertyName, jsonPropertyName, validationType.getKey() + "." + option);
            }
        }
    }

    /**
     * Add a translation for a type or type/option sensitive label.
     *
     * @param translations The current translations.
     * @param localisationHelper The localisation helper.
     * @param propertyPrefix The prefix of the configuration property.
     * @param jsonPrefix The prefix of the resulting JSON property.
     * @param propertyPostfix The prefix of the configuration and JSON property.
     */
    private void addTranslation(Map<String, String> translations, LocalisationHelper localisationHelper, String propertyPrefix, String jsonPrefix, String propertyPostfix) {
        var messageKey = propertyPrefix+"."+propertyPostfix;
        if (localisationHelper.propertyExists(messageKey)) {
            translations.put(jsonPrefix+"."+propertyPostfix, localisationHelper.localise(messageKey));
        }
    }

}
