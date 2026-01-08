package eu.europa.ec.itb.validation.commons.war.ratelimit;

import eu.europa.ec.itb.validation.commons.RateLimitPolicy;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import javax.xml.namespace.QName;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitSoapInterceptorTest {

    @Test
    void testProceedForOtherOperations() {
        var rateLimiter = mock(RateLimiterService.class);
        var keyGenerator = mock(RateLimitKeyGenerator.class);
        var message = mock(SoapMessage.class);
        var exchange = mock(Exchange.class);
        var bindingOperationInfo = mock(BindingOperationInfo.class);
        var operationInfo = mock(OperationInfo.class);
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getBindingOperationInfo()).thenReturn(bindingOperationInfo);
        when(bindingOperationInfo.getOperationInfo()).thenReturn(operationInfo);
        when(operationInfo.getName()).thenReturn(new QName("other"));
        var interceptor = new RateLimitSoapInterceptor(rateLimiter, keyGenerator);
        assertDoesNotThrow(() -> interceptor.handleMessage(message));
    }

    @Test
    void testProceed() {
        var rateLimiter = mock(RateLimiterService.class);
        when(rateLimiter.tryConsume(anyString(), any(RateLimitPolicy.class))).then((Answer<RateLimitCheckResult>) invocation -> new RateLimitCheckResult(true, null));
        var keyGenerator = mock(RateLimitKeyGenerator.class);
        when(keyGenerator.generate(any(), any())).thenReturn("key");
        var message = mock(SoapMessage.class);
        var exchange = mock(Exchange.class);
        var bindingOperationInfo = mock(BindingOperationInfo.class);
        var operationInfo = mock(OperationInfo.class);
        var request = mock(HttpServletRequest.class);
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getBindingOperationInfo()).thenReturn(bindingOperationInfo);
        when(bindingOperationInfo.getOperationInfo()).thenReturn(operationInfo);
        when(operationInfo.getName()).thenReturn(new QName("validate"));
        when(message.get(AbstractHTTPDestination.HTTP_REQUEST)).thenReturn(request);
        var interceptor = new RateLimitSoapInterceptor(rateLimiter, keyGenerator);
        assertDoesNotThrow(() -> interceptor.handleMessage(message));
    }

    @Test
    void testBlock() {
        var rateLimiter = mock(RateLimiterService.class);
        when(rateLimiter.tryConsume(anyString(), any(RateLimitPolicy.class))).then((Answer<RateLimitCheckResult>) invocation -> new RateLimitCheckResult(false, 10L));
        var keyGenerator = mock(RateLimitKeyGenerator.class);
        when(keyGenerator.generate(any(), any())).thenReturn("key");
        var message = mock(SoapMessage.class);
        var exchange = mock(Exchange.class);
        var bindingOperationInfo = mock(BindingOperationInfo.class);
        var operationInfo = mock(OperationInfo.class);
        var request = mock(HttpServletRequest.class);
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getBindingOperationInfo()).thenReturn(bindingOperationInfo);
        when(bindingOperationInfo.getOperationInfo()).thenReturn(operationInfo);
        when(operationInfo.getName()).thenReturn(new QName("validate"));
        when(message.get(AbstractHTTPDestination.HTTP_REQUEST)).thenReturn(request);
        var interceptor = new RateLimitSoapInterceptor(rateLimiter, keyGenerator);
        assertThrows(Fault.class, () -> interceptor.handleMessage(message));
    }

    @Test
    void testAllowNonHttpRequest() {
        var rateLimiter = mock(RateLimiterService.class);
        when(rateLimiter.tryConsume(anyString(), any(RateLimitPolicy.class))).then((Answer<RateLimitCheckResult>) invocation -> new RateLimitCheckResult(false, 10L));
        var keyGenerator = mock(RateLimitKeyGenerator.class);
        when(keyGenerator.generate(any(), any())).thenReturn("key");
        var message = mock(SoapMessage.class);
        var exchange = mock(Exchange.class);
        var bindingOperationInfo = mock(BindingOperationInfo.class);
        var operationInfo = mock(OperationInfo.class);
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getBindingOperationInfo()).thenReturn(bindingOperationInfo);
        when(bindingOperationInfo.getOperationInfo()).thenReturn(operationInfo);
        when(operationInfo.getName()).thenReturn(new QName("validate"));
        when(message.get(AbstractHTTPDestination.HTTP_REQUEST)).thenReturn(null);
        var interceptor = new RateLimitSoapInterceptor(rateLimiter, keyGenerator);
        assertDoesNotThrow(() -> interceptor.handleMessage(message));
    }

}
