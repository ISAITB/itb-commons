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

package eu.europa.ec.itb.validation.commons.config;

/**
 * Enumeration holding possible response types for different events.
 */
public enum ErrorResponseTypeEnum {

    /** Fail. log the issue and stop all subsequent processing. */
    FAIL("fail"),
    /** Continue processing but present a warning to the user and log the issue. */
    WARN("warn"),
    /** Log the issue and continue processing without notifying the user. */
    LOG("log");

    private final String value;

    /**
     * Constructor.
     *
     * @param value The enum's underlying value.
     */
    ErrorResponseTypeEnum(String value) {
        this.value = value;
    }

    /**
     * Get the enum type that corresponds to the provided value.
     *
     * @param value The value to process.
     * @return The resulting enum.
     * @throws IllegalArgumentException If the provided value is unknown.
     */
    public static ErrorResponseTypeEnum fromValue(String value) {
        if (FAIL.value.equals(value)) {
            return FAIL;
        } else if (WARN.value.equals(value)) {
            return WARN;
        } else if (LOG.value.equals(value)) {
            return LOG;
        }
        throw new IllegalArgumentException("Unknown response type ["+value+"]");
    }

}
