package eu.europa.ec.itb.validation.commons;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.vs.ValidateRequest;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.test.BaseSpringTest;
import eu.europa.ec.itb.validation.commons.test.BaseTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { BaseInputHelperTest.TestConfig.class, BaseTestConfiguration.class })
class BaseInputHelperTest extends BaseSpringTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public BaseInputHelper<ApplicationConfig, BaseFileManager<ApplicationConfig>, DomainConfig> testInputHelper() {
            return new BaseInputHelper<>() {};
        }
        @Bean
        public BaseFileManager<ApplicationConfig> fileManager() {
            return mock(BaseFileManager.class);
        }
        @Bean
        public DomainConfigCache<DomainConfig> domainConfigCache() {
            return mock(DomainConfigCache.class);
        }
    }

    @Autowired
    private BaseInputHelper<ApplicationConfig, BaseFileManager<ApplicationConfig>, DomainConfig> inputHelper;
    @Autowired
    private BaseFileManager<ApplicationConfig> fileManager;
    @Autowired
    private DomainConfigCache<DomainConfig> domainConfigCache;

    @BeforeEach
    @Override
    protected void setup() throws IOException {
        super.setup();
    }

    @AfterEach
    @Override
    protected void teardown() {
        super.teardown();
        reset(fileManager);
    }

    @Test
    void testValidateContentEmbeddingMethod() {
        var request = new ValidateRequest();
        request.getInput().add(new AnyContent());
        request.getInput().get(0).setName("input1");
        request.getInput().get(0).setValue(ValueEmbeddingEnumeration.STRING.value());
        var result = inputHelper.validateContentEmbeddingMethod(request, "input1");
        assertEquals(ValueEmbeddingEnumeration.STRING, result);
    }

    @Test
    void testValidateContentEmbeddingMethodNotFound() {
        var request = new ValidateRequest();
        request.getInput().add(new AnyContent());
        request.getInput().get(0).setName("input1");
        request.getInput().get(0).setValue(ValueEmbeddingEnumeration.STRING.value());
        var result = inputHelper.validateContentEmbeddingMethod(request, "inputX");
        assertNull(result);
    }

    @Test
    void testValidateContentToValidate() {
        var request = new ValidateRequest();
        request.getInput().add(new AnyContent());
        request.getInput().get(0).setName("input1");
        request.getInput().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        request.getInput().get(0).setValue("CONTENT");
        when(fileManager.storeFileContent(any(), any(), any(), any())).thenAnswer((Answer<File>) invocationOnMock -> {
            assertEquals("CONTENT", invocationOnMock.getArgument(1));
            return createFileWithContents(Path.of(appConfig.getTmpFolder(), "file"), "CONTENT").toFile();
        });
        inputHelper.validateContentToValidate(request, "input1", null, null, Path.of(appConfig.getTmpFolder()).toFile(), HttpClient.Version.HTTP_2);
        verify(fileManager, times(1)).storeFileContent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testValidateContentToValidateWithBase64AsString() {
        var request = new ValidateRequest();
        request.getInput().add(new AnyContent());
        request.getInput().get(0).setName("input1");
        request.getInput().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.BASE_64);
        request.getInput().get(0).setValue(Base64.getEncoder().encodeToString("CONTENT".getBytes(StandardCharsets.UTF_8)));
        when(fileManager.storeFileContent(any(), any(), any(), any())).thenAnswer((Answer<File>) invocationOnMock -> {
            assertEquals("CONTENT", invocationOnMock.getArgument(1));
            return createFileWithContents(Path.of(appConfig.getTmpFolder(), "file"), "CONTENT").toFile();
        });
        inputHelper.validateContentToValidate(request, "input1", ValueEmbeddingEnumeration.STRING, null, Path.of(appConfig.getTmpFolder()).toFile(), HttpClient.Version.HTTP_2);
        verify(fileManager, times(1)).storeFileContent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testValidateContentToValidateWithExplicitBase64() {
        var request = new ValidateRequest();
        request.getInput().add(new AnyContent());
        request.getInput().get(0).setName("input1");
        request.getInput().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        request.getInput().get(0).setValue(Base64.getEncoder().encodeToString("CONTENT".getBytes(StandardCharsets.UTF_8)));
        when(fileManager.storeFileContent(any(), any(), any(), any())).thenAnswer((Answer<File>) invocationOnMock -> {
            assertEquals(Base64.getEncoder().encodeToString("CONTENT".getBytes(StandardCharsets.UTF_8)), invocationOnMock.getArgument(1));
            return createFileWithContents(Path.of(appConfig.getTmpFolder(), "file"), "CONTENT").toFile();
        });
        inputHelper.validateContentToValidate(request, "input1", ValueEmbeddingEnumeration.BASE_64, null, Path.of(appConfig.getTmpFolder()).toFile(), HttpClient.Version.HTTP_2);
        verify(fileManager, times(1)).storeFileContent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testValidateContentToValidateWithNoMatch() {
        var request = new ValidateRequest();
        request.getInput().add(new AnyContent());
        request.getInput().get(0).setName("input1");
        request.getInput().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        request.getInput().get(0).setValue("CONTENT");
        assertThrows(ValidatorException.class, () -> inputHelper.validateContentToValidate(request, "inputX", null, null, Path.of(appConfig.getTmpFolder()).toFile(), HttpClient.Version.HTTP_2));
    }

    @Test
    void testValidateValidationTypeForValidateRequest() {
        var domainConfig = new DomainConfig();
        domainConfig.setDomainName("test");
        domainConfig.setType(List.of("type1"));
        var request = new ValidateRequest();
        request.getInput().add(new AnyContent());
        request.getInput().get(0).setName("type");
        request.getInput().get(0).setValue("type1");
        request.getInput().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        var result = inputHelper.validateValidationType(domainConfig.getDomainName(), domainConfig, request, "type");
        assertEquals("type1", result);
    }

    @Test
    void testValidateValidationTypeForValidateRequestWrongEmbeddingMethod() {
        var domainConfig = new DomainConfig();
        domainConfig.setDomainName("test");
        domainConfig.setType(List.of("type1"));
        var request = new ValidateRequest();
        request.getInput().add(new AnyContent());
        request.getInput().get(0).setName("type");
        request.getInput().get(0).setValue("type1");
        request.getInput().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.BASE_64);
        assertThrows(ValidatorException.class, () -> inputHelper.validateValidationType(domainConfig.getDomainName(), domainConfig, request, "type"));
    }

    @Test
    void testValidateValidationTypeForValidateRequestNoMatchNok() {
        var domainConfig = new DomainConfig();
        domainConfig.setDomainName("test");
        domainConfig.setType(List.of("type1", "type2"));
        assertThrows(ValidatorException.class, () -> inputHelper.validateValidationType(domainConfig.getDomainName(), domainConfig, new ValidateRequest(), "input"));
    }

    @Test
    void testValidateValidationType() {
        var domainConfig = new DomainConfig();
        domainConfig.setType(List.of("type1"));
        assertEquals("type1", inputHelper.validateValidationType(domainConfig, "type1"));
    }

    @Test
    void testValidateValidationTypeAsDefaultOk() {
        var domainConfig = new DomainConfig();
        domainConfig.setType(List.of("type1"));
        domainConfig.setDefaultType("type1");
        assertEquals("type1", inputHelper.validateValidationType(domainConfig, null));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"typeX", "test1"})
    void testValidateValidationTypeMultipleTypesAndMissingDefault(String validationType) {
        var domainConfig = new DomainConfig();
        domainConfig.setType(List.of("type1", "type2"));
        assertThrows(ValidatorException.class, () -> inputHelper.validateValidationType(domainConfig, validationType));
    }

    @ParameterizedTest
    @MethodSource(value = "validationTypes")
    void testValidateValidationTypeDefaultWithSeveralTypesAndDefault(String defaultType, String assertType, String providedType) {
        var domainConfig = new DomainConfig();
        domainConfig.setType(List.of("type1", "type2"));
        domainConfig.setDefaultType(defaultType);
        assertEquals(assertType, inputHelper.validateValidationType(domainConfig, providedType));
    }

    private static Stream<Arguments> validationTypes() {
        return Stream.of(
                Arguments.arguments("type1", "type1", "type1"),
                Arguments.arguments("type2", "type1", "type1"),
                Arguments.arguments("type2", "type2", null));
    }

    @Test
    void testValidateValidationTypeWithSeveralTypesAndDefaultNotFound() {
        var domainConfig = new DomainConfig();
        domainConfig.setType(List.of("type1", "type2"));
        domainConfig.setDefaultType("type2");
        assertThrows(ValidatorException.class, () -> inputHelper.validateValidationType(domainConfig, "typeX"));
    }

    @Test
    void testToExternalArtifactContents() {
        AnyContent content = new AnyContent();
        content.getItem().add(new AnyContent());
        content.getItem().get(0).setName("input1");
        content.getItem().add(new AnyContent());
        content.getItem().get(1).setName("content");
        content.getItem().get(1).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        content.getItem().get(1).setValue("VALUE");
        content.getItem().add(new AnyContent());
        content.getItem().get(2).setName("method");
        content.getItem().get(2).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        content.getItem().get(2).setValue("STRING");
        var result = inputHelper.toExternalArtifactContents(content, "content", "method");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("VALUE", result.get(0).getContent());
    }

    @Test
    void testToExternalArtifactContentsBase64AsString() {
        AnyContent content = new AnyContent();
        content.getItem().add(new AnyContent());
        content.getItem().get(0).setName("input1");
        content.getItem().add(new AnyContent());
        content.getItem().get(1).setName("content");
        content.getItem().get(1).setEmbeddingMethod(ValueEmbeddingEnumeration.BASE_64);
        content.getItem().get(1).setValue(Base64.getEncoder().encodeToString("VALUE".getBytes(StandardCharsets.UTF_8)));
        content.getItem().add(new AnyContent());
        content.getItem().get(2).setName("method");
        content.getItem().get(2).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        content.getItem().get(2).setValue("STRING");
        var result = inputHelper.toExternalArtifactContents(content, "content", "method");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("VALUE", result.get(0).getContent());
    }

    @Test
    void testToExternalArtifactContentsWithChildren() {
        AnyContent content = new AnyContent();
        content.getItem().add(new AnyContent());
        content.getItem().get(0).setName("input1");
        content.getItem().add(new AnyContent());
        content.getItem().get(1).setName("content");
        content.getItem().get(1).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        content.getItem().get(1).setValue("VALUE");
        content.getItem().add(new AnyContent());
        content.getItem().get(2).setName("method");
        content.getItem().get(2).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        content.getItem().get(2).setValue("STRING");
        AnyContent parent = new AnyContent();
        parent.getItem().add(content);
        var result = inputHelper.toExternalArtifactContents(parent, "content", "method");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("VALUE", result.get(0).getContent());
    }

    @Test
    void testToExternalArtifactContentsAsValidateRequest() {
        AnyContent content = new AnyContent();
        content.setName("container");
        content.getItem().add(new AnyContent());
        content.getItem().get(0).setName("input1");
        content.getItem().add(new AnyContent());
        content.getItem().get(1).setName("content");
        content.getItem().get(1).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        content.getItem().get(1).setValue("VALUE");
        content.getItem().add(new AnyContent());
        content.getItem().get(2).setName("method");
        content.getItem().get(2).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        content.getItem().get(2).setValue("STRING");
        ValidateRequest request = new ValidateRequest();
        request.getInput().add(content);
        var result = inputHelper.toExternalArtifactContents(request, "container", "content", "method");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("VALUE", result.get(0).getContent());
    }

    @Test
    void testToExternalArtifactContentsAsValidateRequestEmpty() {
        var result = inputHelper.toExternalArtifactContents(new ValidateRequest(), "container", "content", "method");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetExternalArtifactInfo() {
        AnyContent content = new AnyContent();
        content.setName("container");
        content.getItem().add(new AnyContent());
        content.getItem().get(0).setName("input1");
        content.getItem().add(new AnyContent());
        content.getItem().get(1).setName("content");
        content.getItem().get(1).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        content.getItem().get(1).setValue("VALUE");
        content.getItem().add(new AnyContent());
        content.getItem().get(2).setName("method");
        content.getItem().get(2).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        content.getItem().get(2).setValue("STRING");
        when(fileManager.getExternalValidationArtifacts(any(), any(), any(), any(), any(), any())).thenAnswer((Answer<List<FileContent>>) invocationOnMock -> {
            List<FileContent> fileContents = invocationOnMock.getArgument(4);
            assertNotNull(fileContents);
            assertEquals(1, fileContents.size());
            assertEquals("VALUE", fileContents.get(0).getContent());
            return Collections.emptyList();
        });
        inputHelper.getExternalArtifactInfo(content, new DomainConfig(), "type1", "artifact1", "content", "method", Path.of(appConfig.getTmpFolder()).toFile());
        verify(fileManager, times(1)).getExternalValidationArtifacts(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testValidateExternalArtifactsNoneNoArtifacts() {
        var domainConfig = new DomainConfig();
        domainConfig.setArtifactInfo(new HashMap<>());
        domainConfig.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setExternalArtifactSupport(ExternalArtifactSupport.NONE);
        var externalArtifacts = new ArrayList<FileContent>();
        var result = inputHelper.validateExternalArtifacts(domainConfig, externalArtifacts, "type1", "artifact1", Path.of(appConfig.getTmpFolder()).toFile());
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testValidateExternalArtifactsOptionalNoArtifacts() {
        var domainConfig = new DomainConfig();
        domainConfig.setArtifactInfo(new HashMap<>());
        domainConfig.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setExternalArtifactSupport(ExternalArtifactSupport.OPTIONAL);
        var externalArtifacts = new ArrayList<FileContent>();
        var result = inputHelper.validateExternalArtifacts(domainConfig, externalArtifacts, "type1", "artifact1", Path.of(appConfig.getTmpFolder()).toFile());
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testValidateExternalArtifactsRequiredNoArtifacts() {
        var domainConfig = new DomainConfig();
        domainConfig.setArtifactInfo(new HashMap<>());
        domainConfig.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setExternalArtifactSupport(ExternalArtifactSupport.REQUIRED);
        var externalArtifacts = new ArrayList<FileContent>();
        assertThrows(ValidatorException.class, () -> inputHelper.validateExternalArtifacts(domainConfig, externalArtifacts, "type1", "artifact1", Path.of(appConfig.getTmpFolder()).toFile()));
    }

    @Test
    void testValidateExternalArtifactsNoneWithArtifacts() {
        var domainConfig = new DomainConfig();
        domainConfig.setArtifactInfo(new HashMap<>());
        domainConfig.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setExternalArtifactSupport(ExternalArtifactSupport.NONE);
        var externalArtifacts = new ArrayList<FileContent>();
        externalArtifacts.add(new FileContent());
        externalArtifacts.get(0).setContent("CONTENT");
        externalArtifacts.get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        assertThrows(ValidatorException.class, () -> inputHelper.validateExternalArtifacts(domainConfig, externalArtifacts, "type1", "artifact1", Path.of(appConfig.getTmpFolder()).toFile()));
    }

    @Test
    void testValidateExternalArtifactsOptionalWithArtifacts() {
        var domainConfig = new DomainConfig();
        domainConfig.setArtifactInfo(new HashMap<>());
        domainConfig.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setExternalArtifactSupport(ExternalArtifactSupport.OPTIONAL);
        var externalArtifacts = new ArrayList<FileContent>();
        externalArtifacts.add(new FileContent());
        externalArtifacts.get(0).setContent("CONTENT");
        externalArtifacts.get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        when(fileManager.getExternalValidationArtifacts(any(), any(), any(), any(), any(), any())).thenAnswer((Answer<List<FileInfo>>) invocationOnMock -> Collections.emptyList());
        inputHelper.validateExternalArtifacts(domainConfig, externalArtifacts, "type1", "artifact1", Path.of(appConfig.getTmpFolder()).toFile());
        verify(fileManager, times(1)).getExternalValidationArtifacts(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testValidateExternalArtifactsRequiredWithArtifacts() {
        var domainConfig = new DomainConfig();
        domainConfig.setArtifactInfo(new HashMap<>());
        domainConfig.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setExternalArtifactSupport(ExternalArtifactSupport.REQUIRED);
        var externalArtifacts = new ArrayList<FileContent>();
        externalArtifacts.add(new FileContent());
        externalArtifacts.get(0).setContent("CONTENT");
        externalArtifacts.get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        when(fileManager.getExternalValidationArtifacts(any(), any(), any(), any(), any(), any())).thenAnswer((Answer<List<FileInfo>>) invocationOnMock -> Collections.emptyList());
        inputHelper.validateExternalArtifacts(domainConfig, externalArtifacts, "type1", "artifact1", Path.of(appConfig.getTmpFolder()).toFile());
        verify(fileManager, times(1)).getExternalValidationArtifacts(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testValidateExternalArtifactsForValidateRequest() {
        var domainConfig = new DomainConfig();
        domainConfig.setArtifactInfo(new HashMap<>());
        domainConfig.getArtifactInfo().put("type1", new TypedValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").add("artifact1", new ValidationArtifactInfo());
        domainConfig.getArtifactInfo().get("type1").get("artifact1").setExternalArtifactSupport(ExternalArtifactSupport.OPTIONAL);
        AnyContent content = new AnyContent();
        content.setName("container");
        content.getItem().add(new AnyContent());
        content.getItem().get(0).setName("input1");
        content.getItem().add(new AnyContent());
        content.getItem().get(1).setName("content");
        content.getItem().get(1).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        content.getItem().get(1).setValue("VALUE");
        content.getItem().add(new AnyContent());
        content.getItem().get(2).setName("method");
        content.getItem().get(2).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        content.getItem().get(2).setValue("STRING");
        ValidateRequest request = new ValidateRequest();
        request.getInput().add(content);
        when(fileManager.getExternalValidationArtifacts(any(), any(), any(), any(), any(), any())).thenAnswer((Answer<List<FileInfo>>) invocationOnMock -> Collections.emptyList());
        inputHelper.validateExternalArtifacts(domainConfig, request, "container", "content", "method", "type1", "artifact1", Path.of(appConfig.getTmpFolder()).toFile());
        verify(fileManager, times(1)).getExternalValidationArtifacts(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testGetEmbeddingMethod() {
        assertNull(inputHelper.getEmbeddingMethod(null));
        assertNull(inputHelper.getEmbeddingMethod(""));
        assertNull(inputHelper.getEmbeddingMethod(" "));
        assertEquals(ValueEmbeddingEnumeration.STRING, inputHelper.getEmbeddingMethod("STRING"));
    }

    @Test
    void testSupportsExternalArtifacts() {
        Map<String, TypedValidationArtifactInfo> map = new HashMap<>();
        map.put("type1", new TypedValidationArtifactInfo());
        map.get("type1").add("artifact1", new ValidationArtifactInfo());
        map.get("type1").get("artifact1").setExternalArtifactSupport(ExternalArtifactSupport.NONE);
        assertFalse(inputHelper.supportsExternalArtifacts(map, "artifact1"));
        map.get("type1").get("artifact1").setExternalArtifactSupport(ExternalArtifactSupport.OPTIONAL);
        assertTrue(inputHelper.supportsExternalArtifacts(map, "artifact1"));
        map.get("type1").get("artifact1").setExternalArtifactSupport(ExternalArtifactSupport.REQUIRED);
        assertTrue(inputHelper.supportsExternalArtifacts(map, "artifact1"));
        map.clear();
        assertFalse(inputHelper.supportsExternalArtifacts(map, "artifact1"));
    }

    @Test
    void testDetermineValidationType() {
        DomainConfig domain1 = new DomainConfig();
        domain1.setDomainName("domain1");
        domain1.setType(List.of("type1"));
        DomainConfig domain2 = new DomainConfig();
        domain2.setDomainName("domain2");
        domain2.setDomainAlias("domain1");
        domain2.setDefaultType("alias1");
        domain2.setDomainTypeAlias(Map.of("alias1", "type1"));
        domain2.setType(List.of("alias1"));

        when(domainConfigCache.getConfigForDomainName("domain1", true, false)).thenReturn(domain1);
        when(domainConfigCache.getConfigForDomainName("domain2", true, false)).thenReturn(domain2);

        assertEquals("type1", inputHelper.determineValidationType("type1", "domain1", domain1));
        assertEquals("type1", inputHelper.determineValidationType("alias1", "domain2", domain1));
        assertEquals("type1", inputHelper.determineValidationType("type1", "domain2", domain1));
        assertEquals("type1", inputHelper.determineValidationType(null, "domain2", domain1));

        domain2.setDomainTypeAlias(null);
        assertEquals("xxx", inputHelper.determineValidationType("xxx", "domain2", domain1));
        domain2.setDefaultType(null);
        assertThrows(ValidatorException.class, () -> inputHelper.determineValidationType(null, "domain2", domain1));
    }
}
