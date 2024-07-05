package eu.europa.ec.itb.validation.commons.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.BaseFileManager;
import eu.europa.ec.itb.validation.commons.BaseInputHelper;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.web.JsonConfig;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import eu.europa.ec.itb.validation.commons.web.rest.model.ApiInfo;
import eu.europa.ec.itb.validation.commons.web.rest.model.SchemaInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static eu.europa.ec.itb.validation.commons.Utils.limitReportItemsIfNeeded;
import static eu.europa.ec.itb.validation.commons.web.Constants.MDC_DOMAIN;

/**
 * Base class used by subclasses realising the validator's REST API controller.
 *
 * @param <T> The domain configuration type.
 * @param <X> The application configuration type.
 * @param <Y> The file manager type.
 * @param <Z> The input helper type.
 */
public abstract class BaseRestController <T extends WebDomainConfig, X extends ApplicationConfig, Y extends BaseFileManager<X>, Z extends BaseInputHelper<X, Y, T>> {

    @Autowired
    protected DomainConfigCache<T> domainConfigs;
    @Autowired
    protected Z inputHelper;
    protected ObjectMapper tarObjectMapper = JsonConfig.objectMapper();

    /**
     * Get all domains configured in this validator and their supported validation types.
     *
     * @return A list of domains coupled with their validation types.
     */
    @Operation(summary = "Get API information (all supported domains and validation types).", description="Retrieve the supported domains " +
            "and validation types configured in this validator. These are the domain and validation types that can be used as parameters " +
            "with the API's other operations.")
    @ApiResponse(responseCode = "200", description = "Success", content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = ApiInfo.class))) })
    @ApiResponse(responseCode = "500", description = "Error (If a problem occurred with processing the request)", content = @Content)
    @GetMapping(value = "/api/info", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiInfo[] infoAll() {
        var listDomainsConfig = domainConfigs.getAllDomainConfigurations().stream().filter(d -> d.isDefined() && d.getChannels().contains(ValidatorChannel.REST_API)).toList();
        ApiInfo[] listApiInfo = new ApiInfo[listDomainsConfig.size()];
        int i=0;
        for (var domainConfig : listDomainsConfig) {
            listApiInfo[i] = ApiInfo.fromDomainConfig(domainConfig);
            i++;
        }
        return listApiInfo;
    }

    /**
     * Get the validation types supported by the current domain.
     *
     * @param domain The domain.
     * @return The list of validation types.
     */
    @Operation(summary = "Get API information (for a given domain).", description = "Retrieve the supported validation types " +
            "that can be requested when calling this API's validation operations.")
    @ApiResponse(responseCode = "200", description = "Success", content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiInfo.class)) })
    @ApiResponse(responseCode = "500", description = "Error (If a problem occurred with processing the request)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Not found (for an invalid domain value)", content = @Content)
    @GetMapping(value = "/{domain}/api/info", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiInfo info(
            @Parameter(required = true, name = "domain", description = "A fixed value corresponding to the specific validation domain.",
                    examples = {
                            @ExampleObject(name="order", summary="Sample 'order' configuration", value="order", description = "The domain value to use for the demo 'order' validator."),
                            @ExampleObject(name="any", summary="Generic 'any' configuration", value = "any", description = "The domain value to use for the generic 'any' validator used to validate content with user-provided validation artefacts.")
                    }
            )
            @PathVariable("domain") String domain
    ) {
        var domainConfig = validateDomain(domain);
        return ApiInfo.fromDomainConfig(domainConfig);
    }

    /**
     * Validates that the domain exists.
     *
     * @param domain The domain identifier provided in the call.
     * @return The matched domain configuration.
     * @throws NotFoundException If the domain does not exist, or it does not support a REST API.
     */
    protected T validateDomain(String domain) {
        var domainConfig = domainConfigs.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.isDefined() || !domainConfig.getChannels().contains(ValidatorChannel.REST_API)) {
            throw new NotFoundException(domain);
        }
        MDC.put(MDC_DOMAIN, domain);
        return domainConfig;
    }

    /**
     * Parse, validate and prepare the provided external validation artifacts.
     *
     * @param domainConfig The domain configuration.
     * @param providedSchemas The provided artifacts.
     * @param validationType The validation type.
     * @param artifactType The artifact type (can be null).
     * @param parentFolder The parent folder to use for the validation run.
     * @return The list of artifact files to use.
     */
    protected List<FileInfo> getExternalSchemas(T domainConfig, List<SchemaInfo> providedSchemas, String validationType, String artifactType, File parentFolder) {
        List<SchemaInfo> schemaInfo = Objects.requireNonNullElse(providedSchemas, Collections.emptyList());
        return inputHelper.validateExternalArtifacts(
                domainConfig,
                schemaInfo.stream().map(SchemaInfo::toFileContent).toList(),
                validationType, artifactType, parentFolder);
    }

    /**
     * The report types accepted by any kind of validator.
     *
     * @return The report mime types.
     */
    protected Set<String> getSupportedReportTypes() {
        return Set.of(MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE);
    }

    /**
     * Get the first acceptable "Accept" header value from the request, with a default value if no supported
     * value is found.
     *
     * @param request The HTTP request to check.
     * @param defaultValue The default value to consider if no supported value is found.
     * @return The value.
     */
    protected String getAcceptHeader(HttpServletRequest request, String defaultValue) {
        String valueToReturn = null;
        Enumeration<String> headerValues = request.getHeaders(HttpHeaders.ACCEPT);
        while (headerValues.hasMoreElements() && valueToReturn == null) {
            String acceptHeader = headerValues.nextElement();
            for (String acceptableValue: getSupportedReportTypes()) {
                if (acceptHeader.contains(acceptableValue)) {
                    valueToReturn = acceptableValue;
                    break;
                }
            }
        }
        valueToReturn = StringUtils.defaultIfEmpty(valueToReturn, defaultValue);
        if (MediaType.TEXT_XML_VALUE.equals(valueToReturn)) {
            valueToReturn = MediaType.APPLICATION_XML_VALUE;
        }
        return valueToReturn;
    }

    /**
     * Write the TAR report as JSON to the provided output stream.
     *
     * @param outputStream The stream.
     * @param report The report.
     * @param domainConfig THe domain configuration.
     */
    protected void writeReportAsJson(OutputStream outputStream, TAR report, T domainConfig) {
        limitReportItemsIfNeeded(report, domainConfig);
        try {
            tarObjectMapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, report);
        } catch (IOException e) {
            throw new IllegalStateException("An error occurred while writing the JSON report.", e);
        }
    }

}
