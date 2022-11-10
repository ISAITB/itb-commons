package eu.europa.ec.itb.validation.commons.web.rest;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "validator.docs.licence.description=LicenceDescription",
        "validator.docs.licence.url=Url",
        "validator.docs.version=Version",
        "validator.docs.title=Title",
        "validator.docs.description=Description"
})
@ContextConfiguration(classes = { OpenApiDocumentationConfig.class })
class OpenApiDocumentationConfigTest {

    @Autowired
    ApplicationContext ctx;

    @Test
    void testConfig() {
        var api = ctx.getBean(OpenAPI.class);
        assertNotNull(api);
        assertNotNull(api.getInfo());
        assertEquals("Title", api.getInfo().getTitle());
        assertEquals("LicenceDescription", api.getInfo().getLicense().getName());
        assertEquals("Url", api.getInfo().getLicense().getUrl());
        assertEquals("Description", api.getInfo().getDescription());
        assertEquals("Version", api.getInfo().getVersion());
    }

}
