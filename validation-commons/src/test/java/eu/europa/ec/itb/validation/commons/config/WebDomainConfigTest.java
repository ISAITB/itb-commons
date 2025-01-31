package eu.europa.ec.itb.validation.commons.config;

import com.gitb.core.ValidationModule;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @Test
    void testGroupPresentation() {
        var config = new WebDomainConfig();
        config.setDeclaredType(List.of("type1", "type2", "type3", "type4"));
        config.setValidationTypeOptions(Collections.emptyMap());
        config.setValidationTypeGroups(Map.of("group1", List.of("type1", "type2"), "group2", List.of("type3", "type4")));
        config.setGroupPresentation(GroupPresentationEnum.SPLIT);
        assertTrue(config.hasSplitGroups());
        assertFalse(config.hasInlineGroups());
        config.setHiddenTypes(List.of("type1"));
        assertFalse(config.isHiddenGroup("group1"));
        config.setHiddenTypes(List.of("type1", "type2"));
        assertTrue(config.isHiddenGroup("group1"));
        config.setGroupPresentation(GroupPresentationEnum.INLINE);
        assertFalse(config.hasSplitGroups());
        assertTrue(config.hasInlineGroups());
    }

}
