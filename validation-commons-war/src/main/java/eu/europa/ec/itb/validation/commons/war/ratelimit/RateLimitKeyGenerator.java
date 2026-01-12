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

package eu.europa.ec.itb.validation.commons.war.ratelimit;

import eu.europa.ec.itb.validation.commons.RateLimitPolicy;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.function.Function;

/**
 * Component used to create the key to use for rate limit checking of requests.
 */
public class RateLimitKeyGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitKeyGenerator.class);
    private final Function<HttpServletRequest, String> addressExtractor;

    /**
     * Constructor.
     *
     * @param config The application configuration.
     */
    public RateLimitKeyGenerator(ApplicationConfig.RateLimit config) {
        if (config.getIpHeader() == null || config.getIpHeader().isEmpty()) {
            // No IP header configured - use the remote address as stated in the request.
            addressExtractor = ServletRequest::getRemoteAddr;
        } else {
            // IP header configured (in case of proxy usage) - use this with the remote address as stated in the request as the fallback.
            addressExtractor = (request) -> {
                String headerValue = request.getHeader(config.getIpHeader());
                if (headerValue == null) {
                    LOG.warn("Expected address header [{}] not found in request.", config.getIpHeader());
                    return request.getRemoteAddr();
                } else {
                    return headerValue;
                }
            };
        }
    }

    /**
     * Generate the key to use for the provided request and policy.
     *
     * @param request The request to check.
     * @param policy The applicable policy.
     * @return The key to use.
     */
    public String generate(HttpServletRequest request, RateLimitPolicy policy) {
        String originalAddress = addressExtractor.apply(request);
        String normalizedAddress = normalizeAddress(originalAddress);
        return normalizedAddress+"|"+policy.getConfigurationKey();
    }

    /**
     * Ensure IPv6 addresses are normalised to ensure common treatment of the same logical addresses.
     *
     * @param ip The address to process.
     * @return The normalised address.
     */
    private String normalizeAddress(String ip) {
        try {
            // Normalize IPv6 addresses.
            return InetAddress.getByName(ip).getHostAddress();
        } catch (Exception e) {
            // Fall back to original address.
            return ip;
        }
    }

}
