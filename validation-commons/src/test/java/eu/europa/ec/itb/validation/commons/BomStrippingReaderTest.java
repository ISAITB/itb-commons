package eu.europa.ec.itb.validation.commons;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BomStrippingReaderTest {

    private String expectedContent;

    @BeforeEach
    void readExceptedContent() throws URISyntaxException, IOException {
        expectedContent = Files.readString(Paths.get(ClassLoader.getSystemResource("utfFiles/testFile.txt").toURI()));
    }

    private BufferedReader getReader(String resource) throws URISyntaxException, IOException {
        return new BufferedReader(new BomStrippingReader(Files.newInputStream(Paths.get(ClassLoader.getSystemResource(resource).toURI()))));
    }

    @Test
    void testWithoutBOM() throws IOException, URISyntaxException {
        try (var reader = getReader("utfFiles/testFile.txt")) {
            assertEquals(expectedContent, reader.readLine());
        }
    }

    @Test
    void testForUTF8() throws IOException, URISyntaxException {
        try (var reader = getReader("utfFiles/testFile_BOM_UTF8.txt")) {
            assertEquals(expectedContent, reader.readLine());
        }
    }

    @Test
    void testForUTF16BE() throws IOException, URISyntaxException {
        try (var reader = getReader("utfFiles/testFile_BOM_UTF16BE.txt")) {
            assertEquals(expectedContent, reader.readLine());
        }
    }

    @Test
    void testForUTF16LE() throws IOException, URISyntaxException {
        try (var reader = getReader("utfFiles/testFile_BOM_UTF16LE.txt")) {
            assertEquals(expectedContent, reader.readLine());
        }
    }

    @Test
    void testForUTF32LE() throws IOException, URISyntaxException {
        try (var reader = getReader("utfFiles/testFile_BOM_UTF32LE.txt")) {
            assertEquals(expectedContent, reader.readLine());
        }
    }

    @Test
    void testForUTF32BE() throws IOException, URISyntaxException {
        try (var reader = getReader("utfFiles/testFile_BOM_UTF32BE.txt")) {
            assertEquals(expectedContent, reader.readLine());
        }
    }

}
