package eu.europa.ec.itb.validation.commons.war.ratelimit;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import org.apache.cxf.Bus;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class RateLimitConfigTest {

    @Test
    void testBeanCreation() {
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.getRateLimit()).thenReturn(new ApplicationConfig.RateLimit());
        var config = new RateLimitConfig();
        var rateLimiter = config.rateLimiterService(appConfig);
        assertNotNull(rateLimiter);
        var generator = config.rateLimitKeyGenerator(appConfig);
        assertNotNull(generator);
        var configurer = config.rateLimitMvcConfigurer(rateLimiter, generator);
        assertNotNull(configurer);
        var registry = mock(InterceptorRegistry.class);
        when(registry.addInterceptor(any(RateLimitInterceptor.class))).thenAnswer(invocation -> new InterceptorRegistration(invocation.getArgument(0)));
        configurer.addInterceptors(registry);
        verify(registry, times(1)).addInterceptor(any(RateLimitInterceptor.class));
        var bus = mock(Bus.class);
        when(bus.getInInterceptors()).thenReturn(new ArrayList<>());
        assertNotNull(config.rateLimitSoapInterceptor(rateLimiter, generator, bus));
        verify(bus, times(1)).getInInterceptors();
    }

}
