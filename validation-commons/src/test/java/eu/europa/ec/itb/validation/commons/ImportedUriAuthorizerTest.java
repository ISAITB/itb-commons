package eu.europa.ec.itb.validation.commons;

import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.NormalizedURI;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportedUriAuthorizerTest {

    @Test
    void testForExternalArtifacts() {
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.getNormalizedAllowedUriImports()).thenReturn(List.of(NormalizedURI.of(URI.create("http://10.0.0.1/test"))));
        var domainConfig = mock(DomainConfig.class);
        when(domainConfig.getArtifactInfo()).thenReturn(Map.of("type1", mock(TypedValidationArtifactInfo.class)));
        when(domainConfig.getArtifactInfo().get("type1").getOverallExternalArtifactSupport()).thenReturn(ExternalArtifactSupport.OPTIONAL);
        var authorizer = ImportedUriAuthorizer.from(appConfig, domainConfig, "type1");
        assertTrue(authorizer.isPresent());
        assertTrue(authorizer.get().isUriAllowed("../test/resource"));
        assertTrue(authorizer.get().isUriAllowed("https://www.itb.ec.europa.eu/test/resource"));
        assertTrue(authorizer.get().isUriAllowed("http://10.0.0.1/test/resource"));
        assertThrows(ValidatorException.class, () -> authorizer.get().isUriAllowed("sftp://10.0.0.1/test"));
        assertThrows(ValidatorException.class, () -> authorizer.get().isUriAllowed("http://10.0.0.2/test"));
        assertThrows(ValidatorException.class, () -> authorizer.get().isUriAllowed("http://127.0.0.1/test"));
    }

    @Test
    void testForNoExternalArtifacts() {
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.getNormalizedAllowedUriImports()).thenReturn(List.of(NormalizedURI.of(URI.create("http://10.0.0.1/test"))));
        var domainConfig = mock(DomainConfig.class);
        when(domainConfig.getArtifactInfo()).thenReturn(Map.of("type1", mock(TypedValidationArtifactInfo.class)));
        when(domainConfig.getArtifactInfo().get("type1").getOverallExternalArtifactSupport()).thenReturn(ExternalArtifactSupport.NONE);
        var authorizer = ImportedUriAuthorizer.from(appConfig, domainConfig, "type1");
        assertFalse(authorizer.isPresent());
    }

}
