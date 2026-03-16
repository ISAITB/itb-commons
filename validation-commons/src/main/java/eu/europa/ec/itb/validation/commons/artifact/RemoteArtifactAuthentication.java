/*
 * Copyright (C) 2026 European Union
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

package eu.europa.ec.itb.validation.commons.artifact;

/**
 * Authentication approach for remote artifact lookups.
 */
public enum RemoteArtifactAuthentication {

    /** No authentication needed. */
    NONE,
    /** Use OAuth2.0. */
    OAUTH,
    /** Use a custom HTTP header. */
    HEADER,
    /** Use HTTP basic authentication. */
    BASIC;

    /**
     * Parse the authentication type for the provided string value.
     * @param value The value to parse.
     * @return The authentication type.
     */
    public static RemoteArtifactAuthentication fromValue(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        } else if ("oauth".equals(value)) {
            return OAUTH;
        } else if ("basic".equals(value)) {
            return BASIC;
        } else if ("header".equals(value)) {
            return HEADER;
        } else {
            throw new IllegalArgumentException("Invalid authentication configuration value [%s]".formatted(value));
        }
    }

}
