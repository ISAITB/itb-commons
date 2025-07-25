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
import java.util.List;
import java.util.Optional;

/**
 * Component responsible for reading a stream from a given URL.
 */
@Component
public class URLReader {

    /**
     * Open a stream to the provided URL.
     *
     * @param uri The URL.
     * @param acceptedContentTypes A (nullable) list of content types to accept for the request.
     * @param httpVersion The HTTP version to use.
     * @return The data of the response (stream and content type).
     * @throws ValidatorException If the URL cannot be read.
     */
    StreamInfo stream(URI uri, List<String> acceptedContentTypes, HttpClient.Version httpVersion) {
        try {
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
        } catch (IOException | InterruptedException e) {
            throw new ValidatorException("validator.label.exception.unableToReadURI", e, uri.toString());
        }
    }

}
