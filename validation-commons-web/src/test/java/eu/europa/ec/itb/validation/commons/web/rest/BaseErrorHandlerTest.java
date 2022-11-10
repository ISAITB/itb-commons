package eu.europa.ec.itb.validation.commons.web.rest;

import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

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
        assertEquals(result.getStatusCode(), HttpStatus.NOT_FOUND);
        assertTrue(result.getBody() instanceof ErrorInfo);
        assertEquals("The requested resource could not be found", ((ErrorInfo) result.getBody()).getMessage());
    }

    @Test
    void testHandleValidatorException() {
        var exception = mock(ValidatorException.class);
        when(exception.getMessageForDisplay(any())).thenReturn("Message");
        var result = new BaseErrorHandler().handleValidatorException(exception, mock(WebRequest.class));
        assertNotNull(result);
        assertEquals(result.getStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
        assertTrue(result.getBody() instanceof ErrorInfo);
        assertEquals("Message", ((ErrorInfo) result.getBody()).getMessage());
    }

    @Test
    void testHandleUnexpectedErrors() {
        var exception = mock(Exception.class);
        var result = new BaseErrorHandler().handleUnexpectedErrors(exception, mock(WebRequest.class));
        assertNotNull(result);
        assertEquals(result.getStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
        assertTrue(result.getBody() instanceof ErrorInfo);
        assertEquals("An unexpected error occurred during validation", ((ErrorInfo) result.getBody()).getMessage());
    }

    @Test
    void testErrorTimestamps() {
        var errorInfo = new ErrorInfo("Message");
        assertNotNull(errorInfo.getTimestamp());
    }
}
