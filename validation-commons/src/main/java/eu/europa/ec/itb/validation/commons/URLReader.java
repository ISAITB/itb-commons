package eu.europa.ec.itb.validation.commons;

import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Component responsible of reading a stream from a given URL.
 */
@Component
public class URLReader {

    /**
     * Open a stream to the provided URL.
     *
     * @param uri The URL.
     * @return The stream.
     * @throws ValidatorException If the URL cannot be read.
     */
    InputStream stream(URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .proxy(ProxySelector.getDefault())
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofInputStream())
                    .body();
        } catch (IOException | InterruptedException e) {
            throw new ValidatorException("validator.label.exception.unableToReadURI", e, uri.toString());
        }
    }

}
