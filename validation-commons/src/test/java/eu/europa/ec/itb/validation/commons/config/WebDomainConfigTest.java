package eu.europa.ec.itb.validation.commons.config;

import com.gitb.core.ValidationModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebDomainConfigTest {

    @Test
    void testApplyWebServiceMetadata() {
        var config = new WebDomainConfig();
        var module = new ValidationModule();
        config.applyWebServiceMetadata(module);
        assertEquals("ValidationService", module.getId());
        assertEquals("V", module.getOperation());
        assertNotNull(module.getMetadata());
        assertEquals("ValidationService", module.getMetadata().getName());
        assertEquals("1.0.0", module.getMetadata().getVersion());

        module = new ValidationModule();
        config.setWebServiceId("id");
        config.setValidationServiceName("Name");
        config.setValidationServiceVersion("Version");
        config.applyWebServiceMetadata(module);
        assertNotNull(module.getMetadata());
        assertEquals("Name", module.getMetadata().getName());
        assertEquals("Version", module.getMetadata().getVersion());
    }

}
