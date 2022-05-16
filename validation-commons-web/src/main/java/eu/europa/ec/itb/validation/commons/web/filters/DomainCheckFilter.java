package eu.europa.ec.itb.validation.commons.web.filters;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static eu.europa.ec.itb.validation.commons.web.Constants.IS_MINIMAL;
import static eu.europa.ec.itb.validation.commons.web.Constants.MDC_DOMAIN;

/**
 * Filter to validate a given request for the validator's user interface. It checks that the requested domain is valid,
 * recording it for downstream use, and also ensured that the type of interface requested is supported.
 *
 * @param <Y> The type of web domain configuration.
 * @param <X> The type of web domain configuration cache.
 */
@Component
@Order(1)
public class DomainCheckFilter <Y extends WebDomainConfig, X extends WebDomainConfigCache<Y>> extends OncePerRequestFilter {

    private final RequestMatcher normalUIPath = new AntPathRequestMatcher("/**/upload");
    private final RequestMatcher minimalUIPath = new AntPathRequestMatcher("/**/uploadm");

    @Autowired
    private X domainConfigs;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var uploadRequest = false;
        var isMinimal = false;
        if (normalUIPath.matches(request)) {
            setMinimalUIFlag(request, false);
            uploadRequest = true;
        } else if (minimalUIPath.matches(request)) {
            setMinimalUIFlag(request, true);
            isMinimal = true;
            uploadRequest = true;
        }
        if (uploadRequest) {
            var domainName = extractDomainName(request);
            if (domainName == null) {
                logger.error("No domain could be determined from upload request");
                throw new NotFoundException();
            } else {
                var domainConfig = validateDomain(domainName, isMinimal);
                request.setAttribute(WebDomainConfig.DOMAIN_CONFIG_REQUEST_ATTRIBUTE, domainConfig);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Validates that the domain exists and the requested usage is supported, returning it if so.
     *
     * @param domain    The domain to check.
     * @param isMinimal Whether this is a minimal UI.
     * @return The retrieved domain configuration.
     * @throws NotFoundException If the domain doesn't exist or the requested usage is unsupported.
     */
    private Y validateDomain(String domain, boolean isMinimal) {
        try {
            var config = domainConfigs.getConfigForDomainName(domain);
            if (config == null || !config.isDefined() || !config.getChannels().contains(ValidatorChannel.FORM)) {
                logger.error(String.format("The following domain does not exist: %s", domain));
                throw new NotFoundException();
            }
            MDC.put(MDC_DOMAIN, domain);
            if (isMinimal && !config.isSupportMinimalUserInterface()) {
                logger.error(String.format("Minimal user interface is not supported in this domain [%s].", config.getDomainName()));
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
    private void setMinimalUIFlag(HttpServletRequest request, boolean isMinimal) {
        if (request.getAttribute(IS_MINIMAL) == null) {
            request.setAttribute(IS_MINIMAL, isMinimal);
        }
    }

    /**
     * Extract the domain name from the provided HTTP request.
     *
     * @param request The request.
     * @return The domain name.
     */
    private String extractDomainName(HttpServletRequest request) {
        var pathParts = StringUtils.split(request.getRequestURI(), '/');
        // We want the part before last (this is the domain)
        var index = pathParts.length - 2;
        if (index >= 0) {
            return pathParts[index];
        }
        return null;
    }

}
