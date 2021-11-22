package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.RemoteValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.test.BaseSpringTest;
import eu.europa.ec.itb.validation.commons.test.BaseTestConfiguration;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test
    void testObtainBundleNames() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // The common name is 'labels'
        List<String> fileNames_1 = List.of("labels_en_US", "labels_fr_FR", "labels_es_ES", "labels_en_UK");
        // The common name is 'labels'
        List<String> fileNames_2 = List.of("labels_en_US", "labels_fr_FR", "labels_es_ES", "labels");
        // It should not find a correct match and return null.
        List<String> fileNames_3 = List.of("labels_en_US", "labels_fr_FR", "labels_es_ES", "_labels_en_UK");
        // It should not find a correct match and return null.
        List<String> fileNames_4 = List.of("labels_en_US", "labels_fr_FR", "labels_es_ES", "labelsof_en_UK", "wrongName");
        // The common name is 'labels'.
        List<String> fileNames_5 = List.of("labels_en_US", "labels", "labels_es_ES");
        // The common name is 'labels'.
        List<String> fileNames_6 = List.of("labels_en", "labels_fr_FR", "labels_es_ES", "labels_en_US");
        // The common name is 'my_labels'.
        List<String> fileNames_7 = List.of("my_labels_en_US", "my_labels_fr_FR", "my_labels_es_ES");
        // The common name is '_labels'.
        List<String> fileNames_8 = List.of("_labels_en_US", "_labels_fr", "_labels_es_ES");
        // The common name is 'labels'.
        List<String> fileNames_9 = List.of("labels_en_US", "labels_fr_FR", "labels_es_ES");
        // The common name is '_my__labels'
        List<String> fileNames_10 = List.of("_my__labels_en_US", "_my__labels_fr_FR", "_my__labels_es_ES", "_my__labels_es");
        DomainConfigCache configCache = createDomainConfigCache();
        Method obtainBundleNamesMethod = ReflectionUtils.findMethod(configCache.getClass(), "obtainBundleName", List.class);
        assertNotNull(obtainBundleNamesMethod);
        obtainBundleNamesMethod.setAccessible(Boolean.TRUE);
        assertTrue("labels".contentEquals((String)obtainBundleNamesMethod.invoke(configCache, fileNames_1)));
        assertTrue("labels".contentEquals((String)obtainBundleNamesMethod.invoke(configCache, fileNames_2)));
        assertNull(obtainBundleNamesMethod.invoke(configCache, fileNames_3));
        assertNull(obtainBundleNamesMethod.invoke(configCache, fileNames_4));
        assertTrue("labels".contentEquals((String)obtainBundleNamesMethod.invoke(configCache, fileNames_5)));
        assertTrue("labels".contentEquals((String)obtainBundleNamesMethod.invoke(configCache, fileNames_6)));
        assertTrue("my_labels".contentEquals((String)obtainBundleNamesMethod.invoke(configCache, fileNames_7)));
        assertTrue("_labels".contentEquals((String)obtainBundleNamesMethod.invoke(configCache, fileNames_8)));
        assertTrue("labels".contentEquals((String)obtainBundleNamesMethod.invoke(configCache, fileNames_9)));
        assertTrue("_my__labels".contentEquals((String)obtainBundleNamesMethod.invoke(configCache, fileNames_10)));
    }

    @Test
    void testAddResourceBundlesConfiguration() throws IOException {
        var props = new HashMap<String, String>();
        props.put("validator.locale.default", "fr");
        props.put("validator.locale.available", "en,fr");
        props.put("validator.locale.translations", "translations");
        createFileWithContents(Path.of(appConfig.getResourceRoot(), "domainX", "translations", "labels.properties"), "key1=value1\nkey2=value2");
        var domainConfig = new DomainConfig();
        domainConfig.setDomain("domainX");
        DomainConfigCache configCache = createDomainConfigCache();
        configCache.addResourceBundlesConfiguration(domainConfig, new MapConfiguration(props));
        assertNotNull(domainConfig.getAvailableLocales());
        assertEquals(2, domainConfig.getAvailableLocales().size());
        assertEquals(Locale.ENGLISH, new ArrayList<>(domainConfig.getAvailableLocales()).get(0));
        assertEquals(Locale.FRENCH, new ArrayList<>(domainConfig.getAvailableLocales()).get(1));
        assertEquals(Locale.FRENCH, domainConfig.getDefaultLocale());
        assertEquals("labels", domainConfig.getLocaleTranslationsBundle());

        var props1 = new HashMap<String, String>();
        props1.put("validator.locale.default", "fr");
        props1.put("validator.locale.available", "en,fr");
        props1.put("validator.locale.translations", "translations/labels.properties");
        createFileWithContents(Path.of(appConfig.getResourceRoot(), "domainY", "translations", "labels.properties"), "key1=value1\nkey2=value2");
        var domainConfig1 = new DomainConfig();
        domainConfig1.setDomain("domainY");
        DomainConfigCache configCache1 = createDomainConfigCache();
        configCache1.addResourceBundlesConfiguration(domainConfig1, new MapConfiguration(props1));
        assertEquals("labels", domainConfig1.getLocaleTranslationsBundle());

        var props1b = new HashMap<String, String>();
        props1b.put("validator.locale.default", "fr");
        props1b.put("validator.locale.available", "en,fr");
        props1b.put("validator.locale.translations", "translations/labels");
        createFileWithContents(Path.of(appConfig.getResourceRoot(), "domainZ", "translations", "labels.properties"), "key1=value1\nkey2=value2");
        var domainConfig1b = new DomainConfig();
        domainConfig1b.setDomain("domainZ");
        DomainConfigCache configCache1b = createDomainConfigCache();
        configCache1b.addResourceBundlesConfiguration(domainConfig1b, new MapConfiguration(props1b));
        assertEquals("labels", domainConfig1b.getLocaleTranslationsBundle());

        var props2 = new HashMap<String, String>();
        props2.put("validator.locale.available", "en,fr");
        var domainConfig2 = new DomainConfig();
        DomainConfigCache configCache2 = createDomainConfigCache();
        configCache2.addResourceBundlesConfiguration(domainConfig2, new MapConfiguration(props2));
        assertEquals(Locale.ENGLISH, domainConfig2.getDefaultLocale());

        var props3 = new HashMap<String, String>();
        props3.put("validator.locale.default", "es");
        props3.put("validator.locale.available", "en,fr");
        var domainConfig3 = new DomainConfig();
        DomainConfigCache configCache3 = createDomainConfigCache();
        assertThrows(IllegalStateException.class, () -> configCache3.addResourceBundlesConfiguration(domainConfig3, new MapConfiguration(props3)));

        var props4 = new HashMap<String, String>();
        props4.put("validator.locale.default", "de");
        var domainConfig4 = new DomainConfig();
        DomainConfigCache configCache4 = createDomainConfigCache();
        configCache4.addResourceBundlesConfiguration(domainConfig4, new MapConfiguration(props4));
        assertEquals(Locale.GERMAN, domainConfig4.getDefaultLocale());
    }

    @Test
    void testResolvePathForDomain() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        DomainConfigCache configCache = createDomainConfigCache();
        Method resolvePathMethod = ReflectionUtils.findMethod(configCache.getClass(), "resolveFilePathForDomain", String.class, String.class);
        assertNotNull(resolvePathMethod);
        resolvePathMethod.setAccessible(Boolean.TRUE);
        String domain = "domain1";
        when(appConfig.isRestrictResourcesToDomain()).thenReturn(Boolean.TRUE);
        String absolutePath = Path.of(appConfig.getResourceRoot(), domain, "file1.properties").toString();
        String relativePath = Path.of(".", "file1.properties").toString();
        // The path is relative to the domain root folder. It should return a path.
        Object test1Object = resolvePathMethod.invoke(configCache, domain, relativePath);
        assertNotNull(test1Object);
        assert(((Path)test1Object).startsWith(Path.of(appConfig.getResourceRoot(), domain)));        
        // The path is absolute. It should return null.
        Object test2Object = resolvePathMethod.invoke(configCache, domain, absolutePath);
        assertNull(test2Object);
        when(appConfig.isRestrictResourcesToDomain()).thenReturn(Boolean.FALSE);
        // The path is absolute. It should return the path.
        Object test3Object = resolvePathMethod.invoke(configCache, domain, relativePath);
        assertNotNull(test3Object);
        assert(((Path)test3Object).startsWith(Path.of(appConfig.getResourceRoot(), domain)));
        // The path is relative to the domain folder. It should return a path.
        Object test4Object = resolvePathMethod.invoke(configCache, domain, relativePath);
        assertNotNull(test4Object);
        assert(((Path)test4Object).startsWith(Path.of(appConfig.getResourceRoot(), domain)));
    }

    @Test
    void testClose() throws IOException {
        when(appConfig.getDomain()).thenReturn(Set.of("domain1"));
        when(appConfig.getDomainNameToDomainId()).thenReturn(Map.of("domain1", "domain1"));
        when(appConfig.getDomainIdToDomainName()).thenReturn(Map.of("domain1", "domain1"));
        var cache = createDomainConfigCache();
        cache.initBase();
        var domain = cache.getConfigForDomainName("domain1");
        assertNotNull(domain);
        var loader = mock(URLClassLoader.class);
        domain.setLocaleTranslationsLoader(loader);
        assertDoesNotThrow(cache::close);
        verify(loader, times(1)).close();
    }
}
