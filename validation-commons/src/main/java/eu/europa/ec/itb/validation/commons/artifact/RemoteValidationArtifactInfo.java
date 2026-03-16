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
 * Information on remotely loaded validation artifacts.
 */
public class RemoteValidationArtifactInfo extends CommonValidationArtifactInfo {

    private String url;
    private RemoteArtifactAuthentication authenticationType;
    private String serviceIdentifier;
    private String username;
    private char[] password;
    private String headerName;
    private String headerValue;

    /**
     * @return The URL to load the artifact from.
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url The URL to load the artifact from.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /** @return The authentication type to apply. */
    public RemoteArtifactAuthentication getAuthenticationType() {
        return authenticationType;
    }

    /** @param authenticationType The authentication type to apply. */
    public void setAuthenticationType(RemoteArtifactAuthentication authenticationType) {
        this.authenticationType = authenticationType;
    }

    /** @return The service identifier to use (for OAuth2.0). */
    public String getServiceIdentifier() {
        return serviceIdentifier;
    }

    /** @param serviceIdentifier The service identifier to use (for OAuth2.0). */
    public void setServiceIdentifier(String serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }

    /** @return The username to use (for HTTP basic authentication). */
    public String getUsername() {
        return username;
    }

    /** @param username The username to use (for HTTP basic authentication). */
    public void setUsername(String username) {
        this.username = username;
    }

    /** @return The password to use (for HTTP basic authentication). */
    public char[] getPassword() {
        return password;
    }

    /** @param password The username to use (for HTTP basic authentication). */
    public void setPassword(char[] password) {
        this.password = password;
    }

    /** @return The name of the HTTP header to include for authentication (for HTTP header based authentication). */
    public String getHeaderName() {
        return headerName;
    }

    /** @param headerName The name of the HTTP header to include for authentication (for HTTP header based authentication). */
    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    /** @return The value of the HTTP header to include for authentication (for HTTP header based authentication). */
    public String getHeaderValue() {
        return headerValue;
    }

    /** @param headerValue The value of the HTTP header to include for authentication (for HTTP header based authentication). */
    public void setHeaderValue(String headerValue) {
        this.headerValue = headerValue;
    }
}
