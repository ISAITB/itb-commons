package eu.europa.ec.itb.validation.commons.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.itb.validation.commons.config.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.web.dto.UploadResult;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;

/**
 * Abstract super class of all controllers for validator UI forms.
 */
public abstract class BaseUploadController <X extends WebDomainConfig, Y extends DomainConfigCache<X>> {

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

}
