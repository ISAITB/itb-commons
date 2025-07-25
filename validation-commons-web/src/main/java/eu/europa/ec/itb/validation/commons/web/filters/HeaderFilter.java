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

import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Filter to apply custom settings to HTTP headers.
 */
@Component
@Order(2)
public class HeaderFilter<Y extends WebDomainConfig, X extends WebDomainConfigCache<Y>> extends OncePerRequestFilter {

    private final List<RequestMatcher> configurablyAllowed = List.of(
            new RequestMatcher("/upload", HttpMethod.GET.name()),
            new RequestMatcher("/upload", HttpMethod.POST.name()),
            new RequestMatcher("/uploadm", HttpMethod.GET.name()),
            new RequestMatcher("/uploadm", HttpMethod.POST.name()),
            new RequestMatcher("/error", HttpMethod.POST.name())
    );

    /**
     * Applies custom headers to responses.
     * <p>
     * {{@inheritDoc}}
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var deny = true;
        if (matches(configurablyAllowed, request)) {
            var domainConfig = (Y)request.getAttribute(WebDomainConfig.DOMAIN_CONFIG_REQUEST_ATTRIBUTE);
            if (domainConfig != null && domainConfig.isSupportUserInterfaceEmbedding()) {
                deny = false;
            }
        }
        if (deny) {
            denyEmbedding(response);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Apply the necessary header to prevent embedding in iFrames.
     *
     * @param response The HTTP response.
     */
    private void denyEmbedding(HttpServletResponse response) {
        response.setHeader("X-Frame-Options", "DENY");
    }

    /**
     * Check to see if the provided request matches the defined matchers.
     *
     * @param matchers The matchers to check.
     * @param request The request.
     * @return The check result.
     */
    private boolean matches(Collection<RequestMatcher> matchers, HttpServletRequest request) {
        return matchers.stream().anyMatch(matcher -> matcher.matches(request));
    }

}
