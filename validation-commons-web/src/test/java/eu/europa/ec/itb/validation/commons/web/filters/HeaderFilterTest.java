package eu.europa.ec.itb.validation.commons.web.filters;

import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

class HeaderFilterTest {

    private HeaderFilter<?,?> getFilter() {
        return new HeaderFilter<>();
    }

    @Test
    void testAllowEmbedding() throws ServletException, IOException {
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
    void testBlockEmbedding() throws ServletException, IOException {
        checkEmbedding("/domain2/upload", HttpMethod.GET,false);
        checkEmbedding("/domain2/upload", HttpMethod.POST,false);
        checkEmbedding("/ctx/domain2/upload", HttpMethod.GET,false);
        checkEmbedding("/ctx/domain2/upload", HttpMethod.POST,false);
        checkEmbedding("/domain2/uploadm", HttpMethod.GET, false);
        checkEmbedding("/domain2/uploadm", HttpMethod.POST, false);
        checkEmbedding("/ctx/domain2/uploadm", HttpMethod.GET, false);
        checkEmbedding("/ctx/domain2/uploadm", HttpMethod.POST, false);
    }

    void checkEmbedding(String path, HttpMethod method, boolean allowed) throws ServletException, IOException {
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getServletPath()).thenReturn(path);
        when(request.getMethod()).thenReturn(method.name());
        when(request.getAttribute(WebDomainConfig.DOMAIN_CONFIG_REQUEST_ATTRIBUTE)).thenAnswer(call -> {
            var domainConfig = mock(WebDomainConfig.class);
            when(domainConfig.isSupportUserInterfaceEmbedding()).thenReturn(allowed);
            return domainConfig;
        });
        var filter = getFilter();
        filter.doFilter(request, response, chain);
        verify(response, times(allowed?0:1)).setHeader("X-Frame-Options", "DENY");
    }

}
