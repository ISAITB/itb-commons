package eu.europa.ec.itb.validation.commons.web.rest;

import com.gitb.core.ValueEmbeddingEnumeration;
import eu.europa.ec.itb.validation.commons.web.rest.model.ApiInfo;
import eu.europa.ec.itb.validation.commons.web.rest.model.Output;
import eu.europa.ec.itb.validation.commons.web.rest.model.SchemaInfo;
import eu.europa.ec.itb.validation.commons.web.rest.model.ValidationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RestModelTest {

    @Test
    void testApiInfo() {
        var info = new ApiInfo();
        info.setDomain("domain");
        var type = new ValidationType();
        type.setDescription("desc");
        type.setType("type");
        var types = List.of(type);
        info.setValidationTypes(types);
        assertEquals("domain", info.getDomain());
        assertSame(info.getValidationTypes(), types);
        assertEquals("desc", info.getValidationTypes().get(0).getDescription());
        assertEquals("type", info.getValidationTypes().get(0).getType());
    }

    @Test
    void testOutput() {
        var output = new Output();
        output.setReport("report");
        assertEquals("report", output.getReport());
    }

    @Test
    void testSchemaInfo() {
        var info = new SchemaInfo();
        info.setEmbeddingMethod("STRING");
        info.setSchema("SCHEMA");
        var fileContent = info.toFileContent();
        assertNotNull(fileContent);
        assertEquals(info.getSchema(), fileContent.getContent());
        assertEquals(ValueEmbeddingEnumeration.STRING, fileContent.getEmbeddingMethod());
    }
}
