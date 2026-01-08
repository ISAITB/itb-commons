package eu.europa.ec.itb.validation.commons.war.ratelimit;

import eu.europa.ec.itb.validation.commons.RateLimitPolicy;
import eu.europa.ec.itb.validation.commons.RateLimited;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RateLimitInterceptorTest {

    @Test
    void testHandleForNonHandlers() throws Exception {
        var rateLimiter = mock(RateLimiterService.class);
        var keyGenerator = mock(RateLimitKeyGenerator.class);
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        var interceptor = new RateLimitInterceptor(rateLimiter, keyGenerator);
        var result1 = interceptor.preHandle(request, response, mock(HandlerMethod.class));
        assertTrue(result1);
        var result2 = interceptor.preHandle(request, response, new Object());
        assertTrue(result2);
    }

    @Test
    void testProceed() throws Exception {
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        var rateLimiter = mock(RateLimiterService.class);
        when(rateLimiter.tryConsume(anyString(), any(RateLimitPolicy.class))).then((Answer<RateLimitCheckResult>) invocation -> new RateLimitCheckResult(true, null));
        var keyGenerator = mock(RateLimitKeyGenerator.class);
        when(keyGenerator.generate(any(), any())).thenReturn("key");
        var handler = mock(HandlerMethod.class);
        var annotation = mock(RateLimited.class);
        when(annotation.policy()).thenReturn(RateLimitPolicy.UI_VALIDATE);
        when(handler.getMethodAnnotation(RateLimited.class)).thenReturn(annotation);
        var interceptor = new RateLimitInterceptor(rateLimiter, keyGenerator);
        var result = interceptor.preHandle(request, response, handler);
        assertTrue(result);
    }

    @Test
    void testBlock() throws Exception {
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));
        var rateLimiter = mock(RateLimiterService.class);
        when(rateLimiter.tryConsume(anyString(), any(RateLimitPolicy.class))).then((Answer<RateLimitCheckResult>) invocation -> new RateLimitCheckResult(false, 10L));
        var keyGenerator = mock(RateLimitKeyGenerator.class);
        when(keyGenerator.generate(any(), any())).thenReturn("key");
        var handler = mock(HandlerMethod.class);
        var annotation = mock(RateLimited.class);
        when(annotation.policy()).thenReturn(RateLimitPolicy.UI_VALIDATE);
        when(handler.getMethodAnnotation(RateLimited.class)).thenReturn(annotation);
        var interceptor = new RateLimitInterceptor(rateLimiter, keyGenerator);
        var result = interceptor.preHandle(request, response, handler);
        assertFalse(result);
        verify(response, times(1)).setStatus(429);
        verify(response, times(1)).setHeader("Retry-After", "10");
    }

    @Test
    void testSingleCallPerRequest() throws Exception {
        var attributes = new HashMap<String, Object>();
        var rateLimiter = mock(RateLimiterService.class);
        var keyGenerator = mock(RateLimitKeyGenerator.class);
        var request = mock(HttpServletRequest.class);
        when(request.getAttribute(anyString())).thenAnswer((Answer<Boolean>) invocation -> (Boolean) attributes.get((String) invocation.getArgument(0)));
        doAnswer((Answer<?>)invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1))).when(request).setAttribute(anyString(), any());
        var response = mock(HttpServletResponse.class);
        var interceptor = new RateLimitInterceptor(rateLimiter, keyGenerator);
        var result1 = interceptor.preHandle(request, response, mock(HandlerMethod.class));
        assertTrue(result1);
        var result2 = interceptor.preHandle(request, response, mock(HandlerMethod.class));
        assertTrue(result2);
        verify(request, times(1)).setAttribute("RATE_LIMIT_HANDLED", Boolean.TRUE);
    }

}
