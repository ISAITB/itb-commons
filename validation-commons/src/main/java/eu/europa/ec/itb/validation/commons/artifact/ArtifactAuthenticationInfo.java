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
 * Record to hold authentication information for a validation artifact.
 *
 * @param authenticationType The authentication type.
 * @param serviceIdentifier The OAuth 2.0 service identifier.
 * @param username The username for basic HTTP authentication.
 * @param password The password for basic HTTP authentication.
 * @param headerName The header name for HTTP header based authentication.
 * @param headerValue The header value for HTTP header based authentication.
 */
public record ArtifactAuthenticationInfo(RemoteArtifactAuthentication authenticationType, String serviceIdentifier, String username, char[] password, String headerName, String headerValue) {

    /**
     * Convenience factory method for OAuth 2.0.
     *
     * @param serviceIdentifier The OAuth 2.0 service identifier.
     * @return The authentication info.
     */
    public static ArtifactAuthenticationInfo forOAuth(String serviceIdentifier) {
        return new ArtifactAuthenticationInfo(RemoteArtifactAuthentication.OAUTH, serviceIdentifier, null, null, null, null);
    }

    /**
     * Convenience factory method for HTTP basic authentication.
     *
     * @param username The username for basic HTTP authentication.
     * @param password The password for basic HTTP authentication.
     * @return The authentication info.
     */
    public static ArtifactAuthenticationInfo forHttpBasic(String username, char[] password) {
        return new ArtifactAuthenticationInfo(RemoteArtifactAuthentication.BASIC, null, username, password, null, null);
    }

    /**
     * Convenience factory method for HTTP header based authentication.
     *
     * @param headerName The header name for HTTP header based authentication.
     * @param headerValue The header value for HTTP header based authentication.
     * @return The authentication info.
     */
    public static ArtifactAuthenticationInfo forCustomHeader(String headerName, String headerValue) {
        return new ArtifactAuthenticationInfo(RemoteArtifactAuthentication.HEADER, null, null, null, headerName, headerValue);
    }

    /**
     * Convenience factory method for no authentication.
     *
     * @return The authentication info.
     */
    public static ArtifactAuthenticationInfo forNone() {
        return new ArtifactAuthenticationInfo(RemoteArtifactAuthentication.NONE, null, null, null, null, null);
    }

}
