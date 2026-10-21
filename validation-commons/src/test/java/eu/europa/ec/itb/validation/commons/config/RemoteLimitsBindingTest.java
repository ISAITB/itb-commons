package eu.europa.ec.itb.validation.commons.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.unit.DataSize;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that the limits on remote resources are configured as documented for the validators.
 */
class RemoteLimitsBindingTest {

    /**
     * Mirrors the way the limits are exposed by the application configuration, which is bound with the "validator" prefix.
     */
    public static class Holder {

        private final ApplicationConfig.RemoteLimits remoteLimits = new ApplicationConfig.RemoteLimits();

        public ApplicationConfig.RemoteLimits getRemoteLimits() {
            return remoteLimits;
        }

    }

    @Test
    void testDefaultsApplyWhenNothingIsConfigured() {
        var limits = new ApplicationConfig.RemoteLimits();
        assertEquals(DataSize.ofMegabytes(50), limits.getMaxResourceSize());
        assertEquals(200, limits.getMaxRequests());
        assertEquals(DataSize.ofMegabytes(250), limits.getMaxTotalSize());
        assertEquals(60000L, limits.getRequestTimeout());
        assertEquals(DataSize.ofMegabytes(500), limits.getCacheMaxSize());
    }

    @Test
    void testPropertiesAreBound() {
        var holder = new Holder();
        new Binder(new MapConfigurationPropertySource(Map.of(
                "validator.remoteLimits.maxResourceSize", "10MB",
                "validator.remoteLimits.maxRequests", "20",
                "validator.remoteLimits.maxTotalSize", "1GB",
                "validator.remoteLimits.requestTimeout", "5000",
                "validator.remoteLimits.cacheMaxSize", "128KB"
        ))).bind("validator", Bindable.ofInstance(holder));
        var limits = holder.getRemoteLimits();
        assertEquals(DataSize.ofMegabytes(10), limits.getMaxResourceSize());
        assertEquals(20, limits.getMaxRequests());
        assertEquals(DataSize.ofGigabytes(1), limits.getMaxTotalSize());
        assertEquals(5000L, limits.getRequestTimeout());
        assertEquals(DataSize.ofKilobytes(128), limits.getCacheMaxSize());
    }

}
