package eu.europa.ec.itb.validation.commons.war.config;

import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Filter to apply custom settings to HTTP headers.
 */
@Component
public class HeaderFilter<Y extends WebDomainConfig, X extends WebDomainConfigCache<Y>> extends GenericFilterBean {

    private final List<RequestMatcher> configurablyAllowed = List.of(
            new AntPathRequestMatcher("/**/upload", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/**/upload", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/**/uploadm", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/**/uploadm", HttpMethod.POST.name())
    );
    @Autowired
    private X domainConfigCache;

    /**
     * Applies custom headers to responses.
     *
     * {{@inheritDoc}}
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            var deny = true;
            if (matches(configurablyAllowed, (HttpServletRequest) request)) {
                var pathParts = StringUtils.split(((HttpServletRequest) request).getRequestURI(), '/');
                // We want the part before last (this is the domain)
                var index = pathParts.length - 2;
                if (index >= 0) {
                    var domainConfig = domainConfigCache.getConfigForDomainName(pathParts[index]);
                    if (domainConfig != null && domainConfig.isSupportUserInterfaceEmbedding()) {
                        deny = false;
                    }
                }
            }
            if (deny) {
                denyEmbedding((HttpServletResponse) response);
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Apply the necessary header to prevent embedding in iFrames.
     *
     * @param response The HTTP response.
     */
    private void denyEmbedding(HttpServletResponse response) {
        response.setHeader("X-Frame-Options", "DENY");
    }

    /**
     * Check to see if the provided request matches the defined matchers.
     *
     * @param matchers The matchers to check.
     * @param request The request.
     * @return The check result.
     */
    private boolean matches(Collection<RequestMatcher> matchers, HttpServletRequest request) {
        return matchers.stream().anyMatch(matcher -> matcher.matches(request));
    }
}
