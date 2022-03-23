package eu.europa.ec.itb.validation.commons.jar;

import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class BaseValidationRunnerTest extends BaseTest {

    private BaseValidationRunner<DomainConfig> createRunner() {
        return new BaseValidationRunner<>() {
            @Override
            protected void bootstrapInternal(String[] args, File parentFolder) {
            }
        };
    }

    @Test
    void testIsValidURL() {
        var runner = createRunner();
        assertTrue(runner.isValidURL("http://test.com"));
        assertFalse(runner.isValidURL("BAD_URL"));
    }

    @Test
    void testArgumentAsString() {
        var runner = createRunner();
        assertNull(runner.argumentAsString(new String[] { "A" }, 10));
        assertEquals("b", runner.argumentAsString(new String[] { "a", "b", "c", "d", "e", "f" }, 0));
        assertEquals("c", runner.argumentAsString(new String[] { "a", "b", "c", "d", "e", "f" }, 1));
        assertNull(runner.argumentAsString(new String[] { "a", "b", "c", "d", "e", "f" }, 5));
    }

    @Test
    void testBootstrap() throws IOException {
        var tempFolder = Files.createDirectory(Path.of(tmpFolder.toString(), UUID.randomUUID().toString()));
        final boolean[] called = {false};
        var expectedArgs = new String[] {"a", "b"};
        var runner = new BaseValidationRunner<>() {
            @Override
            protected void bootstrapInternal(String[] args, File parentFolder) {
                assertArrayEquals(expectedArgs, args);
                assertEquals(tempFolder, parentFolder.toPath());
                called[0] = true;
            }
        };
        assertTrue(Files.exists(tempFolder));
        runner.bootstrap(expectedArgs, tempFolder.toFile());
        assertTrue(Files.notExists(tempFolder));
        assertTrue(called[0]);
    }

    @Test
    void testInitSingleDomain() {
        var runner = createRunner();
        var domainConfigCache = mock(DomainConfigCache.class);
        var domain1 = new DomainConfig();
        domain1.setDomain("domain1");
        domain1.setDomainName("domainName1");
        doReturn(List.of(domain1)).when(domainConfigCache).getAllDomainConfigurations();
        runner.domainConfigCache = domainConfigCache;
        assertDoesNotThrow(runner::init);
    }

    @Test
    void testInitNoDomains() {
        var runner = createRunner();
        var domainConfigCache = mock(DomainConfigCache.class);
        doReturn(Collections.EMPTY_LIST).when(domainConfigCache).getAllDomainConfigurations();
        runner.domainConfigCache = domainConfigCache;
        assertThrows(IllegalStateException.class, runner::init);
    }

    @Test
    void testInitMultipleDomains() {
        var runner = createRunner();
        var domainConfigCache = mock(DomainConfigCache.class);
        var domain1 = new DomainConfig();
        domain1.setDomain("domain1");
        domain1.setDomainName("domainName1");
        var domain2 = new DomainConfig();
        domain2.setDomain("domain2");
        domain2.setDomainName("domainName21");
        doReturn(List.of(domain1, domain2)).when(domainConfigCache).getAllDomainConfigurations();
        runner.domainConfigCache = domainConfigCache;
        assertThrows(IllegalArgumentException.class, runner::init);
    }

}
