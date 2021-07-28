package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.RemoteValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.test.BaseSpringTest;
import eu.europa.ec.itb.validation.commons.test.BaseTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { BaseTestConfiguration.class })
public class DomainConfigCacheTest extends BaseSpringTest {

    @Autowired
    private ApplicationConfig appConfig;

    @BeforeEach
    @Override
    protected void setup() throws IOException {
        super.setup();
        Files.createDirectory(Path.of(appConfig.getResourceRoot(), "domain1"));
        Files.createDirectory(Path.of(appConfig.getResourceRoot(), "domain2"));
        Files.copy(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("testDomainConfigs/plugin.jar")), Path.of(appConfig.getResourceRoot(), "domain1", "plugin.jar"));
        try {
            createFileWithContents(Path.of(appConfig.getResourceRoot(), "domain1", "config.properties"), Files.readString(Paths.get(ClassLoader.getSystemResource("testDomainConfigs/domain1.properties").toURI())));
            createFileWithContents(Path.of(appConfig.getResourceRoot(), "domain2", "config.properties"), Files.readString(Paths.get(ClassLoader.getSystemResource("testDomainConfigs/domain2.properties").toURI())));
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterEach
    @Override
    protected void teardown() {
        super.teardown();
    }

    private DomainConfigCache<DomainConfig> createDomainConfigCache() {
        DomainConfigCache<DomainConfig> cache = new DomainConfigCache<>() {
            @Override
            protected DomainConfig newDomainConfig() {
                return new DomainConfig();
            }

            @Override
            protected ValidatorChannel[] getSupportedChannels() {
                return new ValidatorChannel[]{ValidatorChannel.FORM, ValidatorChannel.REST_API, ValidatorChannel.SOAP_API, ValidatorChannel.EMAIL};
            }
        };
        cache.appConfig = appConfig;
        return cache;
    }

    @Test
    void testConfigLoad() {
        when(appConfig.getDomain()).thenReturn(Set.of("domain1"));
        when(appConfig.getDomainNameToDomainId()).thenReturn(Map.of("domainName1", "domain1"));
        when(appConfig.getDomainIdToDomainName()).thenReturn(Map.of("domain1", "domainName1"));
        when(appConfig.isRestrictResourcesToDomain()).thenReturn(Boolean.TRUE);
        var cache = createDomainConfigCache();
        cache.initBase();
        assertNotNull(cache.getConfigForDomainName("BADNAME"));
        assertFalse(cache.getConfigForDomainName("BADNAME").isDefined());
        var config = cache.getConfigForDomain("domain1");
        var configByName = cache.getConfigForDomainName("domainName1");
        assertSame(config, configByName);
        assertEquals(1, cache.getAllDomainConfigurations().size());
        assertEquals("domain1", config.getDomain());
        assertEquals("domainName1", config.getDomainName());
        assertEquals(2, config.getDeclaredType().size());
        assertTrue(config.getDeclaredType().contains("type1"));
        assertTrue(config.getDeclaredType().contains("type2"));
        assertEquals(5, config.getType().size());
        assertTrue(config.getType().contains("type1.option1_1"));
        assertTrue(config.getType().contains("type1.option1_2"));
        assertTrue(config.getType().contains("type2.option2_1"));
        assertTrue(config.getType().contains("type2.option2_2"));
        assertTrue(config.getType().contains("type2.option2_3"));
        assertEquals(2, config.getChannels().size());
        assertTrue(config.getChannels().contains(ValidatorChannel.FORM));
        assertTrue(config.getChannels().contains(ValidatorChannel.SOAP_API));
        assertEquals(2, config.getValidationTypeOptions().size());
        assertEquals(2, config.getValidationTypeOptions().get("type1").size());
        assertEquals(3, config.getValidationTypeOptions().get("type2").size());
        assertTrue(config.getValidationTypeOptions().get("type1").contains("option1_1"));
        assertTrue(config.getValidationTypeOptions().get("type1").contains("option1_2"));
        assertTrue(config.getValidationTypeOptions().get("type2").contains("option2_1"));
        assertTrue(config.getValidationTypeOptions().get("type2").contains("option2_2"));
        assertTrue(config.getValidationTypeOptions().get("type2").contains("option2_3"));
        assertEquals(1, config.getPluginDefaultConfig().size());
        assertEquals(5, config.getPluginPerTypeConfig().size());
        assertEquals(1, config.getPluginPerTypeConfig().get("type1.option1_1").size());
        assertEquals(0, config.getPluginPerTypeConfig().get("type1.option1_2").size());
        assertEquals(1000, config.getMaximumReportsForDetailedOutput());
        assertEquals(10000, config.getMaximumReportsForXmlOutput());
    }

    @Test
    void testWithMinimalConfig() {
        when(appConfig.getDomain()).thenReturn(Set.of("domain2"));
        when(appConfig.getDomainNameToDomainId()).thenReturn(Map.of("domainName2", "domain2"));
        when(appConfig.getDomainIdToDomainName()).thenReturn(Map.of("domain2", "domainName2"));
        var cache = createDomainConfigCache();
        cache.initBase();
        var config = cache.getConfigForDomain("domain2");
        assertEquals(1, cache.getAllDomainConfigurations().size());
        assertEquals("domain2", config.getDomain());
        assertEquals(1, config.getDeclaredType().size());
        assertEquals(1, config.getType().size());
        assertTrue(config.getValidationTypeOptions().isEmpty());
        assertEquals(5000, config.getMaximumReportsForDetailedOutput());
        assertEquals(50000, config.getMaximumReportsForXmlOutput());
    }

    @Test
    void testMultipleDomains() {
        when(appConfig.getDomain()).thenReturn(Set.of("domain1", "domain2"));
        when(appConfig.getDomainNameToDomainId()).thenReturn(Map.of("domainName1", "domain1", "domainName2", "domain2"));
        when(appConfig.getDomainIdToDomainName()).thenReturn(Map.of("domain1", "domainName1", "domain2", "domainName2"));
        var cache = createDomainConfigCache();
        cache.initBase();
        assertEquals(2, cache.getAllDomainConfigurations().size());
        assertEquals("domain1", cache.getConfigForDomain("domain1").getDomain());
        assertEquals("domain2", cache.getConfigForDomain("domain2").getDomain());
    }

    @Test
    void testOverrides() {
        when(appConfig.getDomain()).thenReturn(Set.of("domain2"));
        when(appConfig.getDomainNameToDomainId()).thenReturn(Map.of("domainName2", "domain2"));
        when(appConfig.getDomainIdToDomainName()).thenReturn(Map.of("domain2", "domainName2"));
        System.setProperty("validator.type", "newType");
        try {
            var cache = createDomainConfigCache();
            cache.initBase();
            var config = cache.getConfigForDomain("domain2");
            assertEquals(1, cache.getAllDomainConfigurations().size());
            assertEquals("domain2", config.getDomain());
            assertEquals(1, config.getDeclaredType().size());
            assertEquals("newType", config.getDeclaredType().get(0));
        } finally {
            System.clearProperty("validator.type");
        }
    }

    @Test
    void testIsInDomainFolder() {
        when(appConfig.getDomain()).thenReturn(Set.of("domain2"));
        when(appConfig.getDomainNameToDomainId()).thenReturn(Map.of("domainName2", "domain2"));
        when(appConfig.getDomainIdToDomainName()).thenReturn(Map.of("domain2", "domainName2"));
        var cache = createDomainConfigCache();
        assertTrue(cache.isInDomainFolder("domain2", "config.properties"));
        assertTrue(cache.isInDomainFolder("domain2", Path.of(appConfig.getResourceRoot(), "domain2", "config.properties").toString()));
        assertFalse(cache.isInDomainFolder("domain2", "../config.properties"));
        assertFalse(cache.isInDomainFolder("domain2", "../domain1/config.properties"));
        assertFalse(cache.isInDomainFolder("domain2", Path.of(appConfig.getResourceRoot(), "domain1", "config.properties").toString()));
    }

    @Test
    void testCompleteValidationArtifactConfig() {
        DomainConfig config = new DomainConfig();
        config.setType(List.of("type1", "type2", "type3"));
        config.setArtifactInfo(Map.of("type1", new TypedValidationArtifactInfo(), "type2", new TypedValidationArtifactInfo(), "type3", new TypedValidationArtifactInfo()));
        config.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        config.getArtifactInfo().get("type1").get("artifact1").setExternalArtifactSupport(ExternalArtifactSupport.NONE);
        config.getArtifactInfo().get("type2").add("artifact1", new ValidationArtifactInfo());
        config.getArtifactInfo().get("type2").get("artifact1").setLocalPath("path");
        config.getArtifactInfo().get("type3").add("artifact1", new ValidationArtifactInfo());
        config.getArtifactInfo().get("type3").get("artifact1").setRemoteArtifacts(List.of(new RemoteValidationArtifactInfo()));
        var cache = createDomainConfigCache();
        cache.completeValidationArtifactConfig(config);
        assertFalse(config.getArtifactInfo().get("type1").hasPreconfiguredArtifacts());
        assertEquals(ExternalArtifactSupport.REQUIRED, config.getArtifactInfo().get("type1").get("artifact1").getExternalArtifactSupport());
        assertEquals(ExternalArtifactSupport.REQUIRED, config.getArtifactInfo().get("type1").getOverallExternalArtifactSupport());
        assertTrue(config.getArtifactInfo().get("type2").hasPreconfiguredArtifacts());
        assertTrue(config.getArtifactInfo().get("type3").hasPreconfiguredArtifacts());
    }

}