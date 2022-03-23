package eu.europa.ec.itb.validation.commons.web.errors;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.itb.validation.commons.web.Constants;
import org.junit.jupiter.api.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebErrorControllerTest {

    @Test
    void testHandleErrorNormalRequest() {
        var request = mock(HttpServletRequest.class);
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(500);
        when(request.getAttribute(Constants.IS_MINIMAL)).thenReturn(false);
        when(request.getHeader(Constants.AJAX_REQUEST_HEADER)).thenReturn(null);
        when(request.getHeader("referer")).thenReturn(null);
        var response = mock(HttpServletResponse.class);
        var controller = new WebErrorController();
        var result = controller.handleError(request, response);
        assertNotNull(result);
        assertNotNull(result.getModelMap().getAttribute("minimalUI"));
        assertNotNull(result.getModelMap().getAttribute("previousPage"));
        assertNotNull(result.getModelMap().getAttribute("errorMessage"));
        assertEquals(Boolean.FALSE, result.getModelMap().getAttribute("minimalUI"));
        assertEquals(Boolean.FALSE, result.getModelMap().getAttribute("previousPage"));
    }

    @Test
    void testHandleErrorAjaxRequest() throws IOException {
        var request = mock(HttpServletRequest.class);
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(500);
        when(request.getAttribute(Constants.IS_MINIMAL)).thenReturn(false);
        when(request.getHeader(Constants.AJAX_REQUEST_HEADER)).thenReturn("XmlHttpRequest");
        when(request.getHeader("referer")).thenReturn(null);
        var response = mock(HttpServletResponse.class);
        var bos = new ByteArrayOutputStream();
        var out = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                bos.write(b);
            }
            @Override
            public boolean isReady() {
                return true;
            }
            @Override
            public void setWriteListener(WriteListener listener) {
            }
        };
        when(response.getOutputStream()).thenReturn(out);
        var controller = new WebErrorController();
        var result = controller.handleError(request, response);
        assertNull(result);
        ObjectMapper mapper = new ObjectMapper();
        var node = mapper.readTree(new StringReader(bos.toString(StandardCharsets.UTF_8)));
        assertNotNull(node.get("errorMessage").asText());
    }

}
