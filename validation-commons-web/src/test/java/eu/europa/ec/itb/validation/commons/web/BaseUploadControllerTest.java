package eu.europa.ec.itb.validation.commons.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.web.dto.UploadResult;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;

import static eu.europa.ec.itb.validation.commons.web.Constants.IS_MINIMAL;
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

    @Test
    void testCheckInputType() {
        // No input.
        assertThrows(IllegalArgumentException.class, () -> assertNull(controller.checkInputType(null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> controller.checkInputType(null, mock(MultipartFile.class), "http://uri", "STRING"));
        // Content type provided.
        assertEquals("CONTENT_TYPE", controller.checkInputType("CONTENT_TYPE", null, null, null));
        assertEquals("CONTENT_TYPE", controller.checkInputType("CONTENT_TYPE", mock(MultipartFile.class), "http://uri", "STRING"));
        // No content type provided but one input supplied.
        assertEquals(BaseUploadController.CONTENT_TYPE_FILE, controller.checkInputType(null, mock(MultipartFile.class), null, null));
        assertEquals(BaseUploadController.CONTENT_TYPE_URI, controller.checkInputType(null, null, "http://uri", null));
        assertEquals(BaseUploadController.CONTENT_TYPE_STRING, controller.checkInputType(null, null, null, "STRING"));
        // No content type with multiple inputs.
        assertThrows(IllegalArgumentException.class, () -> controller.checkInputType(null, mock(MultipartFile.class), null, "STRING"));
        assertThrows(IllegalArgumentException.class, () -> controller.checkInputType(null, mock(MultipartFile.class), "http://uri", "STRING"));
        assertThrows(IllegalArgumentException.class, () -> controller.checkInputType(null, mock(MultipartFile.class), "http://uri", null));
        assertThrows(IllegalArgumentException.class, () -> controller.checkInputType(null, null, "http://uri", "STRING"));
    }

    @Test
    void testCheckInputTypeWithAcceptedFailure() {
        assertNull(controller.checkInputType(null, null, null, null, false));
        assertEquals("", controller.checkInputType("", null, null, null, false));
        // No content type with multiple inputs.
        assertNull(controller.checkInputType(null, mock(MultipartFile.class), "http://uri", "STRING", false));
        assertNull(controller.checkInputType(null, mock(MultipartFile.class), null, "STRING", false));
        assertNull(controller.checkInputType(null, mock(MultipartFile.class), "http://uri", null, false));
        assertNull(controller.checkInputType(null, null, "http://uri", "STRING", false));
    }

    @Test
    void testInMinimalUI() {
        var request = mock(HttpServletRequest.class);
        when(request.getAttribute(IS_MINIMAL)).thenReturn(Boolean.TRUE);
        assertTrue(controller.isMinimalUI(request));
        when(request.getAttribute(IS_MINIMAL)).thenReturn(Boolean.FALSE);
        assertFalse(controller.isMinimalUI(request));
        when(request.getAttribute(IS_MINIMAL)).thenReturn(null);
        assertFalse(controller.isMinimalUI(request));
    }

}
