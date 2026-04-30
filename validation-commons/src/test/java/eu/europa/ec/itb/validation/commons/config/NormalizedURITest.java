package eu.europa.ec.itb.validation.commons.config;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NormalizedURITest {

    @Test
    void testNormalization() {
        assertTrue(NormalizedURI.of(URI.create("http://my.test.org")).isPrefixOf(NormalizedURI.of(URI.create("http://my.test.org"))));
        assertTrue(NormalizedURI.of(URI.create("http://my.test.org")).isPrefixOf(NormalizedURI.of(URI.create("http://my.test.org:80"))));
        assertTrue(NormalizedURI.of(URI.create("https://my.test.org")).isPrefixOf(NormalizedURI.of(URI.create("https://my.test.org:443"))));
        assertTrue(NormalizedURI.of(URI.create("http://my.test.org:80")).isPrefixOf(NormalizedURI.of(URI.create("http://my.test.org"))));
        assertTrue(NormalizedURI.of(URI.create("https://my.test.org:443")).isPrefixOf(NormalizedURI.of(URI.create("https://my.test.org"))));
        assertTrue(NormalizedURI.of(URI.create("https://my.test.org")).isPrefixOf(NormalizedURI.of(URI.create("https://my.test.org/"))));
        assertTrue(NormalizedURI.of(URI.create("https://my.test.org")).isPrefixOf(NormalizedURI.of(URI.create("https://my.test.org/path"))));
        assertTrue(NormalizedURI.of(URI.create("HTTPS://my.test.org")).isPrefixOf(NormalizedURI.of(URI.create("https://my.test.org/path"))));
        assertFalse(NormalizedURI.of(URI.create("https://my.test.org/path")).isPrefixOf(NormalizedURI.of(URI.create("https://my.test.org/other"))));
        assertFalse(NormalizedURI.of(URI.create("https://my.test.org/path/subpath")).isPrefixOf(NormalizedURI.of(URI.create("https://my.test.org/PATH/subpath"))));
    }

}
