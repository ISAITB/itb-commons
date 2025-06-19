/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
