package eu.europa.ec.itb.validation.commons.war.ratelimit;

import eu.europa.ec.itb.validation.commons.RateLimitPolicy;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.test.BaseTest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RateLimitKeyGeneratorTest extends BaseTest {

    @Test
    void testConfigurationWithIpHeader() {
        var config = new ApplicationConfig.RateLimit();
        config.setIpHeader("My-Header");
        var request = mock(HttpServletRequest.class);
        when(request.getHeader("My-Header")).thenReturn("127.0.0.1");
        when(request.getRemoteAddr()).thenReturn("127.0.0.2");
        var requestWithoutHeader = mock(HttpServletRequest.class);
        when(requestWithoutHeader.getHeader("My-Header")).thenReturn(null);
        when(requestWithoutHeader.getRemoteAddr()).thenReturn("127.0.0.2");

        var generator = new RateLimitKeyGenerator(config);
        var key1 = generator.generate(request, RateLimitPolicy.UI_VALIDATE);
        assertNotNull(key1);
        assertEquals("127.0.0.1|"+RateLimitPolicy.UI_VALIDATE.getConfigurationKey(), key1);
        var key2 = generator.generate(requestWithoutHeader, RateLimitPolicy.UI_VALIDATE);
        assertNotNull(key2);
        assertEquals("127.0.0.2|"+RateLimitPolicy.UI_VALIDATE.getConfigurationKey(), key2);
    }

    @Test
    void testConfigurationWithoutIpHeader() {
        var config = new ApplicationConfig.RateLimit();
        var request = mock(HttpServletRequest.class);
        when(request.getHeader("My-Header")).thenReturn("127.0.0.1");
        when(request.getRemoteAddr()).thenReturn("127.0.0.2");

        var generator = new RateLimitKeyGenerator(config);
        var key = generator.generate(request, RateLimitPolicy.UI_VALIDATE);
        assertNotNull(key);
        assertEquals("127.0.0.2|"+RateLimitPolicy.UI_VALIDATE.getConfigurationKey(), key);
    }

}
