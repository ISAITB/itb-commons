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

package eu.europa.ec.itb.validation.commons.war.ratelimit;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import org.apache.cxf.Bus;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Bean configuration class to manage the components involved in validation rate limiting.
 */
@Configuration
@ConditionalOnProperty(name = ApplicationConfig.RATE_LIMIT_ENABLED_PROPERTY, havingValue = "true")
public class RateLimitConfig {

    /**
     * Construct the rate limiter service.
     *
     * @param config The application configuration.
     * @return The bean.
     */
    @Bean
    public RateLimiterService rateLimiterService(ApplicationConfig config) {
        return new RateLimiterService(config.getRateLimit());
    }

    /**
     * Construct the rate limit key generation component.
     *
     * @param config The application configuration.
     * @return The bean.
     */
    @Bean
    public RateLimitKeyGenerator rateLimitKeyGenerator(ApplicationConfig config) {
        return new RateLimitKeyGenerator(config.getRateLimit());
    }

    /**
     * Construct a WebMvcConfigurer to include the necessary interceptor in the Web MVC calls.
     *
     * @param rateLimiter The rate limiter service.
     * @param keyGenerator The key generator.
     * @return The bean.
     */
    @Bean
    public WebMvcConfigurer rateLimitMvcConfigurer(RateLimiterService rateLimiter, RateLimitKeyGenerator keyGenerator) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(@NonNull InterceptorRegistry registry) {
                registry.addInterceptor(new RateLimitInterceptor(rateLimiter, keyGenerator))
                        .addPathPatterns("/**/upload", "/**/uploadm") // UI
                        .addPathPatterns("/**/api/**"); // REST API
            }
        };
    }

    /**
     * Construct the CXF interceptor responsible for checking rate limits in SOAP validation calls.
     *
     * @param rateLimiter The rate limiter.
     * @param keyGenerator The key generator.
     * @param bus The CXF bus.
     * @return The bean.
     */
    @Bean
    public RateLimitSoapInterceptor rateLimitSoapInterceptor(RateLimiterService rateLimiter, RateLimitKeyGenerator keyGenerator, Bus bus) {
        RateLimitSoapInterceptor interceptor = new RateLimitSoapInterceptor(rateLimiter, keyGenerator);
        bus.getInInterceptors().add(interceptor);
        return interceptor;
    }

}
