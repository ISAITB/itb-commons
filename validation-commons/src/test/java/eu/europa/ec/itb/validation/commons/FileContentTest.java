package eu.europa.ec.itb.validation.commons;

import com.gitb.core.ValueEmbeddingEnumeration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FileContentTest {

    @Test
    void testIsValidMethod() {
        assertTrue(FileContent.isValidEmbeddingMethod("STRING"));
        assertTrue(FileContent.isValidEmbeddingMethod("BASE_64"));
        assertTrue(FileContent.isValidEmbeddingMethod("URI"));
        assertTrue(FileContent.isValidEmbeddingMethod("URL"));
        assertFalse(FileContent.isValidEmbeddingMethod("BAD"));
    }

    @Test
    void testEmbeddingMethodParse() {
        assertEquals(ValueEmbeddingEnumeration.STRING, FileContent.embeddingMethodFromString("STRING"));
        assertEquals(ValueEmbeddingEnumeration.BASE_64, FileContent.embeddingMethodFromString("BASE64"));
        assertEquals(ValueEmbeddingEnumeration.URI, FileContent.embeddingMethodFromString("URI"));
        assertEquals(ValueEmbeddingEnumeration.URI, FileContent.embeddingMethodFromString("URL"));
        assertNull(FileContent.embeddingMethodFromString(null));
        try {
            FileContent.embeddingMethodFromString("BAD");
            fail("Expected exception for bad value.");
        } catch (IllegalArgumentException e) {
            // Ok.
        }
    }

}
