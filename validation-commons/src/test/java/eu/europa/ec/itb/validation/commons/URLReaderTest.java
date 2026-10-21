package eu.europa.ec.itb.validation.commons;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the reading of URLs with restrictions applied to the requests made.
 */
class URLReaderTest {

    private final URLReader reader = new URLReader();
    private HttpServer server;
    private final AtomicInteger blockedTargetRequests = new AtomicInteger();
    private final List<String> authorizationHeaders = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/content", exchange -> respond(exchange, 200, "content", null));
        server.createContext("/allowed/redirect", exchange -> respond(exchange, 302, "", "/allowed/content"));
        server.createContext("/allowed/content", exchange -> respond(exchange, 200, "allowed-content", null));
        server.createContext("/allowed/to-blocked", exchange -> respond(exchange, 301, "", "/blocked/content"));
        server.createContext("/blocked/content", exchange -> {
            blockedTargetRequests.incrementAndGet();
            respond(exchange, 200, "blocked-content", null);
        });
        server.createContext("/allowed/loop", exchange -> respond(exchange, 302, "", "/allowed/loop"));
        server.createContext("/allowed/to-file", exchange -> respond(exchange, 302, "", "file:///etc/hosts"));
        server.createContext("/allowed/missing-location", exchange -> respond(exchange, 302, "", null));
        server.createContext("/allowed/error", exchange -> respond(exchange, 404, "", null));
        server.createContext("/allowed/chain", exchange -> {
            // Redirect to a path that further redirects, many times.
            String path = exchange.getRequestURI().getPath();
            int count = path.endsWith("chain") ? 0 : Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
            respond(exchange, 302, "", "/allowed/chain/" + (count + 1));
        });
        server.createContext("/allowed/auth", exchange -> {
            authorizationHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            respond(exchange, 200, "auth", null);
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static void respond(HttpExchange exchange, int status, String body, String location) throws IOException {
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

    private RemoteRequestGuard allowOnlyPathsStartingWith(String prefix) {
        return new RemoteRequestGuard(uri -> {
            if (!uri.getPath().startsWith(prefix)) {
                throw new ValidatorException("validator.label.exception.notAllowedToReadImportedUri", uri);
            }
        }, null);
    }

    private String read(StreamInfo info) throws IOException {
        try (var stream = info.stream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void testUnguardedRequestFollowsRedirectsAsBefore() throws IOException {
        assertEquals("blocked-content", read(reader.stream(url("/allowed/to-blocked"), null, HttpClient.Version.HTTP_1_1)));
    }

    @Test
    void testGuardedRequestReturnsContent() throws IOException {
        assertEquals("content", read(reader.stream(url("/content"), null, HttpClient.Version.HTTP_1_1, null, new RemoteRequestGuard(null, null))));
    }

    @Test
    void testGuardedRequestFollowsRedirectToAllowedLocation() throws IOException {
        assertEquals("allowed-content", read(reader.stream(url("/allowed/redirect"), null, HttpClient.Version.HTTP_1_1, null, allowOnlyPathsStartingWith("/allowed"))));
    }

    @Test
    void testGuardedRequestDoesNotFollowRedirectToBlockedLocation() {
        var guard = allowOnlyPathsStartingWith("/allowed");
        assertThrows(ValidatorException.class, () -> reader.stream(url("/allowed/to-blocked"), null, HttpClient.Version.HTTP_1_1, null, guard));
        assertEquals(0, blockedTargetRequests.get(), "The redirect target must not be requested");
    }

    @Test
    void testGuardedRequestRejectsRedirectToNonHttpLocation() {
        var guard = new RemoteRequestGuard(null, null);
        assertThrows(ValidatorException.class, () -> reader.stream(url("/allowed/to-file"), null, HttpClient.Version.HTTP_1_1, null, guard));
    }

    @Test
    void testGuardedRequestRejectsRedirectLoop() {
        var guard = new RemoteRequestGuard(null, null);
        assertThrows(ValidatorException.class, () -> reader.stream(url("/allowed/loop"), null, HttpClient.Version.HTTP_1_1, null, guard));
    }

    @Test
    void testGuardedRequestRejectsTooManyRedirects() {
        var guard = new RemoteRequestGuard(null, null);
        assertThrows(ValidatorException.class, () -> reader.stream(url("/allowed/chain"), null, HttpClient.Version.HTTP_1_1, null, guard));
    }

    @Test
    void testGuardedRequestRejectsRedirectWithoutLocation() {
        var guard = new RemoteRequestGuard(null, null);
        assertThrows(ValidatorException.class, () -> reader.stream(url("/allowed/missing-location"), null, HttpClient.Version.HTTP_1_1, null, guard));
    }

    @Test
    void testGuardedRequestReportsErrorResponses() {
        var guard = new RemoteRequestGuard(null, null);
        assertThrows(ValidatorException.class, () -> reader.stream(url("/allowed/error"), null, HttpClient.Version.HTTP_1_1, null, guard));
    }

    @Test
    void testDecoratorOnlyAppliedWhenPermittedForTheRequest() throws IOException {
        Consumer<HttpRequest.Builder> decorator = builder -> builder.header("Authorization", "Bearer secret");
        // Artefact served from the same host as the request: credentials sent.
        read(reader.stream(url("/allowed/auth"), null, HttpClient.Version.HTTP_1_1, decorator, RemoteRequestGuard.forArtefact(null, url("/artefact.xsd"))));
        // Artefact not retrieved from a URI (e.g. uploaded): credentials never sent.
        read(reader.stream(url("/allowed/auth"), null, HttpClient.Version.HTTP_1_1, decorator, RemoteRequestGuard.forArtefact(null, null)));
        // Artefact served from another host: credentials not sent.
        read(reader.stream(url("/allowed/auth"), null, HttpClient.Version.HTTP_1_1, decorator, RemoteRequestGuard.forArtefact(null, URI.create("https://artefacts.example.org/a.xsd"))));
        assertEquals(List.of("Bearer secret", "null", "null"), authorizationHeaders);
    }

    @Test
    void testSameOriginCheck() {
        // Same scheme, host and (default or explicit) port, irrespective of case and path.
        assertTrue(RemoteRequestGuard.isSameOrigin(URI.create("https://Example.org/a"), URI.create("https://example.ORG:443/b")));
        assertTrue(RemoteRequestGuard.isSameOrigin(URI.create("http://example.org:8080/a"), URI.create("http://example.org:8080/b")));
        // Credentials must not be sent over an unencrypted connection (downgrade) or to another service on the same host.
        assertFalse(RemoteRequestGuard.isSameOrigin(URI.create("https://example.org/a"), URI.create("http://example.org/b")));
        assertFalse(RemoteRequestGuard.isSameOrigin(URI.create("https://example.org/a"), URI.create("https://example.org:8443/b")));
        assertFalse(RemoteRequestGuard.isSameOrigin(URI.create("https://example.org/a"), URI.create("https://other.example.org/b")));
        assertFalse(RemoteRequestGuard.isSameOrigin(URI.create("file:///tmp/a"), URI.create("https://example.org/b")));
        assertFalse(RemoteRequestGuard.isSameOrigin(null, URI.create("https://example.org/b")));
    }

}
