/*
 * Copyright (C) 2025 European Union
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

package eu.europa.ec.itb.validation.commons.web.filters;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static eu.europa.ec.itb.validation.commons.web.Constants.*;

/**
 * Filter to validate a given request for the validator's user interface. It checks that the requested domain is valid,
 * recording it for downstream use, and also ensures that the type of interface requested is supported.
 *
 * @param <Y> The type of web domain configuration.
 * @param <X> The type of web domain configuration cache.
 */
@Component
@Order(1)
public class DomainCheckFilter <Y extends WebDomainConfig, X extends WebDomainConfigCache<Y>> extends OncePerRequestFilter {

    private final RequestMatcher normalUIPath = new RequestMatcher("/upload");
    private final RequestMatcher minimalUIPath = new RequestMatcher("/uploadm");

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
            // Record whether this is a form submission from the validator's own UI.
            setSelfSubmittedFlag(request);
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
            if (config.getDomainAlias() != null) {
                // The requested domain is marked as an alias of another domain.
                config = domainConfigs.getConfigForDomainName(config.getDomainAlias());
            }
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
     * Record whether a submission was originated from the validator's own upload form UI.
     *
     * @param request The request to check and update.
     */
    private void setSelfSubmittedFlag(HttpServletRequest request) {
        if (request.getAttribute(IS_SELF_SUBMITTED) == null) {
            request.setAttribute(IS_SELF_SUBMITTED, "self".equals(request.getHeader(SUBMIT_SOURCE_HEADER)));
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
