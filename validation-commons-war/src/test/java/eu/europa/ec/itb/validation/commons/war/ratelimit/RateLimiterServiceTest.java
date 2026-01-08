package eu.europa.ec.itb.validation.commons.war.ratelimit;

import eu.europa.ec.itb.validation.commons.RateLimitPolicy;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest extends BaseTest {

    @Test
    void testConfigurationDefault() {
        var config = new ApplicationConfig.RateLimit();
        var defaultService = new RateLimiterService(config);
        var result = defaultService.tryConsume("key1", RateLimitPolicy.UI_VALIDATE);
        assertNotNull(result);
        assertTrue(result.proceed());
        assertNull(result.secondsToWaitForRetry());
    }

    @Test
    void testConfigurationLimits() {
        var config = new ApplicationConfig.RateLimit();
        config.setEnabled(true);
        config.setCapacity(Map.of(
                RateLimitPolicy.UI_VALIDATE, 2L,
                RateLimitPolicy.REST_VALIDATE, 3L,
                RateLimitPolicy.REST_VALIDATE_MULTIPLE, 4L,
                RateLimitPolicy.SOAP_VALIDATE, 5L
        ));
        var service = new RateLimiterService(config);
        // All requests up to the limits must be allowed.
        for (int i=0; i < config.getCapacity().get(RateLimitPolicy.UI_VALIDATE); i++) assertTrue(service.tryConsume("127.0.0.1|"+RateLimitPolicy.UI_VALIDATE.getConfigurationKey(), RateLimitPolicy.UI_VALIDATE).proceed());
        for (int i=0; i < config.getCapacity().get(RateLimitPolicy.REST_VALIDATE); i++) assertTrue(service.tryConsume("127.0.0.1|"+RateLimitPolicy.REST_VALIDATE.getConfigurationKey(), RateLimitPolicy.REST_VALIDATE).proceed());
        for (int i=0; i < config.getCapacity().get(RateLimitPolicy.REST_VALIDATE_MULTIPLE); i++) assertTrue(service.tryConsume("127.0.0.1|"+RateLimitPolicy.REST_VALIDATE_MULTIPLE.getConfigurationKey(), RateLimitPolicy.REST_VALIDATE_MULTIPLE).proceed());
        for (int i=0; i < config.getCapacity().get(RateLimitPolicy.SOAP_VALIDATE); i++) assertTrue(service.tryConsume("127.0.0.1|"+RateLimitPolicy.SOAP_VALIDATE.getConfigurationKey(), RateLimitPolicy.SOAP_VALIDATE).proceed());
        // Subsequent requests should be blocked.
        assertFalse(service.tryConsume("127.0.0.1|"+RateLimitPolicy.UI_VALIDATE.getConfigurationKey(), RateLimitPolicy.UI_VALIDATE).proceed());
        assertFalse(service.tryConsume("127.0.0.1|"+RateLimitPolicy.REST_VALIDATE.getConfigurationKey(), RateLimitPolicy.REST_VALIDATE).proceed());
        assertFalse(service.tryConsume("127.0.0.1|"+RateLimitPolicy.REST_VALIDATE_MULTIPLE.getConfigurationKey(), RateLimitPolicy.REST_VALIDATE_MULTIPLE).proceed());
        assertFalse(service.tryConsume("127.0.0.1|"+RateLimitPolicy.SOAP_VALIDATE.getConfigurationKey(), RateLimitPolicy.SOAP_VALIDATE).proceed());
        // Requests for other keys should not be blocked (if below limit).
        assertTrue(service.tryConsume("127.0.0.2|"+RateLimitPolicy.UI_VALIDATE.getConfigurationKey(), RateLimitPolicy.UI_VALIDATE).proceed());
        assertTrue(service.tryConsume("127.0.0.2|"+RateLimitPolicy.REST_VALIDATE.getConfigurationKey(), RateLimitPolicy.REST_VALIDATE).proceed());
        assertTrue(service.tryConsume("127.0.0.2|"+RateLimitPolicy.REST_VALIDATE_MULTIPLE.getConfigurationKey(), RateLimitPolicy.REST_VALIDATE_MULTIPLE).proceed());
        assertTrue(service.tryConsume("127.0.0.2|"+RateLimitPolicy.SOAP_VALIDATE.getConfigurationKey(), RateLimitPolicy.SOAP_VALIDATE).proceed());
    }

    @Test
    void testWarnings() {
        var config = new ApplicationConfig.RateLimit();
        config.setEnabled(true);
        config.setWarnOnly(true);
        config.setCapacity(Map.of(RateLimitPolicy.UI_VALIDATE, 2L));
        var service = new RateLimiterService(config);
        assertTrue(service.tryConsume("127.0.0.1|"+RateLimitPolicy.UI_VALIDATE.getConfigurationKey(), RateLimitPolicy.UI_VALIDATE).proceed());
        assertTrue(service.tryConsume("127.0.0.1|"+RateLimitPolicy.UI_VALIDATE.getConfigurationKey(), RateLimitPolicy.UI_VALIDATE).proceed());
        // Should also succeed.
        assertTrue(service.tryConsume("127.0.0.1|"+RateLimitPolicy.UI_VALIDATE.getConfigurationKey(), RateLimitPolicy.UI_VALIDATE).proceed());
    }

    @Test
    void testLimitForMissingProfile() {
        var config = new ApplicationConfig.RateLimit();
        config.setEnabled(true);
        var service = new RateLimiterService(config);
        for (int i=0; i < RateLimitPolicy.UI_VALIDATE.getDefaultCapacity(); i++) assertTrue(service.tryConsume("127.0.0.1|"+RateLimitPolicy.UI_VALIDATE.getConfigurationKey(), RateLimitPolicy.UI_VALIDATE).proceed());
        assertFalse(service.tryConsume("127.0.0.1|"+RateLimitPolicy.UI_VALIDATE.getConfigurationKey(), RateLimitPolicy.UI_VALIDATE).proceed());
    }

    @Test
    void testInvalidConfigurationReplacement() {
        var config = new ApplicationConfig.RateLimit();
        config.setEnabled(true);
        config.setCapacity(new HashMap<>());
        config.getCapacity().put(RateLimitPolicy.UI_VALIDATE, null);
        config.getCapacity().put(RateLimitPolicy.REST_VALIDATE, -1L);
        var service = new RateLimiterService(config);
        for (int i=0; i < RateLimitPolicy.UI_VALIDATE.getDefaultCapacity(); i++) assertTrue(service.tryConsume("127.0.0.1|"+RateLimitPolicy.UI_VALIDATE.getConfigurationKey(), RateLimitPolicy.UI_VALIDATE).proceed());
        for (int i=0; i < RateLimitPolicy.REST_VALIDATE.getDefaultCapacity(); i++) assertTrue(service.tryConsume("127.0.0.1|"+RateLimitPolicy.REST_VALIDATE.getConfigurationKey(), RateLimitPolicy.UI_VALIDATE).proceed());
        assertFalse(service.tryConsume("127.0.0.1|"+RateLimitPolicy.UI_VALIDATE.getConfigurationKey(), RateLimitPolicy.UI_VALIDATE).proceed());
        assertFalse(service.tryConsume("127.0.0.1|"+RateLimitPolicy.REST_VALIDATE.getConfigurationKey(), RateLimitPolicy.REST_VALIDATE).proceed());
    }

}
