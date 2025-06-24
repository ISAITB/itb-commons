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

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the embedded Tomcat server.
 */
@Configuration
public class TomcatConfig {

    /**
     * Customise the Tomcat server connector.
     *
     * @return The factory bean.
     */
    @Bean
    public TomcatServletWebServerFactory tomcatFactory() {
        return new TomcatServletWebServerFactory() {
            @Override
            protected void customizeConnector(org.apache.catalina.connector.Connector connector) {
                super.customizeConnector(connector);
                /*
                 * Requests received by the validator web app are multipart requests (if performed
                 * through the UI). In this case we need to make sure that Tomcat's limit to the
                 * maximum number of multipart request parts does not block us (a default limit of
                 * 10 was added in Tomcat release 10.1.42).
                 */
                connector.setMaxParameterCount(10000);
                connector.setMaxPartCount(100);
            }
        };
    }

}
