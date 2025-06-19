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

package eu.europa.ec.itb.validation.commons.web.rest;

import java.time.LocalDateTime;

/**
 * DTO to wrap an error message's information.
 */
public class ErrorInfo {

    private final String message;
    private final LocalDateTime timestamp;

    /**
     * Constructor.
     *
     * @param message The message to wrap.
     */
    public ErrorInfo(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * @return The message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return The timestamp this object was created.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
