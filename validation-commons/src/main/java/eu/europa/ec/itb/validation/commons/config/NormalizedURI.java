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

package eu.europa.ec.itb.validation.commons.config;

import java.net.URI;
import java.util.Objects;

/**
 * A URI that has been normalized to allow consistent comparisons with other URIs.
 *
 * @param scheme
 * @param host
 * @param port
 * @param path
 */
public record NormalizedURI(String scheme, String host, int port, String path) {

    /**
     * Create a normalized version of this URI.
     *
     * @param uri The URI to normalize.
     * @return The normalized URI.
     */
    public static NormalizedURI of(URI uri) {
        URI normalised = uri.normalize();
        int port = normalised.getPort() != -1
                ? normalised.getPort()
                : switch (normalised.getScheme().toLowerCase()) {
            case "https" -> 443;
            case "http"  -> 80;
            default      -> -1;
        };
        String path = normalised.getPath() != null ? normalised.getPath() : "";
        return new NormalizedURI(
                normalised.getScheme().toLowerCase(),
                normalised.getHost() != null ? normalised.getHost().toLowerCase() : null,
                port,
                path
        );
    }

    /**
     * Check whether the current URI is a prefix of the provided one.
     *
     * @param input The URI to check.
     * @return The check result.
     */
    public boolean isPrefixOf(NormalizedURI input) {
        return Objects.equals(input.scheme(), this.scheme())
                && Objects.equals(input.host(), this.host())
                && input.port() == this.port()
                && input.path().startsWith(this.path());
    }
}