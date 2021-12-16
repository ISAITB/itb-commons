package eu.europa.ec.itb.validation.commons.web.locale;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomLocaleResolverTest {

    @Test
    void testResolveLocaleForMissingInfo() {
        CustomLocaleResolver localeResolver = new CustomLocaleResolver();
        var result = localeResolver.resolveLocale(null, null, null, null);
        assertEquals(Locale.ENGLISH, result);
    }

    @Test
    void testResolveCorrectDefaultLocale() {
        // the language set is the default
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("lang", Locale.ENGLISH.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        DomainConfig config = getDomainConfig();
        ApplicationConfig appConfig = getApplicationConfig();
        CustomLocaleResolver localeResolver = new CustomLocaleResolver();
        assertEquals(Locale.ENGLISH.getLanguage(), localeResolver.resolveLocale(request, response, config, appConfig).getLanguage());
        assertEquals(1, response.getCookies().length);
        assertEquals(Locale.ENGLISH.getLanguage(), response.getCookies()[0].getValue());
    }

    @Test
    void testResolveUnavailableLocale() {
        // the language set is not among the available locales
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("lang", Locale.CHINESE.getLanguage());
        MockHttpServletResponse response = new MockHttpServletResponse();
        DomainConfig config = getDomainConfig();
        ApplicationConfig appConfig = getApplicationConfig();
        CustomLocaleResolver localeResolver = new CustomLocaleResolver();
        assertEquals(Locale.ENGLISH.getLanguage(), localeResolver.resolveLocale(request, response, config, appConfig).getLanguage());
        assertEquals(0, response.getCookies().length);
    }

    @Test
    void testResolveAvailableLocale() {
        // the language set is among the locales but not the default one
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("lang", Locale.GERMAN.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        DomainConfig config = getDomainConfig();
        ApplicationConfig appConfig = getApplicationConfig();
        CustomLocaleResolver localeResolver = new CustomLocaleResolver();
        assertEquals(Locale.GERMAN.getLanguage(), localeResolver.resolveLocale(request, response, config, appConfig).getLanguage());
        assertEquals(1, response.getCookies().length);
        assertEquals(Locale.GERMAN.getLanguage(), response.getCookies()[0].getValue());
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
        DomainConfig domainConfig = new WebDomainConfig();
        domainConfig.setDomainName("domain");
        domainConfig.setDefaultLocale(Locale.ENGLISH);
        domainConfig.setAvailableLocales(Set.of(Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN));
        return domainConfig;
    }

    private ApplicationConfig getApplicationConfig() {
        ApplicationConfig appConfig = new ApplicationConfig() {
        };
        appConfig.setIdentifier("identifier");
        return appConfig;
    }

}
