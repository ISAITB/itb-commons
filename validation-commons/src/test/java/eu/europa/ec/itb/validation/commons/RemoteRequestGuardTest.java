package eu.europa.ec.itb.validation.commons;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.NormalizedURI;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the restrictions applied to content that users refer to by means of a URI.
 */
class RemoteRequestGuardTest {

    private final URLReader reader = new URLReader();
    private final List<String> requestedPaths = Collections.synchronizedList(new ArrayList<>());
    private final List<String> authorizationHeaders = Collections.synchronizedList(new ArrayList<>());
    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/allowed/content", exchange -> respond(exchange, 200, "allowed-content", null));
        server.createContext("/allowed/redirect", exchange -> respond(exchange, 302, "", "/allowed/content"));
        server.createContext("/allowed/to-other", exchange -> respond(exchange, 302, "", "/other/content"));
        server.createContext("/other/content", exchange -> respond(exchange, 200, "other-content", null));
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void respond(HttpExchange exchange, int status, String body, String location) throws IOException {
        requestedPaths.add(exchange.getRequestURI().getPath());
        authorizationHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (location != null) {
            exchange.getResponseHeaders().add("Location", location);
        }
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private URI url(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private ApplicationConfig config(String... allowedInputPrefixes) {
        var config = mock(ApplicationConfig.class);
        when(config.getNormalizedAllowedUriInputs()).thenReturn(java.util.Arrays.stream(allowedInputPrefixes).map(prefix -> NormalizedURI.of(URI.create(prefix))).toList());
        when(config.getNormalizedAllowedUriImports()).thenReturn(List.of());
        return config;
    }

    private String read(StreamInfo info) throws IOException {
        try (var stream = info.stream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void testRedirectWithinAllowedInputsIsFollowed() throws IOException {
        var guard = RemoteRequestGuard.forUserInput(config(url("/allowed/").toString()), url("/allowed/redirect"));
        assertEquals("allowed-content", read(reader.stream(url("/allowed/redirect"), null, HttpClient.Version.HTTP_1_1, null, guard)));
    }

    @Test
    void testRedirectToInternalAddressOutsideAllowedInputsIsRejected() {
        var guard = RemoteRequestGuard.forUserInput(config(url("/allowed/").toString()), url("/allowed/to-other"));
        var error = assertThrows(ValidatorException.class, () -> reader.stream(url("/allowed/to-other"), null, HttpClient.Version.HTTP_1_1, null, guard));
        assertTrue(error.getMessage().contains("blocked"), error.getMessage());
        assertEquals(List.of("/allowed/to-other"), requestedPaths, "The redirect target must not be requested");
    }

    @Test
    void testInternalAddressWithoutAllowedInputsIsRejected() {
        var guard = RemoteRequestGuard.forUserInput(config(), url("/allowed/content"));
        assertThrows(ValidatorException.class, () -> reader.stream(url("/allowed/content"), null, HttpClient.Version.HTTP_1_1, null, guard));
        assertTrue(requestedPaths.isEmpty());
    }

    @Test
    void testUserProvidedCredentialsAreOnlySentToTheProvidedOrigin() throws IOException {
        Consumer<HttpRequest.Builder> decorator = builder -> builder.header("Authorization", "Bearer secret");
        var guard = RemoteRequestGuard.forUserInput(config(url("/").toString()), url("/allowed/redirect"));
        read(reader.stream(url("/allowed/redirect"), null, HttpClient.Version.HTTP_1_1, decorator, guard));
        // Same origin throughout.
        assertEquals(List.of("Bearer secret", "Bearer secret"), authorizationHeaders);
        // Another origin (including the same host on another port) never gets them.
        assertFalse(guard.mayDecorate(URI.create("http://127.0.0.1:1/other")));
        assertFalse(guard.mayDecorate(URI.create("https://127.0.0.1:" + server.getAddress().getPort() + "/other")));
        assertFalse(guard.mayDecorate(URI.create("http://example.org/other")));
    }

}
