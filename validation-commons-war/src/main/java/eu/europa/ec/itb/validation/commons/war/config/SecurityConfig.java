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

package eu.europa.ec.itb.validation.commons.war.config;

import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import eu.europa.ec.itb.validation.commons.web.CSPNonceFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static eu.europa.ec.itb.validation.commons.web.CSPNonceFilter.CSP_NONCE_HEADER_PLACEHOLDER;

/**
 * Validator web application security configuration.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig <Y extends WebDomainConfig, X extends WebDomainConfigCache<Y>> {

    @Autowired
    private X domainConfigCache;

    /**
     * Enable CORS and disable CSRF checks.
     *
     * @param http The HTTP security configuration.
     * @return The filter chain to use.
     * @throws Exception If an error occurs.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Enable all CORS requests (see corsConfigurationSource bean).
                .cors(Customizer.withDefaults())
                // Disabling as this has issues when running behind a reverse proxy for a stateless app.
                .csrf(AbstractHttpConfigurer::disable)
                // Referrer-Policy.
                .headers(headers -> headers.referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)))
                // Permissions-Policy.
                .headers(headers -> headers.permissionsPolicyHeader(per -> per.policy("accelerometer=(), ambient-light-sensor=(), autoplay=(), battery=(), camera=(), cross-origin-isolated=(), display-capture=(), document-domain=(), encrypted-media=(), execution-while-not-rendered=(), execution-while-out-of-viewport=(), fullscreen=(), geolocation=(), gyroscope=(), keyboard-map=(), magnetometer=(), microphone=(), midi=(), navigation-override=(), payment=(), picture-in-picture=(), publickey-credentials-get=(), screen-wake-lock=(), sync-xhr=(self), usb=(), web-share=(), xr-spatial-tracking=(), clipboard-read=(), clipboard-write=(), gamepad=(), speaker-selection=()")))
                // XSS mode block.
                .headers(headers -> headers.xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)))
                // CSP configuration.
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp
                        .policyDirectives("script-src 'self' 'nonce-"+CSP_NONCE_HEADER_PLACEHOLDER+"'")))
                .addFilterBefore(new CSPNonceFilter(), HeaderWriterFilter.class)
                // Disable authentication.
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        if (hasEmbeddableValidator()) {
            // Disabling X-Frame-Options by default to set it by domain (see HeaderFilter).
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        } else {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny));
        }
        return http.build();
    }

    /**
     * Check to see whether this validator includes a validator configuration that supports embedding.
     *
     * @return The check result.
     */
    private boolean hasEmbeddableValidator() {
        for (var config: domainConfigCache.getAllDomainConfigurations()) {
            if (config.isSupportUserInterfaceEmbedding()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Configure CORS as allowed.
     *
     * @return The CORS configuration.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowedMethods(List.of("OPTIONS", "HEAD", "GET", "PUT", "POST", "DELETE", "PATCH"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}