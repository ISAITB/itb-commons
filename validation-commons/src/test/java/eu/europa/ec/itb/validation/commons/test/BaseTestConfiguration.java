package eu.europa.ec.itb.validation.commons.test;

import eu.europa.ec.itb.validation.commons.URLReader;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfigCache;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class BaseTestConfiguration {

    @Bean
    public ApplicationConfig appConfig() {
        return Mockito.mock(ApplicationConfig.class);
    }

    @Bean
    public DomainConfigCache<DomainConfig> domainConfigCache() {
        return Mockito.mock(DomainConfigCache.class);
    }

    @Bean
    public URLReader urlReader() {
        return Mockito.mock(URLReader.class);
    }

}
