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

package eu.europa.ec.itb.validation.commons.web.filters;

import eu.europa.ec.itb.validation.commons.RemoteResourceBudget;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that applies the limits on remote resource lookups to each request. All remote lookups made while a request is
 * processed (e.g. the loading of imported schemas or contexts) count towards a budget that is specific to the request.
 * This applies to the web UI, as well as the REST and SOAP APIs.
 */
@Component
@Order(3)
public class RemoteBudgetFilter extends OncePerRequestFilter {

    @Autowired
    private ApplicationConfig appConfig;

    /**
     * Process the request with the remote resource limits applied.
     * <p>
     * {{@inheritDoc}}
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try (var ignored = RemoteResourceBudget.open(appConfig.getRemoteLimits())) {
            filterChain.doFilter(request, response);
        }
    }

}
