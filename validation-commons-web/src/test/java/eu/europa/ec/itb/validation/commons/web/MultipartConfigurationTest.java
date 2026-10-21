package eu.europa.ec.itb.validation.commons.web;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.util.unit.DataSize;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that uploaded multipart request parts are stored in the validator's temporary folder.
 */
class MultipartConfigurationTest {

    private ApplicationConfig appConfig(String tmpFolder) {
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.getMultipartTmpFolder()).thenCallRealMethod();
        when(appConfig.getTmpFolder()).thenReturn(tmpFolder);
        return appConfig;
    }

    @Test
    void testLocationIsAbsoluteAndMatchesTheFolderPreparedByTheValidator() {
        // On Windows this is not an absolute path (and would be considered by the web server to be relative to its work folder).
        var appConfig = appConfig("/validator/tmp");
        var element = new MultipartConfiguration().multipartConfigElement(new MultipartProperties(), appConfig);
        var location = new File(element.getLocation());
        assertTrue(location.isAbsolute(), "The location must be absolute but was " + location);
        assertEquals(appConfig.getMultipartTmpFolder(), location);
        assertEquals("multipart", location.getName());
        assertEquals(new File("/validator/tmp").getAbsoluteFile(), location.getParentFile());
    }

    @Test
    void testRelativeTemporaryFolderIsResolvedAgainstTheWorkingDirectory() {
        var element = new MultipartConfiguration().multipartConfigElement(new MultipartProperties(), appConfig("relative/tmp"));
        var location = new File(element.getLocation());
        assertTrue(location.isAbsolute());
        assertEquals(new File("relative/tmp/multipart").getAbsoluteFile(), location);
    }

    @Test
    void testOtherMultipartSettingsAreKept() {
        var properties = new MultipartProperties();
        properties.setMaxFileSize(DataSize.ofMegabytes(10));
        properties.setMaxRequestSize(DataSize.ofMegabytes(100));
        properties.setLocation("/ignored/location");
        var element = new MultipartConfiguration().multipartConfigElement(properties, appConfig("/validator/tmp"));
        assertEquals(DataSize.ofMegabytes(10).toBytes(), element.getMaxFileSize());
        assertEquals(DataSize.ofMegabytes(100).toBytes(), element.getMaxRequestSize());
        assertEquals(new File("/validator/tmp/multipart").getAbsolutePath(), element.getLocation());
    }

}
