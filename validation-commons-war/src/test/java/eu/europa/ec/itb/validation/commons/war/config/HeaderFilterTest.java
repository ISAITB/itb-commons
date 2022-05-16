package eu.europa.ec.itb.validation.commons.war.config;

import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

class HeaderFilterTest {

    private HeaderFilter<?,?> getFilter() throws NoSuchFieldException, IllegalAccessException {
        var domain1 = mock(WebDomainConfig.class);
        when(domain1.isSupportUserInterfaceEmbedding()).thenReturn(true);
        var domain2 = mock(WebDomainConfig.class);
        when(domain2.isSupportUserInterfaceEmbedding()).thenReturn(false);
        var domainConfigCache = mock(WebDomainConfigCache.class);
        when(domainConfigCache.getConfigForDomainName("domain1")).thenReturn(domain1);
        when(domainConfigCache.getConfigForDomainName("domain2")).thenReturn(domain2);
        var filter = new HeaderFilter<>();
        var field = HeaderFilter.class.getDeclaredField("domainConfigCache");
        field.setAccessible(true);
        field.set(filter, domainConfigCache);
        return filter;
    }

    @Test
    void testAllowEmbedding() throws NoSuchFieldException, IllegalAccessException, ServletException, IOException {
        checkEmbedding("/domain1/upload", HttpMethod.GET,true);
        checkEmbedding("/domain1/upload", HttpMethod.POST,true);
        checkEmbedding("/ctx/domain1/upload", HttpMethod.GET,true);
        checkEmbedding("/ctx/domain1/upload", HttpMethod.POST,true);
        checkEmbedding("/domain1/uploadm", HttpMethod.GET,true);
        checkEmbedding("/domain1/uploadm", HttpMethod.POST,true);
        checkEmbedding("/ctx/domain1/uploadm", HttpMethod.GET,true);
        checkEmbedding("/ctx/domain1/uploadm", HttpMethod.POST,true);
    }

    @Test
    void testBlockEmbedding() throws NoSuchFieldException, IllegalAccessException, ServletException, IOException {
        checkEmbedding("/domain2/upload", HttpMethod.GET,false);
        checkEmbedding("/domain2/upload", HttpMethod.POST,false);
        checkEmbedding("/ctx/domain2/upload", HttpMethod.GET,false);
        checkEmbedding("/ctx/domain2/upload", HttpMethod.POST,false);
        checkEmbedding("/domain2/uploadm", HttpMethod.GET, false);
        checkEmbedding("/domain2/uploadm", HttpMethod.POST, false);
        checkEmbedding("/ctx/domain2/uploadm", HttpMethod.GET, false);
        checkEmbedding("/ctx/domain2/uploadm", HttpMethod.POST, false);
    }

    void checkEmbedding(String path, HttpMethod method, boolean allowed) throws NoSuchFieldException, IllegalAccessException, ServletException, IOException {
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getServletPath()).thenReturn(path);
        when(request.getMethod()).thenReturn(method.name());
        var filter = getFilter();
        filter.doFilter(request, response, chain);
        verify(response, times(allowed?0:1)).setHeader("X-Frame-Options", "DENY");
    }

}
