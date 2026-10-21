package eu.europa.ec.itb.validation.commons;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the limits applied to the remote resources loaded during a validation.
 */
class RemoteResourceBudgetTest {

    private static final int KB = 1024;

    private final URLReader reader = new URLReader();
    private HttpServer server;
    private final AtomicInteger requestsReceived = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/small", exchange -> respond(exchange, 200, 10 * KB, null));
        server.createContext("/medium", exchange -> respond(exchange, 200, 600 * KB, null));
        server.createContext("/large", exchange -> respond(exchange, 200, 2048 * KB, null));
        server.createContext("/chunked-large", exchange -> {
            // No declared length, so the size can only be determined while reading.
            exchange.sendResponseHeaders(200, 0);
            byte[] chunk = new byte[64 * KB];
            try (var out = exchange.getResponseBody()) {
                for (int i = 0; i < 32; i++) {
                    out.write(chunk);
                }
            } catch (IOException e) {
                // The client stopped reading, as expected.
            }
        });
        server.createContext("/redirect/1", exchange -> respond(exchange, 302, 0, "/redirect/2"));
        server.createContext("/redirect/2", exchange -> respond(exchange, 302, 0, "/redirect/3"));
        server.createContext("/redirect/3", exchange -> respond(exchange, 200, 10 * KB, null));
        server.createContext("/stalled", exchange -> {
            // Send part of the response and then stop.
            exchange.sendResponseHeaders(200, 100 * KB);
            exchange.getResponseBody().write(new byte[KB]);
            exchange.getResponseBody().flush();
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void respond(HttpExchange exchange, int status, int size, String location) throws IOException {
        requestsReceived.incrementAndGet();
        if (location != null) {
            exchange.getResponseHeaders().add("Location", location);
        }
        exchange.sendResponseHeaders(status, size == 0 ? -1 : size);
        if (size > 0) {
            byte[] chunk = new byte[Math.min(size, 64 * KB)];
            for (int written = 0; written < size; written += chunk.length) {
                exchange.getResponseBody().write(chunk, 0, Math.min(chunk.length, size - written));
            }
        }
        exchange.close();
    }

    private URI url(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private static ApplicationConfig.RemoteLimits limits(int maxResourceKb, int maxRequests, int maxTotalKb) {
        var limits = new ApplicationConfig.RemoteLimits();
        limits.setMaxResourceSize(DataSize.ofKilobytes(maxResourceKb));
        limits.setMaxRequests(maxRequests);
        limits.setMaxTotalSize(DataSize.ofKilobytes(maxTotalKb));
        limits.setRequestTimeout(30000);
        return limits;
    }

    private long readAll(String path) throws IOException {
        try (var stream = reader.stream(url(path), null, HttpClient.Version.HTTP_1_1).stream()) {
            return stream.readAllBytes().length;
        }
    }

    @Test
    void testNoLimitsWithoutScope() throws IOException {
        assertNull(RemoteResourceBudget.current());
        assertEquals(2048 * KB, readAll("/large"));
    }

    @Test
    void testResourcesWithinLimitsAreLoaded() throws IOException {
        try (var ignored = RemoteResourceBudget.open(limits(1024, 10, 4096))) {
            assertEquals(10 * KB, readAll("/small"));
            assertEquals(600 * KB, readAll("/medium"));
            RemoteResourceBudget.rethrowIfExceeded();
        }
        assertNull(RemoteResourceBudget.current());
    }

    @Test
    void testDeclaredSizeAboveResourceLimitIsRejectedBeforeReading() throws IOException {
        try (var ignored = RemoteResourceBudget.open(limits(1024, 10, 4096))) {
            try (var stream = reader.stream(url("/large"), null, HttpClient.Version.HTTP_1_1).stream()) {
                var error = assertThrows(ValidatorException.class, stream::read);
                assertTrue(error.getMessage().contains("exceeds the maximum allowed size"), error.getMessage());
            }
            assertThrows(ValidatorException.class, RemoteResourceBudget::rethrowIfExceeded);
        }
    }

    @Test
    void testUndeclaredSizeAboveResourceLimitIsRejectedWhileReading() throws IOException {
        try (var ignored = RemoteResourceBudget.open(limits(1024, 10, 4096))) {
            try (var stream = reader.stream(url("/chunked-large"), null, HttpClient.Version.HTTP_1_1).stream()) {
                var error = assertThrows(ValidatorException.class, stream::readAllBytes);
                assertTrue(error.getMessage().contains("exceeds the maximum allowed size"), error.getMessage());
            }
        }
    }

    @Test
    void testTotalSizeAcrossResourcesIsLimited() throws IOException {
        try (var ignored = RemoteResourceBudget.open(limits(1024, 10, 1024))) {
            assertEquals(600 * KB, readAll("/medium"));
            var error = assertThrows(ValidatorException.class, () -> readAll("/medium"));
            assertTrue(error.getMessage().contains("maximum allowed total size"), error.getMessage());
        }
    }

    @Test
    void testNumberOfRequestsIsLimitedAndTheBudgetStaysExhausted() throws IOException {
        try (var ignored = RemoteResourceBudget.open(limits(1024, 2, 4096))) {
            readAll("/small");
            readAll("/small");
            var error = assertThrows(ValidatorException.class, () -> readAll("/small"));
            assertTrue(error.getMessage().contains("maximum allowed number of remote requests"), error.getMessage());
            // Once a limit is crossed nothing further can be loaded.
            assertThrows(ValidatorException.class, () -> readAll("/small"));
            assertEquals(2, requestsReceived.get(), "Requests over the limit must not be made");
        }
    }

    @Test
    void testRedirectsCountAsRequests() throws IOException {
        try (var ignored = RemoteResourceBudget.open(limits(1024, 3, 4096))) {
            // Loading the resource takes 3 requests: /redirect/1 -> /redirect/2 -> /redirect/3.
            assertEquals(10 * KB, readAll("/redirect/1"));
        }
        try (var ignored = RemoteResourceBudget.open(limits(1024, 2, 4096))) {
            var error = assertThrows(ValidatorException.class, () -> readAll("/redirect/1"));
            assertTrue(error.getMessage().contains("maximum allowed number of remote requests"), error.getMessage());
        }
    }

    @Test
    void testStalledResponseTimesOut() throws IOException {
        var limits = limits(1024, 10, 4096);
        limits.setRequestTimeout(1000);
        try (var ignored = RemoteResourceBudget.open(limits)) {
            long start = System.nanoTime();
            try (var stream = reader.stream(url("/stalled"), null, HttpClient.Version.HTTP_1_1).stream()) {
                var error = assertThrows(ValidatorException.class, stream::readAllBytes);
                assertTrue(error.getMessage().contains("maximum allowed time"), error.getMessage());
            }
            assertTrue(Duration.ofNanos(System.nanoTime() - start).toSeconds() < 10, "The read must not wait for the server");
        }
    }

    @Test
    void testBudgetedClientAppliesTheBudgetToAnyRequest() throws Exception {
        var client = BudgetedHttpClient.newClient(Duration.ofSeconds(5));
        var request = HttpRequest.newBuilder(url("/medium")).GET().build();
        try (var ignored = RemoteResourceBudget.open(limits(100, 10, 4096))) {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var stream = response.body()) {
                assertThrows(ValidatorException.class, stream::readAllBytes);
            }
        }
        // Without a budget the same request is not limited.
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (var stream = response.body()) {
            assertEquals(600 * KB, stream.readAllBytes().length);
        }
    }

    @Test
    void testSuspendExemptsWorkFromTheBudget() throws IOException {
        try (var ignored = RemoteResourceBudget.open(limits(1024, 1, 4096))) {
            try (var suspended = RemoteResourceBudget.suspend()) {
                assertNull(RemoteResourceBudget.current());
                readAll("/small");
                readAll("/small");
            }
            assertNotNull(RemoteResourceBudget.current());
            readAll("/small");
            assertThrows(ValidatorException.class, () -> readAll("/small"));
        }
    }

    @Test
    void testNestedScopesShareTheBudget() throws IOException {
        try (var outer = RemoteResourceBudget.open(limits(1024, 2, 4096))) {
            var budget = RemoteResourceBudget.current();
            try (var inner = RemoteResourceBudget.open(limits(1024, 100, 4096))) {
                assertSame(budget, RemoteResourceBudget.current());
                readAll("/small");
            }
            // Closing the inner scope must not end the outer one.
            assertSame(budget, RemoteResourceBudget.current());
            readAll("/small");
            assertThrows(ValidatorException.class, () -> readAll("/small"));
        }
        assertNull(RemoteResourceBudget.current());
    }

    @Test
    void testResponsesReadAsStringAreNotWrapped() throws Exception {
        // Only streams are limited, which is why only streams should be requested.
        var client = BudgetedHttpClient.newClient(Duration.ofSeconds(5));
        var request = HttpRequest.newBuilder(url("/small")).GET().build();
        try (var ignored = RemoteResourceBudget.open(limits(1, 10, 4096))) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(10 * KB, response.body().length());
        }
    }

}
