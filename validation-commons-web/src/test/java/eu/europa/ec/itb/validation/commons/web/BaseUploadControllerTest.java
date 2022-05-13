package eu.europa.ec.itb.validation.commons.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.web.dto.UploadResult;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseUploadControllerTest {

    BaseUploadController<?, ?> controller;

    @BeforeEach
    void setup() throws NoSuchFieldException, IllegalAccessException {
        var domainConfigCache = mock(DomainConfigCache.class);
        when(domainConfigCache.getConfigForDomainName("domainWithoutForm")).thenAnswer(call -> {
           var config = mock(WebDomainConfig.class);
           when(config.isDefined()).thenReturn(true);
           when(config.getChannels()).thenReturn(Set.of(ValidatorChannel.SOAP_API));
           when(config.isSupportMinimalUserInterface()).thenReturn(true);
           return config;
        });
        when(domainConfigCache.getConfigForDomainName("domainWithForm")).thenAnswer(call -> {
            var config = mock(WebDomainConfig.class);
            when(config.isDefined()).thenReturn(true);
            when(config.getChannels()).thenReturn(Set.of(ValidatorChannel.FORM));
            when(config.isSupportMinimalUserInterface()).thenReturn(true);
            return config;
        });
        when(domainConfigCache.getConfigForDomainName("domainNotDefined")).thenAnswer(call -> {
            var config = mock(WebDomainConfig.class);
            when(config.isDefined()).thenReturn(false);
            return config;
        });
        when(domainConfigCache.getConfigForDomainName("domainWithoutMinimalUI")).thenAnswer(call -> {
            var config = mock(WebDomainConfig.class);
            when(config.isDefined()).thenReturn(true);
            when(config.getChannels()).thenReturn(Set.of(ValidatorChannel.FORM));
            when(config.isSupportMinimalUserInterface()).thenReturn(false);
            return config;
        });
        when(domainConfigCache.getConfigForDomain("nullDomain")).thenReturn(null);
        controller = new BaseUploadController<>() {};
        var domainConfigsField = BaseUploadController.class.getDeclaredField("domainConfigs");
        domainConfigsField.setAccessible(true);
        domainConfigsField.set(controller, domainConfigCache);
    }

    @Test
    void testValidateDomainNoForm() {
        var mockRequest = mock(HttpServletRequest.class);
        assertThrows(NotFoundException.class, () -> controller.validateDomain(mockRequest, "domainWithoutForm"));
    }

    @Test
    void testValidateDomainNull() {
        var mockRequest = mock(HttpServletRequest.class);
        assertThrows(NotFoundException.class, () -> controller.validateDomain(mockRequest, "nullDomain"));
    }

    @Test
    void testValidateDomainNotDefined() {
        var mockRequest = mock(HttpServletRequest.class);
        assertThrows(NotFoundException.class, () -> controller.validateDomain(mockRequest, "domainNotDefined"));
    }

    @Test
    void testValidateDomainForMinimalUIOK() {
        var mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getAttribute(Constants.IS_MINIMAL)).thenReturn(false);
        var result = controller.validateDomain(mockRequest, "domainWithForm");
        assertNotNull(result);
    }

    @Test
    void testValidateDomainForMinimalUINOK() {
        var mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getAttribute(Constants.IS_MINIMAL)).thenReturn(true);
        assertThrows(NotFoundException.class, () -> controller.validateDomain(mockRequest, "domainWithoutMinimalUI"));
    }

    @Test
    void testUnexpectedError() {
        assertThrows(NotFoundException.class, () -> controller.validateDomain(null, "domainWithoutMinimalUI"));
    }

    @Test
    void testSetMinimalUINotPreviouslySet() {
        var mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getAttribute(Constants.IS_MINIMAL)).thenReturn(null);
        controller.setMinimalUIFlag(mockRequest, true);
        verify(mockRequest, times(1)).setAttribute(Constants.IS_MINIMAL, true);
    }

    @Test
    void testSetMinimalUIPreviouslySet() {
        var mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getAttribute(Constants.IS_MINIMAL)).thenReturn(Boolean.TRUE);
        controller.setMinimalUIFlag(mockRequest, true);
        verify(mockRequest, times(0)).setAttribute(eq(Constants.IS_MINIMAL), anyBoolean());
    }

    @Test
    void testWriteResultToString() {
        var uploadResult = new UploadResult<>();
        uploadResult.setMessage("Message");
        var result = controller.writeResultToString(uploadResult);
        assertNotNull(result);
        var mapper = new ObjectMapper();
        UploadResult<?> readResult = null;
        try {
            readResult = mapper.readValue(result, UploadResult.class);
        } catch (JsonProcessingException e) {
            fail("Invalid JSON returned");
        }
        assertNotNull(readResult);
        assertEquals(uploadResult.getMessage(), readResult.getMessage());
    }

    @Test
    void testWriteResultToStringError() throws NoSuchFieldException, IOException, IllegalAccessException {
        var badObjectMapper = mock(ObjectMapper.class);
        doThrow(IOException.class).when(badObjectMapper).writeValue(any(Writer.class), any(Object.class));
        var mapper = BaseUploadController.class.getDeclaredField("objectMapper");
        mapper.setAccessible(true);
        mapper.set(controller, badObjectMapper);
        assertThrows(IllegalStateException.class, () -> controller.writeResultToString(new UploadResult<>()));
    }

}
