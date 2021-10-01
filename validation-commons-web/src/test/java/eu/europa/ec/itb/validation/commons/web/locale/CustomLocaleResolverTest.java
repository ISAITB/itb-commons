package eu.europa.ec.itb.validation.commons.web.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.LabelConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;

public class CustomLocaleResolverTest {

    @Test
    void testResolveCorrectDefaultLocale() {
        // the language set is the default
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("lang", "en");
        MockHttpServletResponse response = new MockHttpServletResponse();
        DomainConfig config = getDomainConfig();
        ApplicationConfig appConfig = getApplicationConfig();
        CustomLocaleResolver localeResolver = new CustomLocaleResolver();
        assertEquals("en", localeResolver.resolveLocale(request, response, config, appConfig).getLanguage());
        assertEquals(1, response.getCookies().length);
        assertEquals("en", response.getCookies()[0].getValue());
    }

    @Test
    void testResolveUnavailableLocale() {
        // the language set is not among the available locales
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("lang", "de");
        MockHttpServletResponse response = new MockHttpServletResponse();
        DomainConfig config = getDomainConfig();
        ApplicationConfig appConfig = getApplicationConfig();
        CustomLocaleResolver localeResolver = new CustomLocaleResolver();
        assertEquals("en", localeResolver.resolveLocale(request, response, config, appConfig).getLanguage());
        assertEquals(0, response.getCookies().length);
    }

    @Test
    void testResolveAvailableLocale() {
        // the language set is among the locales but not the default one
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("lang", "es");
        MockHttpServletResponse response = new MockHttpServletResponse();
        DomainConfig config = getDomainConfig();
        ApplicationConfig appConfig = getApplicationConfig();
        CustomLocaleResolver localeResolver = new CustomLocaleResolver();
        assertEquals("es", localeResolver.resolveLocale(request, response, config, appConfig).getLanguage());
        assertEquals(1, response.getCookies().length);
        assertEquals("es", response.getCookies()[0].getValue());
    }

    @Test
    void testResolveUnsetLocale() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        DomainConfig config = getDomainConfig();
        ApplicationConfig appConfig = getApplicationConfig();
        CustomLocaleResolver localeResolver = new CustomLocaleResolver();
        assertEquals("en", localeResolver.resolveLocale(request, response, config, appConfig).getLanguage());
        assertEquals(0, response.getCookies().length);
    }

    private DomainConfig getDomainConfig() {
        DomainConfig domainConfig = new WebDomainConfig<LabelConfig>() {
            @Override
            protected LabelConfig newLabelConfig() {
                return null;
            }
        };
        domainConfig.setDomainName("domain");
        domainConfig.setDefaultLocale("en");
        domainConfig.setAvailableLocales(Set.of(new String[] { "en", "fr", "es" }));
        return domainConfig;
    }

    private ApplicationConfig getApplicationConfig() {
        ApplicationConfig appConfig = new ApplicationConfig() {
        };
        appConfig.setIdentifier("identifier");
        return appConfig;
    }

}
