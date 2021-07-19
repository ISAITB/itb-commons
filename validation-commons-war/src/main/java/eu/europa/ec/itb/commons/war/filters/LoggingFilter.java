package eu.europa.ec.itb.commons.war.filters;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import java.io.IOException;

/**
 * Filter to clear up the logging ThreadLocal values per request.
 */
@Component
public class LoggingFilter implements Filter {

    /**
     * Clear the logging ThreadLocals once web request processing is complete.
     *
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(request, response);
        MDC.clear();
    }
}
