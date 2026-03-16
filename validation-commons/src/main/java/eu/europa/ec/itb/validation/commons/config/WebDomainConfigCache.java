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

package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Base class for the loading and storing of domain configuration for validators that are web applications.
 *
 * @param <T> The specific domain configuration class of the validator.
 */
public abstract class WebDomainConfigCache <T extends WebDomainConfig> extends DomainConfigCache<T> {

    /**
     * @param domainName The domain's name.
     * @return The domain configuration.
     */
    @Override
    public T getConfigForDomainName(String domainName) {
        return super.getConfigForDomainName(domainName);
    }

    /**
     * @param domain The domain identifier.
     * @return The domain configuration.
     */
    @Override
    public T getConfigForDomain(String domain) {
        return super.getConfigForDomain(domain);
    }

    /**
     * Initialise (once) and return all domain configurations.
     *
     * @return The domain configurations.
     */
    @Override
    public List<T> getAllDomainConfigurations() {
        return super.getAllDomainConfigurations();
    }

    /**
     * Enrich the domain configuration using the provided properties (for web-related information).
     *
     * @param domainConfig The domain configuration to enrich.
     * @param config The configuration properties to consider.
     */
    @Override
    protected void addDomainConfiguration(T domainConfig, Configuration config) {
        super.addDomainConfiguration(domainConfig, config);
        domainConfig.setWebServiceId(config.getString("validator.webServiceId", "ValidatorService"));
        domainConfig.setWebServiceDescription(ParseUtils.parseMap("validator.webServiceDescription", config));
        domainConfig.setShowAbout(config.getBoolean("validator.showAbout", true));
        domainConfig.setSupportMinimalUserInterface(config.getBoolean("validator.supportMinimalUserInterface", false));
        domainConfig.setSupportUserInterfaceEmbedding(domainConfig.getChannels().contains(ValidatorChannel.FORM) && config.getBoolean("validator.supportUserInterfaceEmbedding", true));
        domainConfig.setHiddenTypes(Arrays.stream(StringUtils.split(config.getString("validator.hiddenType", ""), ',')).map(String::trim).toList());
        domainConfig.setOAuthManagers(ParseUtils.parseObjectMap("validator.oAuthService", config, this::toOAuthConfiguration));
    }

    /**
     * Parse the configuration of an OAuth2.0 service to use when authenticating remote artifact lookups.
     *
     * @param configName The configuration name.
     * @param configValues The configuration values.
     * @return The service configuration.
     */
    private AuthorizedClientServiceOAuth2AuthorizedClientManager toOAuthConfiguration(String configName, Map<String, String> configValues) {
        ClientRegistration registration = ClientRegistration
                .withRegistrationId(configName)
                .clientId(configValues.get("clientId"))
                .clientSecret(configValues.get("clientSecret"))
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri(configValues.get("tokenUrl"))
                .build();
        var repo = new InMemoryClientRegistrationRepository(registration);
        var service = new InMemoryOAuth2AuthorizedClientService(repo);
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(repo, service);
        manager.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build());
        return manager;
    }
}
