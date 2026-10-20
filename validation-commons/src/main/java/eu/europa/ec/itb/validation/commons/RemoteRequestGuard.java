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

package eu.europa.ec.itb.validation.commons;

import java.net.URI;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Restrictions to apply when a validation artefact causes further requests to be made (e.g. schema imports).
 * <p>
 * The restrictions are applied by the {@link URLReader} to every request it makes, including those that follow redirects,
 * so that a permitted URI cannot be used to reach a URI that would not have been permitted.
 *
 * @param uriCheck Check made before each request, throwing an exception if the URI is not allowed (may be null if no restrictions apply).
 * @param decoratorApplies Determines whether the request decorator (e.g. configured credentials) may be applied to a request for a URI (null means always).
 */
public record RemoteRequestGuard(Consumer<URI> uriCheck, Predicate<URI> decoratorApplies) {

    /**
     * Create the guard for requests made on behalf of a validation artefact (e.g. the imports of a schema).
     * <p>
     * Credentials configured for the artefact are only sent to the host that the artefact itself is served from. Artefacts
     * that were not retrieved from a remote host (e.g. uploaded content) never cause credentials to be sent.
     *
     * @param uriAuthorizer The authorizer for the requested URIs (null if the artefact is trusted).
     * @param artefactSource The URI from which the artefact was retrieved (null if it was not retrieved from a URI).
     * @return The guard.
     */
    public static RemoteRequestGuard forArtefact(ImportedUriAuthorizer uriAuthorizer, URI artefactSource) {
        return new RemoteRequestGuard(
                uriAuthorizer == null ? null : uriAuthorizer::isUriAllowed,
                uri -> isSameHost(artefactSource, uri)
        );
    }

    /**
     * Check whether the two URIs refer to the same (non-empty) host.
     *
     * @param first The first URI (may be null).
     * @param second The second URI (may be null).
     * @return The check result.
     */
    static boolean isSameHost(URI first, URI second) {
        if (first == null || second == null || first.getHost() == null || second.getHost() == null) {
            return false;
        }
        return first.getHost().toLowerCase(Locale.ROOT).equals(second.getHost().toLowerCase(Locale.ROOT));
    }

    /**
     * Check that a request may be made for the provided URI.
     *
     * @param uri The URI.
     */
    public void check(URI uri) {
        if (uriCheck != null) {
            uriCheck.accept(uri);
        }
    }

    /**
     * @param uri The URI of a request.
     * @return Whether the request decorator may be applied to a request for the given URI.
     */
    public boolean mayDecorate(URI uri) {
        return decoratorApplies == null || decoratorApplies.test(uri);
    }

}
