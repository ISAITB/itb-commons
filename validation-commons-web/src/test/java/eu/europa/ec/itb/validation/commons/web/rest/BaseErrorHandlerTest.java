package eu.europa.ec.itb.validation.commons.web.rest;

import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseErrorHandlerTest {

    @Test
    void testHandleNotFound() {
        var exception = mock(NotFoundException.class);
        when(exception.getRequestedDomain()).thenReturn("domain1");
        var result = new BaseErrorHandler().handleNotFound(exception, mock(WebRequest.class));
        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertInstanceOf(ErrorInfo.class, result.getBody());
        assertEquals("The requested resource could not be found", ((ErrorInfo) result.getBody()).getMessage());
    }

    @Test
    void testHandleValidatorException() {
        var exception = mock(ValidatorException.class);
        when(exception.getMessageForDisplay(any())).thenReturn("Message");
        var result = new BaseErrorHandler().handleValidatorException(exception, mock(WebRequest.class));
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertInstanceOf(ErrorInfo.class, result.getBody());
        assertEquals("Message", ((ErrorInfo) result.getBody()).getMessage());
    }

    @Test
    void testHandleValidatorExceptionWithInputReference() {
        var exception = new ValidatorException("validator.label.exception.base64").withInputReference("contentToValidate[1]");
        var result = new BaseErrorHandler().handleValidatorException(exception, mock(WebRequest.class));
        var body = assertInstanceOf(ErrorInfo.class, result.getBody());
        assertEquals("contentToValidate[1]", body.getResource());
        assertEquals("Error in input [contentToValidate[1]]: Error when processing the provided Base64 data.", body.getMessage());
    }

    @Test
    void testErrorMessageLocalisedWithRecordedLocaliser() {
        var servletRequest = new MockServletRequestHolder();
        try {
            BaseErrorHandler.recordLocaliser(new LocalisationHelper(Locale.FRENCH));
            var exception = new ValidatorException("validator.label.exception.base64").withInputReference("contentToValidate[0]");
            var result = new BaseErrorHandler().handleValidatorException(exception, servletRequest.webRequest);
            var body = assertInstanceOf(ErrorInfo.class, result.getBody());
            assertTrue(body.getMessage().startsWith("Erreur dans l'entrée [contentToValidate[0]] : "), body.getMessage());
            assertEquals("contentToValidate[0]", body.getResource());
        } finally {
            servletRequest.close();
        }
    }

    @Test
    void testRecordLocaliserTakesPrecedenceOverAcceptLanguage() {
        var servletRequest = new MockServletRequestHolder();
        try {
            servletRequest.request.addHeader("Accept-Language", "de");
            BaseErrorHandler.recordLocaliser(new LocalisationHelper(Locale.FRENCH));
            var result = new BaseErrorHandler().handleValidatorException(new ValidatorException("validator.label.exception.base64"), servletRequest.webRequest);
            assertEquals("Erreur lors du traitement des données Base64 fournies.", ((ErrorInfo) result.getBody()).getMessage());
        } finally {
            servletRequest.close();
        }
    }

    @Test
    void testErrorMessageLocalisedWithAcceptLanguage() {
        var servletRequest = new MockServletRequestHolder();
        try {
            servletRequest.request.addHeader("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8");
            var result = new BaseErrorHandler().handleValidatorException(new ValidatorException("validator.label.exception.base64"), servletRequest.webRequest);
            assertEquals("Erreur lors du traitement des données Base64 fournies.", ((ErrorInfo) result.getBody()).getMessage());
        } finally {
            servletRequest.close();
        }
    }

    @Test
    void testErrorMessageEnglishWhenNoOrInvalidLocale() {
        for (var header: new String[] {null, "", "*", "!!invalid!!"}) {
            var servletRequest = new MockServletRequestHolder();
            try {
                if (header != null) servletRequest.request.addHeader("Accept-Language", header);
                var result = new BaseErrorHandler().handleValidatorException(new ValidatorException("validator.label.exception.base64"), servletRequest.webRequest);
                assertEquals("Error when processing the provided Base64 data.", ((ErrorInfo) result.getBody()).getMessage(), "Header: " + header);
            } finally {
                servletRequest.close();
            }
        }
    }

    @Test
    void testRecordLocaliserWithoutRequestDoesNotFail() {
        RequestContextHolder.resetRequestAttributes();
        assertDoesNotThrow(() -> BaseErrorHandler.recordLocaliser(new LocalisationHelper(Locale.FRENCH)));
    }

    /**
     * Helper to bind a mock request to the current thread, as done for real requests.
     */
    private static class MockServletRequestHolder {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final ServletWebRequest webRequest = new ServletWebRequest(request);

        MockServletRequestHolder() {
            RequestContextHolder.setRequestAttributes(webRequest);
        }

        void close() {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void testErrorInfoResourceOmittedFromJsonWhenUnknown() throws Exception {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var withoutResource = mapper.writeValueAsString(new ErrorInfo("Message"));
        assertFalse(withoutResource.contains("resource"));
        var withResource = mapper.writeValueAsString(new ErrorInfo("Message", "externalRules[0]"));
        assertTrue(withResource.contains("\"resource\":\"externalRules[0]\""));
    }

    @Test
    void testHandleUnexpectedErrors() {
        var exception = mock(Exception.class);
        var result = new BaseErrorHandler().handleUnexpectedErrors(exception, mock(WebRequest.class));
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertInstanceOf(ErrorInfo.class, result.getBody());
        assertEquals("An unexpected error occurred during validation", ((ErrorInfo) result.getBody()).getMessage());
    }

    @Test
    void testErrorTimestamps() {
        var errorInfo = new ErrorInfo("Message");
        assertNotNull(errorInfo.getTimestamp());
    }
}
