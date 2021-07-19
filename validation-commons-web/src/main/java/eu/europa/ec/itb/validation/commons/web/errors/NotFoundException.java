package eu.europa.ec.itb.validation.commons.web.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown in case a given resource is not found.
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "The requested resource could not be found")
public class NotFoundException extends RuntimeException {

    private final String requestedDomain;

    /**
     * Constructor with no domain information.
     */
    public NotFoundException() {
        this(null);
    }

    /**
     * Constructor for a given invalid domain that was requested.
     *
     * @param requestedDomain The requested domain.
     */
    public NotFoundException(String requestedDomain) {
        this.requestedDomain = requestedDomain;
    }

    /**
     * The domain that was requested (and resulted in a not found error).
     *
     * @return The domain name.
     */
    public String getRequestedDomain() {
        return requestedDomain;
    }

}
