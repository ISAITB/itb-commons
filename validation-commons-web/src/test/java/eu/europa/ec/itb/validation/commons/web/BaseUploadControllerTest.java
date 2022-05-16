package eu.europa.ec.itb.validation.commons.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.web.dto.UploadResult;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseUploadControllerTest {

    BaseUploadController<?, ?> controller;

    @BeforeEach
    void setup() {
        controller = new BaseUploadController<>() {};
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

    @Test
    void testGetDomainConfig() {
        var request = mock(HttpServletRequest.class);
        when(request.getAttribute(anyString())).thenReturn(mock(WebDomainConfig.class));
        assertNotNull(controller.getDomainConfig(request));
    }

    @Test
    void testGetDomainConfigNotExisting() {
        var request = mock(HttpServletRequest.class);
        when(request.getAttribute(anyString())).thenReturn(null);
        assertThrows(NotFoundException.class, () -> controller.getDomainConfig(request));
    }

}
