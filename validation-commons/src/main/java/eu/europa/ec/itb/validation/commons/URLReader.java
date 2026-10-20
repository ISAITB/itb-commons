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

import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Component responsible for reading a stream from a given URL.
 */
@Component
public class URLReader {

    /**
     * The maximum number of redirects followed when requests are guarded.
     */
    static final int MAX_REDIRECTS = 10;

    /**
     * Open a stream to the provided URL.
     *
     * @param uri The URL.
     * @param acceptedContentTypes A (nullable) list of content types to accept for the request.
     * @param httpVersion The HTTP version to use.
     * @return The data of the response (stream and content type).
     * @throws ValidatorException If the URL cannot be read.
     */
    public StreamInfo stream(URI uri, List<String> acceptedContentTypes, HttpClient.Version httpVersion) {
        return stream(uri, acceptedContentTypes, httpVersion, null);
    }

    /**
     * Open a stream to the provided URL.
     *
     * @param uri The URL.
     * @param acceptedContentTypes A (nullable) list of content types to accept for the request.
     * @param httpVersion The HTTP version to use.
     * @param requestDecorator A custom decorator function to apply on the HTTP request (null if not needed).
     * @return The data of the response (stream and content type).
     * @throws ValidatorException If the URL cannot be read.
     */
    public StreamInfo stream(URI uri, List<String> acceptedContentTypes, HttpClient.Version httpVersion, Consumer<HttpRequest.Builder> requestDecorator) {
        try {
            HttpRequest.Builder requestBuilder = newRequestBuilder(uri, acceptedContentTypes, httpVersion);
            if (requestDecorator != null) {
                requestDecorator.accept(requestBuilder);
            }
            HttpRequest request = requestBuilder.build();
            HttpResponse<InputStream> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .proxy(ProxySelector.getDefault())
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                throw new ValidatorException("validator.label.exception.uriReturnedError", uri.toString(), response.statusCode());
            }
            return toStreamInfo(response);
        } catch (IOException | InterruptedException e) {
            throw new ValidatorException("validator.label.exception.unableToReadURI", e, uri.toString());
        }
    }

    /**
     * Open a stream to the provided URL, applying restrictions to the requests made.
     * <p>
     * In contrast to the unrestricted approach, redirects are not followed automatically. They are followed here (up to a
     * maximum number) after checking each target with the provided guard. This ensures that an allowed URI cannot be used to
     * reach a URI that is not allowed. The request decorator (e.g. configured credentials) is only applied to the requests
     * that the guard permits it for.
     *
     * @param uri The URL.
     * @param acceptedContentTypes A (nullable) list of content types to accept for the request.
     * @param httpVersion The HTTP version to use.
     * @param requestDecorator A custom decorator function to apply on the HTTP request (null if not needed).
     * @param guard The restrictions to apply (if null the request is made without restrictions).
     * @return The data of the response (stream and content type).
     * @throws ValidatorException If the URL cannot be read or a request is not allowed.
     */
    public StreamInfo stream(URI uri, List<String> acceptedContentTypes, HttpClient.Version httpVersion, Consumer<HttpRequest.Builder> requestDecorator, RemoteRequestGuard guard) {
        if (guard == null) {
            return stream(uri, acceptedContentTypes, httpVersion, requestDecorator);
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .proxy(ProxySelector.getDefault())
                    .build();
            Set<URI> visited = new HashSet<>();
            URI current = uri;
            for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
                if (!visited.add(current)) {
                    throw new IllegalStateException("Redirect loop detected at [%s]".formatted(current));
                }
                guard.check(current);
                HttpRequest.Builder requestBuilder = newRequestBuilder(current, acceptedContentTypes, httpVersion);
                if (requestDecorator != null && guard.mayDecorate(current)) {
                    requestDecorator.accept(requestBuilder);
                }
                HttpResponse<InputStream> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();
                if (isRedirect(status)) {
                    String location = response.headers().firstValue("Location").orElse(null);
                    response.body().close();
                    if (StringUtils.isBlank(location)) {
                        throw new IllegalStateException("Redirect without location from [%s]".formatted(current));
                    }
                    current = current.resolve(location.trim());
                } else if (status >= 400) {
                    response.body().close();
                    throw new ValidatorException("validator.label.exception.uriReturnedError", current.toString(), status);
                } else {
                    return toStreamInfo(response);
                }
            }
            throw new IllegalStateException("Too many redirects from [%s]".formatted(uri));
        } catch (IOException | IllegalStateException | IllegalArgumentException e) {
            throw new ValidatorException("validator.label.exception.unableToReadURI", e, uri.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ValidatorException("validator.label.exception.unableToReadURI", e, uri.toString());
        }
    }

    /**
     * @param status The HTTP status code.
     * @return Whether the status is a redirect that we follow.
     */
    private boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    /**
     * Create the request builder for a GET request.
     *
     * @param uri The URL.
     * @param acceptedContentTypes A (nullable) list of content types to accept for the request.
     * @param httpVersion The HTTP version to use.
     * @return The builder.
     */
    private HttpRequest.Builder newRequestBuilder(URI uri, List<String> acceptedContentTypes, HttpClient.Version httpVersion) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .version(httpVersion)
                .GET();
        if (acceptedContentTypes != null && !acceptedContentTypes.isEmpty()) {
            List<String> nonEmptyContentTypes = acceptedContentTypes.stream().filter(value -> value != null && !value.isBlank()).toList();
            if (!nonEmptyContentTypes.isEmpty()) {
                requestBuilder = requestBuilder.header("Accept", String.join(",", nonEmptyContentTypes));
            }
        }
        return requestBuilder;
    }

    /**
     * Convert a response to the information we return.
     *
     * @param response The response.
     * @return The stream and content type.
     */
    private StreamInfo toStreamInfo(HttpResponse<InputStream> response) {
        Optional<String> contentType = response.headers().firstValue("Content-Type")
                .map(value -> {
                    if (Strings.CS.contains(value, ";")) {
                        String[] parts = StringUtils.split(value, ";");
                        return parts[0].trim();
                    } else {
                        return value;
                    }
                });
        return new StreamInfo(response.body(), contentType);
    }

}
