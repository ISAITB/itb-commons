package eu.europa.ec.itb.validation.commons.web.filters;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Check whether a given request matches certain path and method requirements.
 */
public class RequestMatcher {

    private final String pathEnding;
    private final Optional<String> method;

    /**
     * Constructor to match a path ending and any HTTP method.
     *
     * @param pathEnding The ending of the path.
     */
    public RequestMatcher(String pathEnding) {
        this.pathEnding = pathEnding;
        this.method = Optional.empty();
    }

    /**
     * Constructor to match a path ending and a specific HTTP method.
     *
     * @param pathEnding The ending of the path.
     * @param method The HTTP method.
     */
    public RequestMatcher(String pathEnding, String method) {
        this.pathEnding = pathEnding;
        this.method = Optional.of(method);
    }

    /**
     * Check whether the provided request matches this matcher's criteria.
     *
     * @param request The request to check.
     * @return The check result.
     */
    public boolean matches(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        return method.map((m) -> m.equalsIgnoreCase(request.getMethod())).orElse(true) && requestPath.endsWith(pathEnding);
    }

}
