package eu.europa.ec.itb.validation.commons.web.rest;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.*;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.web.JsonConfig;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import eu.europa.ec.itb.validation.commons.web.rest.model.SchemaInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BaseRestControllerTest {

    BaseRestController<WebDomainConfig, ApplicationConfig, BaseFileManager<ApplicationConfig>, BaseInputHelper<ApplicationConfig, BaseFileManager<ApplicationConfig>, WebDomainConfig>> controller;

    @BeforeEach
    void setup() {
        var domain1 = mock(WebDomainConfig.class);
        when(domain1.getDomain()).thenReturn("domain1");
        when(domain1.getDomainName()).thenReturn("domain1Name");
        when(domain1.isDefined()).thenReturn(true);
        when(domain1.getChannels()).thenReturn(Set.of(ValidatorChannel.REST_API));
        when(domain1.getType()).thenReturn(List.of("type1", "type2"));
        when(domain1.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        when(domain1.getCompleteTypeOptionLabel(anyString(), any(LocalisationHelper.class))).thenAnswer((x) -> "Description of " + x.getArgument(0));

        var domain2 = mock(WebDomainConfig.class);
        when(domain2.getDomain()).thenReturn("domain2");
        when(domain2.isDefined()).thenReturn(true);
        when(domain2.getChannels()).thenReturn(Set.of(ValidatorChannel.REST_API));
        when(domain2.getDomainName()).thenReturn("domain2Name");
        when(domain2.getType()).thenReturn(List.of("type1", "type2"));
        when(domain2.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        when(domain2.getCompleteTypeOptionLabel(anyString(), any(LocalisationHelper.class))).thenAnswer((x) -> "Description of " + x.getArgument(0));

        var domain3 = mock(WebDomainConfig.class);
        when(domain3.getDomain()).thenReturn("domain3");
        when(domain3.isDefined()).thenReturn(true);
        when(domain3.getChannels()).thenReturn(Set.of(ValidatorChannel.SOAP_API));
        when(domain3.getDomainName()).thenReturn("domain3Name");
        when(domain3.getType()).thenReturn(List.of("type1", "type2"));
        when(domain3.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        when(domain3.getCompleteTypeOptionLabel(anyString(), any(LocalisationHelper.class))).thenAnswer((x) -> "Description of " + x.getArgument(0));

        controller = new BaseRestController<>() {};
        controller.domainConfigs = mock(DomainConfigCache.class);
        when(controller.domainConfigs.getAllDomainConfigurations()).thenReturn(List.of(domain1, domain2, domain3));
        when(controller.domainConfigs.getConfigForDomainName("domain1Name")).thenReturn(domain1);
        when(controller.domainConfigs.getConfigForDomainName("domain2Name")).thenReturn(domain2);
        when(controller.domainConfigs.getConfigForDomainName("domain3Name")).thenReturn(domain3);
    }

    @Test
    void testInfoAll() {
        var result = controller.infoAll();
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("domain1Name", result[0].getDomain());
        assertNotNull(result[0].getValidationTypes());
        assertEquals(2, result[0].getValidationTypes().size());
        assertEquals("type1", result[0].getValidationTypes().get(0).getType());
        assertEquals("Description of type1", result[0].getValidationTypes().get(0).getDescription());
        assertEquals("type2", result[0].getValidationTypes().get(1).getType());
        assertEquals("Description of type2", result[0].getValidationTypes().get(1).getDescription());

        assertEquals("domain2Name", result[1].getDomain());
        assertNotNull(result[1].getValidationTypes());
        assertEquals(2, result[1].getValidationTypes().size());
        assertEquals("type1", result[1].getValidationTypes().get(0).getType());
        assertEquals("Description of type1", result[1].getValidationTypes().get(0).getDescription());
        assertEquals("type2", result[1].getValidationTypes().get(1).getType());
        assertEquals("Description of type2", result[1].getValidationTypes().get(1).getDescription());
    }

    @Test
    void testInfo() {
        var result = controller.info("domain1Name");
        assertEquals("domain1Name", result.getDomain());
        assertNotNull(result.getValidationTypes());
        assertEquals(2, result.getValidationTypes().size());
        assertEquals("type1", result.getValidationTypes().get(0).getType());
        assertEquals("Description of type1", result.getValidationTypes().get(0).getDescription());
        assertEquals("type2", result.getValidationTypes().get(1).getType());
        assertEquals("Description of type2", result.getValidationTypes().get(1).getDescription());
    }

    @Test
    void testNotFound() {
        assertThrows(NotFoundException.class, () -> controller.info("domainXName"));
        assertThrows(NotFoundException.class, () -> controller.info("domain3Name"));
    }

    @Test
    void testGetExternalSchemas() {
        controller.inputHelper = mock(BaseInputHelper.class);
        when(controller.inputHelper.validateExternalArtifacts(any(WebDomainConfig.class), anyList(), anyString(), anyString(), any(File.class)))
                .thenReturn(Collections.emptyList());
        var schemaInfo1 = mock(SchemaInfo.class);
        when(schemaInfo1.toFileContent()).thenReturn(new FileContent());
        var domainConfig = mock(WebDomainConfig.class);
        var file = mock(File.class);
        var result = controller.getExternalSchemas(domainConfig, List.of(schemaInfo1), "type1", "artifactType1", file);
        verify(controller.inputHelper, times(1)).validateExternalArtifacts(any(WebDomainConfig.class), anyList(), eq("type1"), eq("artifactType1"), eq(file));
        verify(schemaInfo1, times(1)).toFileContent();
    }

    @Test
    void testGetAcceptHeader() {
        // Found supported value.
        var request = mock(HttpServletRequest.class);
        when(request.getHeaders(anyString())).thenReturn(Collections.enumeration(List.of("application/json")));
        assertEquals("application/json", controller.getAcceptHeader(request, "application/xml"));
        // Found not-supported value.
        request = mock(HttpServletRequest.class);
        when(request.getHeaders(anyString())).thenReturn(Collections.enumeration(List.of("text/xml")));
        assertEquals("application/xml", controller.getAcceptHeader(request, "application/xml"));
    }

    @Test
    void testWriteReportAsJson() throws IOException {
        var report = new TAR();
        var config = mock(WebDomainConfig.class);
        when(config.getMaximumReportsForXmlOutput()).thenReturn(5L);
        report.setName("name");
        controller.tarObjectMapper = new JsonConfig().objectMapper();
        try (var out = new ByteArrayOutputStream()) {
            controller.writeReportAsJson(out, report, config);
            var reportString = out.toString();
            assertTrue(reportString.indexOf("name") > 0);
        }

        var badStream = mock(OutputStream.class);
        doThrow(IOException.class).when(badStream).write(anyInt());
        doThrow(IOException.class).when(badStream).write(any(byte[].class));
        doThrow(IOException.class).when(badStream).write(any(byte[].class), anyInt(), anyInt());

        assertThrows(IllegalStateException.class, () -> controller.writeReportAsJson(badStream, report, config));
    }
}
