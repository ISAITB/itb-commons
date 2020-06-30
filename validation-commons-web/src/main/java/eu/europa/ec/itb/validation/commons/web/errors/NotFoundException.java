package eu.europa.ec.itb.validation.commons.web.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "The requested resource could not be found")
public class NotFoundException extends RuntimeException {

    private final String requestedDomain;

    public NotFoundException() {
        this(null);
    }

    public NotFoundException(String requestedDomain) {
        this.requestedDomain = requestedDomain;
    }

    public String getRequestedDomain() {
        return requestedDomain;
    }

}
