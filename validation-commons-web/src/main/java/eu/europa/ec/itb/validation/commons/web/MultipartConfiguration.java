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

package eu.europa.ec.itb.validation.commons.web;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to ensure that the web server stores the parts of uploaded multipart requests in the validator's own
 * temporary folder, as opposed to the OS temporary folder.
 * <p>
 * The location is not configured via the {@code spring.servlet.multipart.location} property because the web server
 * interprets a relative path as relative to its own work folder (that is created in the OS temporary folder). A configured
 * {@code validator.tmpFolder} such as "/validator/tmp" is not absolute on Windows, in which case the web server would
 * (a) create and use a folder in the OS temporary folder, and (b) not use the folder that the validator prepares.
 * The location is instead always set as an absolute path, matching the folder prepared by the validator
 * (see {@link ApplicationConfig#getMultipartTmpFolder()}). The remaining multipart settings (e.g. the maximum sizes)
 * are still taken from the {@code spring.servlet.multipart.*} properties.
 */
@Configuration
public class MultipartConfiguration {

    /**
     * Create the multipart configuration, which takes the place of the one otherwise created by Spring Boot.
     *
     * @param multipartProperties The multipart properties.
     * @param appConfig The application configuration.
     * @return The configuration.
     */
    @Bean
    public MultipartConfigElement multipartConfigElement(MultipartProperties multipartProperties, ApplicationConfig appConfig) {
        multipartProperties.setLocation(appConfig.getMultipartTmpFolder().getPath());
        return multipartProperties.createMultipartConfig();
    }

}
