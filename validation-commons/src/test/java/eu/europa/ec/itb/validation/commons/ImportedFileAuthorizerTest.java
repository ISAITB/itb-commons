package eu.europa.ec.itb.validation.commons;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportedFileAuthorizerTest {

    @Test
    void testPaths() {
        var appConfig1 = mock(ApplicationConfig.class);
        when(appConfig1.isRestrictResourcesToDomain()).thenReturn(true);
        when(appConfig1.getTmpFolder()).thenReturn("/tmp/folder");
        when(appConfig1.getResourceRoot()).thenReturn("/tmp/resourceRoot");
        var appConfig2 = mock(ApplicationConfig.class);
        when(appConfig2.isRestrictResourcesToDomain()).thenReturn(false);
        when(appConfig2.getTmpFolder()).thenReturn("/tmp/folder");
        when(appConfig2.getResourceRoot()).thenReturn("/tmp/resourceRoot");
        var domainConfig1 = mock(DomainConfig.class);
        when(domainConfig1.getDomainRoot()).thenReturn("/tmp/resourceRoot/domain1");

        var authorizer1 = ImportedFileAuthorizer.from(appConfig1, domainConfig1, Path.of("/tmp/validatorTmpFolder"));
        assertTrue(authorizer1.isPathAllowed(Path.of("/tmp/resourceRoot/domain1/file1")));
        assertTrue(authorizer1.isPathAllowed(Path.of("/tmp/validatorTmpFolder/file1")));
        assertThrows(ValidatorException.class, () -> authorizer1.isPathAllowed(Path.of("/other/file1")));
        assertThrows(ValidatorException.class, () -> authorizer1.isPathAllowed(Path.of("/tmp/resourceRoot/domain2/file1")));
        assertTrue(authorizer1.isUriAllowed(Path.of("/tmp/resourceRoot/domain1/file1").toUri()));
        assertTrue(authorizer1.isUriAllowed(Path.of("/tmp/validatorTmpFolder/file1").toUri()));
        assertThrows(ValidatorException.class, () -> authorizer1.isUriAllowed(Path.of("/other/file1").toUri()));
        assertThrows(ValidatorException.class, () -> authorizer1.isUriAllowed(Path.of("/tmp/resourceRoot/domain2/file1").toUri()));

        var authorizer2 = ImportedFileAuthorizer.from(appConfig2, domainConfig1, Path.of("/tmp/validatorTmpFolder"));
        assertTrue(authorizer2.isPathAllowed(Path.of("/tmp/resourceRoot/domain1/file1")));
        assertTrue(authorizer2.isPathAllowed(Path.of("/tmp/validatorTmpFolder/file1")));
        assertThrows(ValidatorException.class, () -> authorizer2.isPathAllowed(Path.of("/other/file1")));
        assertTrue(authorizer2.isPathAllowed(Path.of("/tmp/resourceRoot/domain2/file1")));

        assertTrue(authorizer2.isUriAllowed(Path.of("/tmp/resourceRoot/domain1/file1").toUri()));
        assertTrue(authorizer2.isUriAllowed(Path.of("/tmp/validatorTmpFolder/file1").toUri()));
        assertThrows(ValidatorException.class, () -> authorizer2.isUriAllowed(Path.of("/other/file1").toUri()));
        assertTrue(authorizer2.isUriAllowed(Path.of("/tmp/resourceRoot/domain2/file1").toUri()));

        assertThrows(ValidatorException.class, () -> authorizer2.isUriAllowed(URI.create("ftp://something")));
        assertThrows(ValidatorException.class, () -> authorizer2.isUriAllowed(null));
        assertThrows(ValidatorException.class, () -> authorizer2.isPathAllowed(null));

    }

}
