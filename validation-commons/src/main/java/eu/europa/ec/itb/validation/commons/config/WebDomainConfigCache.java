package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import org.apache.commons.configuration2.Configuration;

import java.util.List;


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
        domainConfig.setSupportUserInterfaceEmbedding(domainConfig.getChannels().contains(ValidatorChannel.FORM) && config.getBoolean("validator.supportUserInterfaceEmbedding", false));
    }

}
