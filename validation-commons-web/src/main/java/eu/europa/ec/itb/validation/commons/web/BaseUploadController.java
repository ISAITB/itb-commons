package eu.europa.ec.itb.validation.commons.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.web.dto.UploadResult;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;

import static eu.europa.ec.itb.validation.commons.web.Constants.IS_MINIMAL;
import static eu.europa.ec.itb.validation.commons.web.Constants.MDC_DOMAIN;

/**
 * Abstract super class of all controllers for validator UI forms.
 */
public abstract class BaseUploadController <X extends WebDomainConfig, Y extends DomainConfigCache<X>> {

    private static final Logger logger = LoggerFactory.getLogger(BaseUploadController.class);

    @Autowired
    private Y domainConfigs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validates that the domain exists and the requested usage is supported, returning it if so.
     *
     * @param request The HTTP request.
     * @param domain The domain to check.
     * @return The retrieved domain configuration.
     * @throws NotFoundException If the domain doesn't exist or the requested usage is unsupported.
     */
    public X validateDomain(HttpServletRequest request, String domain) {
        try {
            var config = domainConfigs.getConfigForDomainName(domain);
            if (config == null || !config.isDefined() || !config.getChannels().contains(ValidatorChannel.FORM)) {
                logger.error("The following domain does not exist: {}", domain);
                throw new NotFoundException();
            }
            request.setAttribute(WebDomainConfig.DOMAIN_CONFIG_REQUEST_ATTRIBUTE, config);
            MDC.put(MDC_DOMAIN, domain);
            // Check minimal UI.
            setMinimalUIFlag(request, false); // Set to false if not already set.
            if ((Boolean)request.getAttribute(IS_MINIMAL) && !config.isSupportMinimalUserInterface()) {
                logger.error("Minimal user interface is not supported in this domain [{}].", config.getDomainName());
                throw new NotFoundException();
            }
            return config;
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new NotFoundException();
        }
    }

    /**
     * Record whether the current request is through a minimal UI.
     *
     * @param request The current request.
     * @param isMinimal True in case of the minimal UI being used.
     */
    public void setMinimalUIFlag(HttpServletRequest request, boolean isMinimal) {
        if (request.getAttribute(IS_MINIMAL) == null) {
            request.setAttribute(IS_MINIMAL, isMinimal);
        }
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
