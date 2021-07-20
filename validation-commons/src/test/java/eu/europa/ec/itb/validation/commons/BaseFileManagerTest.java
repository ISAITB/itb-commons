package eu.europa.ec.itb.validation.commons;

import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import eu.europa.ec.itb.validation.commons.artifact.RemoteValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.test.BaseSpringTest;
import eu.europa.ec.itb.validation.commons.test.BaseTestConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { BaseFileManagerTest.TestConfig.class, BaseTestConfiguration.class })
public class BaseFileManagerTest extends BaseSpringTest {

    @Primary
    @TestConfiguration
    static class TestConfig {
        @Bean
        public BaseFileManager<ApplicationConfig> testFileManager() {
            return new BaseFileManager<>() {
                @Override
                public String getFileExtension(String contentType) {
                    return "txt";
                }
                @Override
                public void init() {
                    externalDomainFileCacheLocks.clear();
                    preconfiguredLocalArtifactMap.clear();
                    preconfiguredRemoteArtifactMap.clear();
                    preprocessor = null;
                }
            };
        }
    }

    @Autowired
    private URLReader urlReader;
    @Autowired
    private DomainConfigCache<DomainConfig> domainConfigCache;
    @Autowired
    private BaseFileManager<ApplicationConfig> fileManager;

    @BeforeEach
    protected void setup() throws IOException {
        super.setup();
        fileManager.init();
    }

    @AfterEach
    protected void teardown() {
        super.teardown();
        fileManager.init();
        reset(urlReader, domainConfigCache);
    }

    @Test
    void testGetContentTypeForFile() {
        assertNull(fileManager.getContentTypeForFile(null, null), "The default content type for a file should return null by default.");
    }

    @Test
    void testIsAcceptedArtifactFile() {
        assertTrue(fileManager.isAcceptedArtifactFile(null, null), "By default all artifact file should be considered accepted.");
    }

    @Test
    void testGetTmpFolder() {
        var folder = fileManager.getTempFolder();
        assertNotNull(folder);
        assertEquals(appConfig.getTmpFolder(), fileManager.getTempFolder().getAbsolutePath());
    }

    @Test
    void testGetWebTmpFolder() {
        var folder = fileManager.getWebTmpFolder();
        assertNotNull(folder);
        assertEquals(Path.of(appConfig.getTmpFolder(), "web"), folder.toPath());
    }

    @Test
    void testGetRemoteFileCacheFolder() {
        var folder = fileManager.getRemoteFileCacheFolder();
        assertNotNull(folder);
        assertEquals(Path.of(appConfig.getTmpFolder(), "remote_config"), folder.toPath());
    }

