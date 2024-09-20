package eu.europa.ec.itb.validation.commons;

import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(Lifecycle.PER_CLASS)
class LocalisationHelperTest {

    private String localTranslationsFolderPath;
    private String localTranslationsBundleName;
    private boolean tmpDirExisted;
    private Locale defaultLocale;
    private DomainConfig config;

    @BeforeAll
    void setUp() throws IOException {
        this.defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.ROOT);
        // create a folder for the domain config properties
        File currentDir = new File(Thread.currentThread().getContextClassLoader().getResource("").getPath());
        File tmpWd = new File(currentDir, "tmp");
        tmpDirExisted = tmpWd.exists();
        if (!tmpDirExisted) {
            tmpWd.mkdir();
        }
        this.config = new WebDomainConfig();
        Map<String, String> domainProperties = new HashMap<>();
        domainProperties.put("test", "testDefaultDomain");
        domainProperties.put("testParameterised1", "Test1 {0}");
        this.config.setDomainProperties(domainProperties);
        // create a folder for the locale translations and set up path
        File localTranslationsWd = new File(tmpWd, UUID.randomUUID().toString());
        localTranslationsWd.mkdirs();
        this.localTranslationsFolderPath = localTranslationsWd.getAbsolutePath();
        this.localTranslationsBundleName = "localTranslations";
        this.config.setLocaleTranslationsLoader(new URLClassLoader(new URL[] {localTranslationsWd.getAbsoluteFile().toURI().toURL()}));
        this.config.setLocaleTranslationsBundle(localTranslationsBundleName);
        // add properties files
        Properties localTranslationsProps = new Properties();
        localTranslationsProps.setProperty("testLocal", "testDefaultLocal");
        localTranslationsProps.setProperty("testParameterised2", "Test2 {0}");
        localTranslationsProps.store(
                new FileWriter(new File(localTranslationsWd, localTranslationsBundleName + ".properties")),
                "Test properties for the defaultlocale translations");
        Properties localTranslationsEnProps = new Properties();
        localTranslationsEnProps.setProperty("testLocal", "testEnLocal");
        localTranslationsEnProps.store(
                new FileWriter(new File(localTranslationsWd, localTranslationsBundleName + "_en.properties")),
                "Test properties for the English locale translations");
        Properties localTranslationsEsProps = new Properties();
        localTranslationsEsProps.setProperty("testLocal", "testEsLocal");
        localTranslationsEsProps.store(
                new FileWriter(new File(localTranslationsWd, localTranslationsBundleName + "_es.properties")),
                "Test properties for the Spanish locale translations");
    }

    @Test
    void testInitializeLocaliser() throws IllegalArgumentException {
        LocalisationHelper localiser = new LocalisationHelper(this.config, Locale.ENGLISH);
        assertNotNull(localiser);
        DomainConfig configWithoutLocaleTranslations = new WebDomainConfig();
        Map<String, String> domainProperties = new HashMap<String, String>();
        configWithoutLocaleTranslations.setDomainProperties(domainProperties);
        LocalisationHelper localiserWithoutLocaleTranslations = new LocalisationHelper(configWithoutLocaleTranslations, Locale.ENGLISH);
        assertNotNull(localiserWithoutLocaleTranslations);
    }

    @Test
    void testInitializeLocaliserForLocale() throws IllegalArgumentException {
        LocalisationHelper localiser = new LocalisationHelper(Locale.ENGLISH);
        assertNotNull(localiser);
        assertEquals(Locale.ENGLISH, localiser.getLocale());
    }

    @Test
    void testInitializeLocaliserForDefaultLocales() throws IllegalArgumentException {
        var domainConfig = mock(DomainConfig.class);
        when(domainConfig.getDefaultLocale()).thenReturn(Locale.GERMAN);
        LocalisationHelper localiser1 = new LocalisationHelper(domainConfig);
        assertEquals(Locale.GERMAN, localiser1.getLocale());
        LocalisationHelper localiser2 = new LocalisationHelper(null, null);
        assertEquals(Locale.ENGLISH, localiser2.getLocale());
    }

    @Test
    void testPropertyExists() {
        LocalisationHelper localiser = new LocalisationHelper(this.config, Locale.ENGLISH);
        assertTrue(localiser.propertyExists("test"));
        assertTrue(localiser.propertyExists("testLocal"));
        assertFalse(localiser.propertyExists("nonExistingProperty"));
    }

    @Test
    void testLocaliseParameterised() {
        LocalisationHelper localiser = new LocalisationHelper(this.config, Locale.ENGLISH);
        assertEquals("Test1 5", localiser.localise("testParameterised1", "5"));
        assertEquals("Test2 6", localiser.localise("testParameterised2", "6"));
        assertEquals("Test1 5", localiser.localise("testParameterised1", 5L));
        assertEquals("Test2 6", localiser.localise("testParameterised2", 6L));
    }

    @Test
    void testLocalisationExistingLocale() {
        LocalisationHelper localiser = new LocalisationHelper(this.config, Locale.ENGLISH);
        assertEquals("testDefault", localiser.localise("testDefault"));
        assertEquals("testValidator", localiser.localise("testValidator"));
        assertEquals("testDefaultDomain", localiser.localise("test"));
        assertEquals("testEnLocal", localiser.localise("testLocal"));
    }

    @Test
    void testLocalisationDefaultValues() {
        LocalisationHelper localiser = new LocalisationHelper(this.config, Locale.GERMAN);
        assertEquals("testDefault", localiser.localise("testDefault"));
        assertEquals("testValidator", localiser.localise("testValidator"));
        assertEquals("testDefaultDomain", localiser.localise("test"));
        assertEquals("testDefaultLocal", localiser.localise("testLocal"));
    }

    @Test
    void testLocalisationUnknownProperty() {
        LocalisationHelper localiser = new LocalisationHelper(this.config, Locale.GERMAN);
        assertEquals("[INVALID_KEY]", localiser.localise("INVALID_KEY"));
    }

    @Test
    void testWithoutLocalTranslations() {
        DomainConfig configWithoutLocaleTranslations = new WebDomainConfig();
        Map<String, String> domainProperties = new HashMap<String, String>();
        domainProperties.put("test", "testDefaultDomain");
        domainProperties.put("testParameterised1", "Test1 %d");
        configWithoutLocaleTranslations.setDomainProperties(domainProperties);
        LocalisationHelper localiser = new LocalisationHelper(configWithoutLocaleTranslations, Locale.ENGLISH);
        assertEquals("testDefault", localiser.localise("testDefault"));
        assertEquals("testValidator", localiser.localise("testValidator"));
        assertEquals("testDefaultDomain", localiser.localise("test"));
        assertEquals("NonExisting", localiser.localise("testLocal"));
    }

    @AfterAll
    void cleanUp() {
        Locale.setDefault(this.defaultLocale);
        // clean the temporary directories
        if (tmpDirExisted) {
            deleteDirectory(new File(localTranslationsFolderPath));
        } else {
            deleteDirectory(new File("./tmp"));
        }
    }

    private boolean deleteDirectory(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            var files = fileOrDir.listFiles();
            for (File child : files) {
                deleteDirectory(child);
            }
            return fileOrDir.delete();
        }
        return fileOrDir.delete();
    }

}
