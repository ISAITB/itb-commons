package eu.europa.ec.itb.validation.commons.config;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = ApplicationConfigTest.ApplicationConfigForTesting.class)
@ContextConfiguration(classes = {ApplicationConfigTest.TestConfig.class})
@TestPropertySource("classpath:testConfig/testApplication.properties")
class ApplicationConfigTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        public ApplicationConfig appConfig() {
            return new ApplicationConfigForTesting();
        }
    }

    @ConfigurationProperties(prefix = "validator")
    static class ApplicationConfigForTesting extends ApplicationConfig {}

    @Autowired
    private ApplicationConfig appConfig;

    @BeforeEach
    void setup() throws IOException {
        var tmpFolder = Files.createTempDirectory("itb");
        var resourceRoot = Path.of(tmpFolder.toString(), "resourceRoot");
        appConfig.setTmpFolder(tmpFolder.toString());
        appConfig.setResourceRoot(resourceRoot.toString());

    }

    @AfterEach
    void teardown() {
        FileUtils.deleteQuietly(Path.of(appConfig.getTmpFolder()).toFile());
    }

    @Test
    void testConfigLoading() {
        assertEquals(500000L, appConfig.getCleanupWebRate());
        assertEquals(2, appConfig.getDomain().size());
        assertTrue(appConfig.getDomain().contains("domain1"));
        assertTrue(appConfig.getDomain().contains("domain2"));
        assertTrue(appConfig.isRestrictResourcesToDomain());
        assertEquals("http://test.com", appConfig.getWebhook().getStatistics());
        assertEquals("secret", appConfig.getWebhook().getStatisticsSecret());
        assertTrue(appConfig.getWebhook().isStatisticsEnableCountryDetection());
        assertEquals("/tmp/file.mmdb", appConfig.getWebhook().getStatisticsCountryDetectionDbFile());
        assertEquals("HEADER", appConfig.getWebhook().getIpHeader());
        assertTrue(appConfig.isSupportsAdditionalInformationInReportItems());
        assertTrue(appConfig.isSupportsTestDefinitionInReportItems());
    }

    @Test
    void testInit() throws IOException {
        Files.createDirectory(Path.of(appConfig.getResourceRoot()));
        Files.createDirectory(Path.of(appConfig.getResourceRoot(), "domain1"));
        Files.createDirectory(Path.of(appConfig.getResourceRoot(), "domain2"));
        appConfig.init();
        assertEquals("nameForDomain1", appConfig.getDomainIdToDomainName().get("domain1"));
        assertEquals("nameForDomain2", appConfig.getDomainIdToDomainName().get("domain2"));
        assertEquals("domain1", appConfig.getDomainNameToDomainId().get("nameForDomain1"));
        assertEquals("domain2", appConfig.getDomainNameToDomainId().get("nameForDomain2"));
        assertNotNull(appConfig.getStartupTimestamp());
        assertNotNull(appConfig.getResourceUpdateTimestamp());
        var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss (XXX)");
        assertDoesNotThrow(() -> sdf.parse(appConfig.getStartupTimestamp()));
        assertDoesNotThrow(() -> sdf.parse(appConfig.getResourceUpdateTimestamp()));
    }

    @Test
    void testInitDomainsWithAutodetectOk() throws IOException {
        appConfig.getDomain().clear();
        Files.createDirectory(Path.of(appConfig.getResourceRoot()));
        Files.createDirectory(Path.of(appConfig.getResourceRoot(), "domain1"));
        Files.createDirectory(Path.of(appConfig.getResourceRoot(), "domain2"));
        appConfig.init();
        assertEquals("nameForDomain1", appConfig.getDomainIdToDomainName().get("domain1"));
        assertEquals("nameForDomain2", appConfig.getDomainIdToDomainName().get("domain2"));
        assertEquals("domain1", appConfig.getDomainNameToDomainId().get("nameForDomain1"));
        assertEquals("domain2", appConfig.getDomainNameToDomainId().get("nameForDomain2"));
    }

    @Test
    void testInitDomainsWithAutodetectNok() throws IOException {
        var domains = appConfig.getDomain();
        try {
            appConfig.setDomain(null);
            Files.createDirectory(Path.of(appConfig.getResourceRoot()));
            assertThrows(IllegalStateException.class, () -> appConfig.init());
        } finally {
            appConfig.setDomain(domains);
        }
    }

    @Test
    void testInitBadResourceRoot() {
        appConfig.setResourceRoot(null);
        assertThrows(IllegalStateException.class, () -> appConfig.init());
    }

}