    @Test
    void testGetFileFromBase64() throws IOException {
        var testContent = "TEST";
        var targetFolder = Path.of(appConfig.getTmpFolder(), "base64test");
        var result = fileManager.getFileFromBase64(targetFolder.toFile(), Base64.getEncoder().encodeToString(testContent.getBytes(StandardCharsets.UTF_8)), "text/plain");
        assertNotNull(result);
        assertEquals(targetFolder, result.getParentFile().toPath(), "File not created under provided target folder.");
        assertEquals(testContent, Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromBase64ForNullContentType() throws IOException {
        var testContent = "TEST";
        var targetFolder = Path.of(appConfig.getTmpFolder(), "web"); // Stored in web temp folder by default.
        var result = fileManager.getFileFromBase64(null, Base64.getEncoder().encodeToString(testContent.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(result);
        assertEquals(targetFolder, result.getParentFile().toPath(), "File not created under provided target folder.");
        assertEquals(testContent, Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromBase64ForNullTargetAndContentType() throws IOException {
        var testContent = "TEST";
        var targetFolder = Path.of(appConfig.getTmpFolder(), "web"); // Stored in web temp folder by default.
        var result = fileManager.getFileFromBase64(null, Base64.getEncoder().encodeToString(testContent.getBytes(StandardCharsets.UTF_8)), null);
        assertNotNull(result);
        assertEquals(targetFolder, result.getParentFile().toPath(), "File not created under provided target folder.");
        assertEquals(testContent, Files.readString(result.toPath()));
    }

    @Test
    void testGetInputStreamFromURL() {
        var testURI = URI.create("http://test.com");
        when(urlReader.stream(any())).thenAnswer((Answer<InputStream>) invocationOnMock -> {
            var arg = invocationOnMock.getArgument(0, URI.class);
            if (!testURI.equals(arg)) {
                fail("Unexpected URL called.");
            }
            return InputStream.nullInputStream();
        });
        var result = fileManager.getInputStreamFromURL(testURI.toString());
        assertNotNull(result);
    }

    @Test
    void testGetFileFromURL() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "url_test");
        var testURI = URI.create("http://test.com");
        when(urlReader.stream(any())).thenReturn(new ByteArrayInputStream("TEST".getBytes(StandardCharsets.UTF_8)));
        var result = fileManager.getFileFromURL(targetFolder.toFile(), testURI.toString(), "txt", "aFile");
        assertNotNull(result);
        assertEquals(Path.of(targetFolder.toString(), "aFile.txt"), result.toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromURLWithURL() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "url_test");
        var testURI = URI.create("http://test.com");
        when(urlReader.stream(any())).thenReturn(new ByteArrayInputStream("TEST".getBytes(StandardCharsets.UTF_8)));
        var result = fileManager.getFileFromURL(targetFolder.toFile(), testURI.toString());
        assertNotNull(result);
        assertEquals(targetFolder, result.getParentFile().toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromURLWithURLAndFilename() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "url_test");
        var testURI = URI.create("http://test.com");
        when(urlReader.stream(any())).thenReturn(new ByteArrayInputStream("TEST".getBytes(StandardCharsets.UTF_8)));
        var result = fileManager.getFileFromURL(targetFolder.toFile(), testURI.toString(),"aFile.txt");
        assertNotNull(result);
        assertEquals(Path.of(targetFolder.toString(), "aFile.txt"), result.toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromURLWithURLAndFilenameAndExtension() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "url_test");
        var testURI = URI.create("http://test.com");
        when(urlReader.stream(any())).thenReturn(new ByteArrayInputStream("TEST".getBytes(StandardCharsets.UTF_8)));
        var result = fileManager.getFileFromURL(targetFolder.toFile(), testURI.toString(),".txt", "aFile");
        assertNotNull(result);
        assertEquals(Path.of(targetFolder.toString(), "aFile.txt"), result.toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromURLWithURLAndFilenameAndExtensionAndArtifactType() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "url_test");
        var testURI = URI.create("http://test.com");
        when(urlReader.stream(any())).thenReturn(new ByteArrayInputStream("TEST".getBytes(StandardCharsets.UTF_8)));
        var result = fileManager.getFileFromURL(targetFolder.toFile(), testURI.toString(),".txt", "aFile", "artifact1");
        assertNotNull(result);
        assertEquals(Path.of(targetFolder.toString(), "aFile.txt"), result.toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromURLDetermineFileName() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "url_test");
        var testURI = URI.create("http://test.com/determinedFile.txt");
        when(urlReader.stream(any())).thenReturn(new ByteArrayInputStream("TEST".getBytes(StandardCharsets.UTF_8)));
        var result = fileManager.getFileFromURL(targetFolder.toFile(), testURI.toString(), null, null);
        assertNotNull(result);
        assertEquals(Path.of(targetFolder.toString(), "determinedFile.txt"), result.toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromURLWithPreprocessor() throws IOException {
        fileManager.preprocessor = new TestPreprocessor();
        var targetFolder = Path.of(appConfig.getTmpFolder(), "url_test");
        var testURI = URI.create("http://test.com");
        when(urlReader.stream(any())).thenReturn(new ByteArrayInputStream("TEST".getBytes(StandardCharsets.UTF_8)));
        var result = fileManager.getFileFromURL(targetFolder.toFile(), testURI.toString(), "txt", "aFile", targetFolder.toFile(), "", "");
        assertNotNull(result);
        assertEquals(Path.of(targetFolder.toString(), "PROCESSED_aFile.txt"), result.toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromURLOrBase64ForFileAndContent() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "url_or_base64_test");
        var result = fileManager.getFileFromURLOrBase64(targetFolder.toFile(), Base64.getEncoder().encodeToString("TEST".getBytes(StandardCharsets.UTF_8)));
        assertNotNull(result);
        assertEquals(targetFolder, result.getParentFile().toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromURLOrBase64ForFileAndContentNoTargetFolder() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "web");
        var result = fileManager.getFileFromURLOrBase64(null, Base64.getEncoder().encodeToString("TEST".getBytes(StandardCharsets.UTF_8)));
        assertNotNull(result);
        assertEquals(targetFolder, result.getParentFile().toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromURLOrBase64ForFileAndContentAndContentType() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "url_or_base64_test");
        var result = fileManager.getFileFromURLOrBase64(targetFolder.toFile(), Base64.getEncoder().encodeToString("TEST".getBytes(StandardCharsets.UTF_8)), "text/plain");
        assertNotNull(result);
        assertEquals(targetFolder, result.getParentFile().toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromURLOrBase64ForFileAndContentAndContentTypeAndArtifactTypeForBase64() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "url_or_base64_test");
        var result = fileManager.getFileFromURLOrBase64(targetFolder.toFile(), Base64.getEncoder().encodeToString("TEST".getBytes(StandardCharsets.UTF_8)), "text/plain", null);
        assertNotNull(result);
        assertEquals(targetFolder, result.getParentFile().toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromURLOrBase64ForFileAndContentAndContentTypeAndArtifactTypeForURL() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "url_or_base64_test");
        when(urlReader.stream(any())).thenReturn(new ByteArrayInputStream("TEST".getBytes(StandardCharsets.UTF_8)));
        var result = fileManager.getFileFromURLOrBase64(targetFolder.toFile(), "http://test.com", "text/plain", null);
        assertNotNull(result);
        assertEquals(targetFolder, result.getParentFile().toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromURLOrBase64ForFileAndContentAndContentTypeAndArtifactTypeForString() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "url_or_base64_test");
        var result = fileManager.getFileFromURLOrBase64(targetFolder.toFile(), " TEST ", "text/plain", null);
        assertNotNull(result);
        assertEquals(targetFolder, result.getParentFile().toPath());
        assertEquals(" TEST ", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromString() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "string_test");
        var result = fileManager.getFileFromString(targetFolder.toFile(), "TEST");
        assertNotNull(result);
        assertEquals(targetFolder, result.getParentFile().toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromStringNoTargetFolder() throws IOException {
//        var targetFolder = Path.of(appConfig.getTmpFolder(), "string_test");
        var result = fileManager.getFileFromString(null, "TEST");
        assertNotNull(result);
        assertEquals(Path.of(appConfig.getTmpFolder(), "web"), result.getParentFile().toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testGetFileFromInputStream() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "string_test");
        var result = fileManager.getFileFromInputStream(targetFolder.toFile(), new ByteArrayInputStream("TEST".getBytes(StandardCharsets.UTF_8)), "text/plain", "aFile.txt");
        assertNotNull(result);
        assertEquals(Path.of(targetFolder.toString(), "aFile.txt"), result.toPath());
        assertEquals("TEST", Files.readString(result.toPath()));
    }

    @Test
    void testCreateTemporaryFolderPath() {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "temp_path");
        var result = fileManager.createTemporaryFolderPath(targetFolder.toFile());
        assertNotNull(result);
        assertEquals(targetFolder, result.getParentFile().toPath());
    }

    @Test
    void testCreateTemporaryFolderPathNullParent() {
        var result = fileManager.createTemporaryFolderPath();
        assertNotNull(result);
        assertEquals(Path.of(appConfig.getTmpFolder(), "web"), result.getParentFile().toPath(), "If no target path is provided the web temp folder should be used.");
    }

    @Test
    void testCreateFile() {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "temp_path");
        var result = fileManager.createFile(targetFolder.toFile());
        assertNotNull(result);
        assertEquals(targetFolder, result.getParent());
    }

    @Test
    void testCreateFileWithExtension() {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "temp_path");
        var result = fileManager.createFile(targetFolder.toFile(), ".txt");
        assertNotNull(result);
        assertEquals(targetFolder, result.getParent());
        assertTrue(result.toFile().getName().endsWith(".txt"));
    }

    @Test
    void testCreateFileWithExtensionAndFileName() {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "temp_path");
        var result = fileManager.createFile(targetFolder.toFile(), ".txt", "aFile");
        assertNotNull(result);
        assertEquals(Path.of(targetFolder.toString(), "aFile.txt"), result);
    }

    private DomainConfig domainConfigForPreprocessingTests() throws IOException {
        DomainConfig domainConfig = new DomainConfig();
        domainConfig.setDomain("domain1");
        domainConfig.setArtifactInfo(new HashMap<>());
        domainConfig.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setPreProcessorPath("path/file.txt");
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setPreProcessorOutputExtension(".txt");
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setExternalArtifactPreProcessorPath("path/file.txt");
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setExternalArtifactPreProcessorOutputExtension(".txt");
        domainConfig.getArtifactInfo().get("type1").add("artifact2", new ValidationArtifactInfo());
        createFileWithContents(Path.of(appConfig.getResourceRoot(), "domain1", "path", "file.txt"), "DATA");
        return domainConfig;
    }

    @Test
    void testPreprocessFileIfNeeded() throws IOException {
        fileManager.preprocessor = new TestPreprocessor();
        var inputFile = Path.of(appConfig.getTmpFolder(), "aFile");
        Files.writeString(inputFile, "TEST");
        var result = fileManager.preprocessFileIfNeeded(domainConfigForPreprocessingTests(), "type1", "artifact1", inputFile.toFile(), false);
        assertNotNull(result);
        assertEquals(Paths.get(inputFile.getParent().toString(), "PROCESSED_aFile.txt"), result.toPath());
    }

    @Test
    void testPreprocessFileIfNeededExternal() throws IOException {
        fileManager.preprocessor = new TestPreprocessor();
        var inputFile = Path.of(appConfig.getTmpFolder(), "aFile");
        Files.writeString(inputFile, "TEST");
        var result = fileManager.preprocessFileIfNeeded(domainConfigForPreprocessingTests(), "type1", "artifact1", inputFile.toFile(), true);
        assertNotNull(result);
        assertEquals(Paths.get(inputFile.getParent().toString(), "PROCESSED_aFile.txt"), result.toPath());
    }

    @Test
    void testPreprocessFileIfNeededNotNeeded() throws IOException {
        fileManager.preprocessor = new TestPreprocessor();
        var inputFile = Path.of(appConfig.getTmpFolder(), "aFile");
        Files.writeString(inputFile, "TEST");
        var result = fileManager.preprocessFileIfNeeded(domainConfigForPreprocessingTests(), "type1", "artifact2", inputFile.toFile(), false);
        assertNotNull(result);
        assertEquals(Paths.get(inputFile.getParent().toString(), "aFile"), result.toPath());
    }

    @Test
    void testGetPreconfiguredValidationArtifacts() throws IOException {
        when(appConfig.isRestrictResourcesToDomain()).thenReturn(true);
        when(domainConfigCache.isInDomainFolder(any(), any())).thenCallRealMethod();
        DomainConfig domainConfig = new DomainConfig();
        domainConfig.setDomain("domain1");
        domainConfig.setDomainName("domain1");
        domainConfig.setArtifactInfo(new HashMap<>());
        domainConfig.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setLocalPath("files");
        createFileWithContents(Path.of(appConfig.getResourceRoot(), "domain1", "files", "file.txt"), "DATA");
        var result = fileManager.getPreconfiguredValidationArtifacts(domainConfig, "type1", "artifact1");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("DATA", Files.readString(result.get(0).getFile().toPath()));
    }

    @Test
    void testGetPreconfiguredValidationArtifactsNonExistentFile() {
        when(appConfig.isRestrictResourcesToDomain()).thenReturn(true);
        when(domainConfigCache.isInDomainFolder(any(), any())).thenCallRealMethod();
        DomainConfig domainConfig = new DomainConfig();
        domainConfig.setDomain("domain1");
        domainConfig.setDomainName("domain1");
        domainConfig.setArtifactInfo(new HashMap<>());
        domainConfig.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setLocalPath("filesX");
        assertThrows(ValidatorException.class, () -> fileManager.getPreconfiguredValidationArtifacts(domainConfig, "type1", "artifact1"));
    }

    @Test
    void testGetPreconfiguredValidationArtifactsFromOtherDomainsAllowed() throws IOException {
        when(appConfig.isRestrictResourcesToDomain()).thenReturn(false);
        when(domainConfigCache.isInDomainFolder(any(), any())).thenCallRealMethod();

        createFileWithContents(Path.of(appConfig.getResourceRoot(), "domain2", "files", "file1.txt"), "DATA1");
        createFileWithContents(Path.of(appConfig.getResourceRoot(), "domain2", "files", "file2.txt"), "DATA2");

        DomainConfig domainConfig = new DomainConfig();
        domainConfig.setDomain("domain1");
        domainConfig.setDomainName("domain1");
        domainConfig.setArtifactInfo(new HashMap<>());
        domainConfig.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setLocalPath("../domain2/files/file1.txt, "+Path.of(appConfig.getResourceRoot(), "domain2/files/file2.txt"));

        var result = fileManager.getPreconfiguredValidationArtifacts(domainConfig, "type1", "artifact1");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("DATA1", Files.readString(result.get(0).getFile().toPath().toRealPath()));
        assertEquals("DATA2", Files.readString(result.get(1).getFile().toPath().toRealPath()));
    }

    @Test
    void testGetPreconfiguredValidationArtifactsFromOtherDomainsNotAllowed() throws IOException {
        when(appConfig.isRestrictResourcesToDomain()).thenReturn(true);
        when(domainConfigCache.isInDomainFolder(any(), any())).thenCallRealMethod();

        DomainConfig domainConfig = new DomainConfig();
        domainConfig.setDomain("domain1");
        domainConfig.setDomainName("domain1");
        domainConfig.setArtifactInfo(new HashMap<>());
        domainConfig.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        // Test relative path.
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setLocalPath("../domain2/files/file1.txt");
        assertThrows(ValidatorException.class, () -> fileManager.getPreconfiguredValidationArtifacts(domainConfig, "type1", "artifact1"));
        // Test absolute path.
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setLocalPath(Path.of(appConfig.getResourceRoot(), "domain2/files/file2.txt").toString());
        assertThrows(ValidatorException.class, () -> fileManager.getPreconfiguredValidationArtifacts(domainConfig, "type1", "artifact1"));
    }

    @Test
    void testGetPreconfiguredValidationArtifactsDefaultType() throws IOException {
        DomainConfig domainConfig = new DomainConfig();
        domainConfig.setDomain("domain1");
        domainConfig.setDomainName("domain1");
        domainConfig.setArtifactInfo(new HashMap<>());
        domainConfig.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").add("default", new ValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").get("default").setLocalPath("files");
        createFileWithContents(Path.of(appConfig.getResourceRoot(), "domain1", "files", "file.txt"), "DATA");
        var result = fileManager.getPreconfiguredValidationArtifacts(domainConfig, "type1");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("DATA", Files.readString(result.get(0).getFile().toPath()));
    }

    @Test
    void testGetPreconfiguredValidationArtifactsWithPreprocessing() throws IOException {
        DomainConfig domainConfig = new DomainConfig();
        domainConfig.setDomain("domain1");
        domainConfig.setDomainName("domain1");
        domainConfig.setArtifactInfo(new HashMap<>());
        domainConfig.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setLocalPath("files");
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setPreProcessorPath("preprocess/aFile.txt");
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setPreProcessorOutputExtension(".txt");
        createFileWithContents(Path.of(appConfig.getResourceRoot(), "domain1", "files", "file.txt"), "DATA");
        createFileWithContents(Path.of(appConfig.getResourceRoot(), "domain1", "preprocess", "aFile.txt"), "CONTENT");
        fileManager.preprocessor = new TestPreprocessor();
        var result = fileManager.getPreconfiguredValidationArtifacts(domainConfig, "type1", "artifact1");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("DATA", Files.readString(result.get(0).getFile().toPath()));
        assertTrue(result.get(0).getFile().getName().startsWith("PROCESSED_"));
        assertTrue(result.get(0).getFile().getName().endsWith(".txt"));
    }

    @Test
    void testGetExternalValidationArtifacts() throws IOException {
        var targetFolder = Path.of(appConfig.getTmpFolder(), "temp_path");
        FileContent file1 = new FileContent();
        file1.setContent("TEST1");
        file1.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        FileContent file2 = new FileContent();
        file2.setContent(Base64.getEncoder().encodeToString("TEST2".getBytes(StandardCharsets.UTF_8)));
        file2.setEmbeddingMethod(ValueEmbeddingEnumeration.BASE_64);
        FileContent file3 = new FileContent();
        file3.setContent("http://test.com");
        file3.setEmbeddingMethod(ValueEmbeddingEnumeration.URI);
        List<FileContent> externalArtifactContents = List.of(file1, file2, file3);
        when(urlReader.stream(any())).thenReturn(new ByteArrayInputStream("TEST3".getBytes(StandardCharsets.UTF_8)));
        var result = fileManager.getExternalValidationArtifacts(new DomainConfig(), "type1", "artifact1", targetFolder.toFile(), externalArtifactContents);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(targetFolder, result.get(0).getFile().getParentFile().getParentFile().toPath());
        assertEquals("TEST1", Files.readString(result.get(0).getFile().toPath()));
        assertEquals(targetFolder, result.get(1).getFile().getParentFile().getParentFile().toPath());
        assertEquals("TEST2", Files.readString(result.get(1).getFile().toPath()));
        assertEquals(targetFolder, result.get(2).getFile().getParentFile().getParentFile().toPath());
        assertEquals("TEST3", Files.readString(result.get(2).getFile().toPath()));
    }

    @Test
    void testRemoveWebFiles() throws IOException {
        when(appConfig.getCleanupWebRate()).thenReturn(-1L);
        var webFolder = Path.of(appConfig.getTmpFolder(), "web");
        var reportsFolder = Path.of(appConfig.getTmpFolder(), "reports");
        Files.createDirectories(webFolder);
        Files.createDirectories(reportsFolder);
        createFileWithContents(Path.of(webFolder.toString(), "file.txt"), "DATA");
        createFileWithContents(Path.of(reportsFolder.toString(), "file.txt"), "DATA");
        fileManager.removeWebFiles();
        assertNotNull(webFolder.toFile().listFiles());
        assertEquals(0, Objects.requireNonNull(webFolder.toFile().listFiles()).length);
        assertNotNull(reportsFolder.toFile().listFiles());
        assertEquals(0, Objects.requireNonNull(reportsFolder.toFile().listFiles()).length);
    }

    @Test
    void testGetReportFolder() {
        assertEquals(Path.of(appConfig.getTmpFolder(), "reports"), fileManager.getReportFolder().toPath());
    }

    @Test
    void testResetRemoteFileCache() throws IOException {
        var config1 = new DomainConfig();
        config1.setDomain("domain1");
        config1.setDomainName("domain1");
        config1.setType(List.of("type1"));
        config1.setArtifactInfo(new HashMap<>());
        config1.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        config1.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        config1.getArtifactInfo().get("type1").get("artifact1").setRemoteArtifacts(new ArrayList<>());
        config1.getArtifactInfo().get("type1").get("artifact1").getRemoteArtifacts().add(new RemoteValidationArtifactInfo());
        config1.getArtifactInfo().get("type1").get("artifact1").getRemoteArtifacts().get(0).setUrl("http://test1.com");
        config1.getArtifactInfo().get("type1").add("artifact2", new ValidationArtifactInfo());
        config1.getArtifactInfo().get("type1").get("artifact2").setRemoteArtifacts(new ArrayList<>());
        config1.getArtifactInfo().get("type1").get("artifact2").getRemoteArtifacts().add(new RemoteValidationArtifactInfo());
        config1.getArtifactInfo().get("type1").get("artifact2").getRemoteArtifacts().get(0).setUrl("http://test2.com");
        config1.getArtifactInfo().get("type1").get("artifact2").getRemoteArtifacts().get(0).setPreProcessorPath("preprocess/aFile.txt");
        config1.getArtifactInfo().get("type1").get("artifact2").getRemoteArtifacts().get(0).setPreProcessorOutputExtension(".txt");
        createFileWithContents(Path.of(appConfig.getResourceRoot(), "domain1", "preprocess", "aFile.txt"), "CONTENT");
        fileManager.preprocessor = new TestPreprocessor();
        when(urlReader.stream(URI.create("http://test1.com"))).thenReturn(new ByteArrayInputStream("TEST1".getBytes(StandardCharsets.UTF_8)));
        when(urlReader.stream(URI.create("http://test2.com"))).thenReturn(new ByteArrayInputStream("TEST2".getBytes(StandardCharsets.UTF_8)));
        when(domainConfigCache.getAllDomainConfigurations()).thenReturn(List.of(config1));
        fileManager.resetRemoteFileCache();
        assertTrue(Files.exists(Path.of(appConfig.getTmpFolder(), "remote_config", "domain1", "type1", "artifact1")));
        assertEquals(1, Objects.requireNonNull(Path.of(appConfig.getTmpFolder(), "remote_config", "domain1", "type1", "artifact1").toFile().listFiles()).length);
        assertEquals("TEST1", Files.readString(Objects.requireNonNull(Path.of(appConfig.getTmpFolder(), "remote_config", "domain1", "type1", "artifact1").toFile().listFiles())[0].toPath()));
        assertTrue(Files.exists(Path.of(appConfig.getTmpFolder(), "remote_config", "domain1", "type1", "artifact2")));
        assertEquals(1, Objects.requireNonNull(Path.of(appConfig.getTmpFolder(), "remote_config", "domain1", "type1", "artifact2").toFile().listFiles()).length);
        assertEquals("TEST2", Files.readString(Objects.requireNonNull(Path.of(appConfig.getTmpFolder(), "remote_config", "domain1", "type1", "artifact2").toFile().listFiles())[0].toPath()));
        assertTrue(Objects.requireNonNull(Path.of(appConfig.getTmpFolder(), "remote_config", "domain1", "type1", "artifact2").toFile().listFiles())[0].getName().startsWith("PROCESSED_"));
        assertTrue(Objects.requireNonNull(Path.of(appConfig.getTmpFolder(), "remote_config", "domain1", "type1", "artifact2").toFile().listFiles())[0].getName().endsWith(".txt"));
    }

    private BAR createBAR(int index) {
        var bar = new BAR();
        bar.setAssertionID("ID_"+index);
        return bar;
    }

    private TAR prepareTestReport() throws DatatypeConfigurationException {
        ObjectFactory objectFactory = new ObjectFactory();
        var report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        report.setCounters(new ValidationCounters());
        report.getCounters().setNrOfAssertions(BigInteger.ONE);
        report.getCounters().setNrOfErrors(BigInteger.ZERO);
        report.getCounters().setNrOfWarnings(BigInteger.ZERO);
        report.setReports(new TestAssertionGroupReportsType());
        report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeInfo(createBAR(0)));
        report.setDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        return report;
    }

    @Test
    void testSaveReport() throws DatatypeConfigurationException {
        DomainConfig domainConfig = new DomainConfig();
        domainConfig.setMaximumReportsForXmlOutput(1000L);
        fileManager.saveReport(prepareTestReport(), Path.of(appConfig.getTmpFolder(), "report.xml").toFile(), domainConfig);
        assertTrue(Files.exists(Path.of(appConfig.getTmpFolder(), "report.xml")));
    }

    @Test
    void testSaveReportWithUUID() throws DatatypeConfigurationException {
        DomainConfig domainConfig = new DomainConfig();
        domainConfig.setMaximumReportsForXmlOutput(1000L);
        fileManager.saveReport(prepareTestReport(), "XYZ", domainConfig);
        assertTrue(Files.exists(Path.of(appConfig.getTmpFolder(), "reports", "TAR-XYZ.xml")));
    }

    @Test
    void testValidationLockSignals() {
        assertEquals(0, fileManager.getExternalDomainFileCacheLock("domain1").getReadLockCount());
        fileManager.signalValidationStart("domain1");
        assertEquals(1, fileManager.getExternalDomainFileCacheLock("domain1").getReadLockCount());
        fileManager.signalValidationEnd("domain1");
        assertEquals(0, fileManager.getExternalDomainFileCacheLock("domain1").getReadLockCount());
    }

    @Test
    void testInit() throws IOException {
        var existingFile = createFileWithContents(Path.of(tmpFolder.toString(), "existingFile.txt"), "DATA");
        var fileManager = mock(BaseFileManager.class);
        fileManager.domainConfigCache = domainConfigCache;
        doReturn(tmpFolder.toFile()).when(fileManager).getTempFolder();
        doCallRealMethod().when(fileManager).init();
        fileManager.init();
        assertTrue(Files.notExists(existingFile));
        verify(fileManager, times(1)).resetRemoteFileCache();
    }

    static class TestPreprocessor implements ArtifactPreprocessor {

        File fileToProcess;
        File preProcessorFile;
        String outputFileExtension;

        @Override
        public File preprocessFile(File fileToProcess, File preProcessorFile, String outputFileExtension) {
            this.fileToProcess = fileToProcess;
            this.preProcessorFile = preProcessorFile;
            this.outputFileExtension = outputFileExtension;
            var finalFile = Path.of(fileToProcess.getParent(), "PROCESSED_"+fileToProcess.getName()+ StringUtils.defaultString(outputFileExtension));
            try {
                Files.writeString(finalFile, Files.readString(fileToProcess.toPath()));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return finalFile.toFile();
        }
    }

}
