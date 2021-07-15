package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.RemoteValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactCombinationApproach;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class DomainConfigCacheSupportMethodTest {

    private DomainConfigCache<DomainConfig> createDomainConfigCache() {
        return new DomainConfigCache<>() {
            @Override
            protected DomainConfig newDomainConfig() {
                return new DomainConfig();
            }

            @Override
            protected ValidatorChannel[] getSupportedChannels() {
                return new ValidatorChannel[]{ValidatorChannel.FORM, ValidatorChannel.REST_API, ValidatorChannel.SOAP_API, ValidatorChannel.EMAIL};
            }
        };
    }
    private DomainConfigCache<DomainConfig> cache;

    @BeforeEach
    void setup() {
        cache = createDomainConfigCache();
    }

    @Test
    void toValidatorChannel() {
        assertEquals(ValidatorChannel.REST_API, cache.toValidatorChannel(Set.of(ValidatorChannel.FORM, ValidatorChannel.REST_API), "rest_api"));
        assertThrows(IllegalArgumentException.class, () -> cache.toValidatorChannel(Set.of(ValidatorChannel.FORM, ValidatorChannel.REST_API), "bad"));
        assertThrows(IllegalStateException.class, () -> cache.toValidatorChannel(Set.of(ValidatorChannel.FORM, ValidatorChannel.REST_API), "soap_api"));
    }

    static class DataHolder {
        String v1;
        List<Integer> v2;
        DataHolder(String v1, List<Integer> v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
    }

    enum TestEnum {
        VALUE1, VALUE2, VALUE3
    }

    @Test
    void testParseValueList() {
        var config = new MapConfiguration(Map.of(
                "key.0.subKey1", "A",
                "key.0.subKey2", "1,2",
                "key.1.subKey1", "B",
                "key.1.subKey2", "3,4"
        ));
        var result = cache.parseValueList("key", config, (values) -> new DataHolder(values.get("subKey1"), Arrays.stream(values.get("subKey2").split(",")).map(Integer::valueOf).collect(Collectors.toList())));
        assertEquals(2, result.size());
        result.sort(Comparator.comparing(v -> v.v1));
        assertEquals("A", result.get(0).v1);
        assertEquals(2, result.get(0).v2.size());
        assertEquals(1, result.get(0).v2.get(0));
        assertEquals(2, result.get(0).v2.get(1));
        assertEquals("B", result.get(1).v1);
        assertEquals(2, result.get(1).v2.size());
        assertEquals(3, result.get(1).v2.get(0));
        assertEquals(4, result.get(1).v2.get(1));
    }

    @Test
    void parseTypedValueList() {
        var config = new MapConfiguration(Map.of(
                "key.type1.0.subKey1", "A",
                "key.type1.0.subKey2", "1,2",
                "key.type2.0.subKey1", "B",
                "key.type2.0.subKey2", "3,4"
        ));
        var result = cache.parseTypedValueList("key", List.of("type1", "type2"), config, (values) -> new DataHolder(values.get("subKey1"), Arrays.stream(values.get("subKey2").split(",")).map(Integer::valueOf).collect(Collectors.toList())));
        assertEquals(2, result.size());
        assertEquals(1, result.get("type1").size());
        assertEquals("A", result.get("type1").get(0).v1);
        assertEquals(2, result.get("type1").get(0).v2.size());
        assertEquals(1, result.get("type1").get(0).v2.get(0));
        assertEquals(2, result.get("type1").get(0).v2.get(1));
        assertEquals(1, result.get("type2").size());
        assertEquals("B", result.get("type2").get(0).v1);
        assertEquals(2, result.get("type2").get(0).v2.size());
        assertEquals(3, result.get("type2").get(0).v2.get(0));
        assertEquals(4, result.get("type2").get(0).v2.get(1));
    }

    @Test
    void testParseBooleanMap() {
        var config = new MapConfiguration(Map.of(
                "key.type1", "true",
                "key.type2", "false",
                "key.type4", "bad"
        ));
        var result = cache.parseBooleanMap("key", config, List.of("type1", "type2", "type3", "type4"));
        assertEquals(4, result.size());
        assertTrue(result.get("type1"));
        assertFalse(result.get("type2"));
        assertFalse(result.get("type3"));
        assertFalse(result.get("type4"));
        result = cache.parseBooleanMap("key", config, List.of("type1", "type2", "type3", "type4"), true);
        assertEquals(4, result.size());
        assertTrue(result.get("type1"));
        assertFalse(result.get("type2"));
        assertTrue(result.get("type3"));
        assertTrue(result.get("type4"));
    }

    @Test
    void testParseCharacterMap() {
        var config = new MapConfiguration(Map.of(
                "key.type1", "A",
                "key.type2", "B"
        ));
        var result = cache.parseCharacterMap("key", config, List.of("type1", "type2", "type3"), 'X');
        assertEquals(3, result.size());
        assertEquals('A', result.get("type1"));
        assertEquals('B', result.get("type2"));
        assertEquals('X', result.get("type3"));
    }

    @Test
    void testParseEnumMap() {
        var config = new MapConfiguration(Map.of(
                "key.type1", "VALUE1",
                "key.type2", "VALUE2"
        ));
        var result = cache.parseEnumMap("key", TestEnum.class, TestEnum.VALUE3, config, List.of("type1", "type2", "type3"));
        assertEquals(3, result.size());
        assertEquals(TestEnum.VALUE1, result.get("type1"));
        assertEquals(TestEnum.VALUE2, result.get("type2"));
        assertEquals(TestEnum.VALUE3, result.get("type3"));
        result = cache.parseEnumMap("key", TestEnum.VALUE3, config, List.of("type1", "type2", "type3"), TestEnum::valueOf);
        assertEquals(3, result.size());
        assertEquals(TestEnum.VALUE1, result.get("type1"));
        assertEquals(TestEnum.VALUE2, result.get("type2"));
        assertEquals(TestEnum.VALUE3, result.get("type3"));
    }

    @Test
    void testParseObjectMap() {
        var config = new MapConfiguration(Map.of(
                "key.commonPart.key1.subKey1", "value1",
                "key.commonPart.key1.subKey2", "value2",
                "key.commonPart.key2.subKey1", "value3",
                "key.commonPart.key2.subKey2", "value4"
        ));
        var result = cache.parseObjectMap("key.commonPart", config, (key, parts) -> new String[] { parts.get("subKey1"), parts.get("subKey2") });
        assertEquals(2, result.size());
        assertNotNull(result.get("key1"));
        assertEquals(2, result.get("key1").length);
        assertEquals("value1", result.get("key1")[0]);
        assertEquals("value2", result.get("key1")[1]);
        assertNotNull(result.get("key2"));
        assertEquals(2, result.get("key2").length);
        assertEquals("value3", result.get("key2")[0]);
        assertEquals("value4", result.get("key2")[1]);
    }

    @Test
    void testParseMap() {
        var config = new MapConfiguration(Map.of(
                "key.commonPart.key1", "value1",
                "key.commonPart.key2", "value2"
        ));
        var result = cache.parseMap("key.commonPart", config);
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
        result = cache.parseMap("key.commonPart", config, List.of("key1", "key2"));
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
        result = cache.parseMap("key.commonPart", config, Map.of("key3", "value3"));
        assertEquals(3, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
        assertEquals("value3", result.get("key3"));
    }

    @Test
    void testAddValidationArtifactInfoForType() {
        var domainConfig = new DomainConfig();
        domainConfig.setType(List.of("type1", "type2"));
        var config = new MapConfiguration(Map.ofEntries(
                // Local artefacts
                Map.entry("key.commonPart.type1", "localPath1"),
                Map.entry("key.commonPart.type1.type", "artifact1"),
                Map.entry("key.commonPart.type1.preprocessor", "path1"),
                Map.entry("key.commonPart.type1.preprocessor.output", "ext1"),
                Map.entry("key.commonPart.type1.combine", "all"),
                // Remote artefacts
                Map.entry("key.commonPart.type1.remote.0.url", "http://url1.com"),
                Map.entry("key.commonPart.type1.remote.0.type", "artifact1"),
                Map.entry("key.commonPart.type1.remote.0.preprocessor", "path2"),
                Map.entry("key.commonPart.type1.remote.0.preprocessor.output", "ext2"),
                Map.entry("key.commonPart.type1.remote.1.url", "http://url2.com"),
                Map.entry("key.commonPart.type1.remote.1.type", "artifact1"),
                // User-provided artefacts
                Map.entry("externalSupport.type1", "optional"),
                Map.entry("externalSupport.type1.combine", "all"),
                Map.entry("externalSupport.type1.preprocessor", "http://url3.com"),
                Map.entry("externalSupport.type1.preprocessor.output", "ext3"),
                Map.entry("key.commonPart.type2", "localPath2"),
                Map.entry("key.commonPart.type2.combinationApproach", "anyOf")
        ));
        cache.addValidationArtifactInfoForType("artifact1", "key.commonPart", "externalSupport", "combine", domainConfig, config);

        assertEquals(2, domainConfig.getArtifactInfo().size());
        assertTrue(domainConfig.getArtifactInfo().get("type1").hasPreconfiguredArtifacts());
        assertEquals(1, domainConfig.getArtifactInfo().get("type1").getTypes().size());
        assertEquals("artifact1", domainConfig.getArtifactInfo().get("type1").getTypes().iterator().next());
        // Local artefacts
        assertEquals("artifact1", domainConfig.getArtifactInfo().get("type1").get("artifact1").getType());
        assertEquals("localPath1", domainConfig.getArtifactInfo().get("type1").get("artifact1").getLocalPath());
        assertEquals("path1", domainConfig.getArtifactInfo().get("type1").get("artifact1").getPreProcessorPath());
        assertEquals("ext1", domainConfig.getArtifactInfo().get("type1").get("artifact1").getPreProcessorOutputExtension());
        assertEquals(ValidationArtifactCombinationApproach.ALL, domainConfig.getArtifactInfo().get("type1").get("artifact1").getArtifactCombinationApproach());
        // Remote artefacts
        assertEquals(2, domainConfig.getArtifactInfo().get("type1").get("artifact1").getRemoteArtifacts().size());
        domainConfig.getArtifactInfo().get("type1").get("artifact1").getRemoteArtifacts().sort(Comparator.comparing(RemoteValidationArtifactInfo::getUrl));
        assertEquals("http://url1.com", domainConfig.getArtifactInfo().get("type1").get("artifact1").getRemoteArtifacts().get(0).getUrl());
        assertEquals("artifact1", domainConfig.getArtifactInfo().get("type1").get("artifact1").getRemoteArtifacts().get(0).getType());
        assertEquals("path2", domainConfig.getArtifactInfo().get("type1").get("artifact1").getRemoteArtifacts().get(0).getPreProcessorPath());
        assertEquals("ext2", domainConfig.getArtifactInfo().get("type1").get("artifact1").getRemoteArtifacts().get(0).getPreProcessorOutputExtension());
        assertEquals("http://url2.com", domainConfig.getArtifactInfo().get("type1").get("artifact1").getRemoteArtifacts().get(1).getUrl());
        assertEquals("artifact1", domainConfig.getArtifactInfo().get("type1").get("artifact1").getRemoteArtifacts().get(1).getType());
        assertNull(domainConfig.getArtifactInfo().get("type1").get("artifact1").getRemoteArtifacts().get(1).getPreProcessorPath());
        assertNull(domainConfig.getArtifactInfo().get("type1").get("artifact1").getRemoteArtifacts().get(1).getPreProcessorOutputExtension());
        // User-provided artefacts
        assertEquals(ExternalArtifactSupport.OPTIONAL, domainConfig.getArtifactInfo().get("type1").getOverallExternalArtifactSupport());
        assertEquals(ExternalArtifactSupport.OPTIONAL, domainConfig.getArtifactInfo().get("type1").get("artifact1").getExternalArtifactSupport());
        assertEquals(ValidationArtifactCombinationApproach.ALL, domainConfig.getArtifactInfo().get("type1").get("artifact1").getExternalArtifactCombinationApproach());
        assertEquals("http://url3.com", domainConfig.getArtifactInfo().get("type1").get("artifact1").getExternalArtifactPreProcessorPath());
        assertEquals("ext3", domainConfig.getArtifactInfo().get("type1").get("artifact1").getExternalArtifactPreProcessorOutputExtension());
        // Type with only local artefacts
        assertEquals(1, domainConfig.getArtifactInfo().get("type2").getTypes().size());
        assertEquals("artifact1", domainConfig.getArtifactInfo().get("type2").getTypes().iterator().next());
        assertTrue(domainConfig.getArtifactInfo().get("type2").hasPreconfiguredArtifacts());
        assertEquals(ExternalArtifactSupport.NONE, domainConfig.getArtifactInfo().get("type2").getOverallExternalArtifactSupport());
        assertEquals(ExternalArtifactSupport.NONE, domainConfig.getArtifactInfo().get("type2").get().getExternalArtifactSupport()); // artifact1 by default
        assertEquals(0, domainConfig.getArtifactInfo().get("type2").get().getRemoteArtifacts().size());
        assertEquals("localPath2", domainConfig.getArtifactInfo().get("type2").get().getLocalPath());
        assertNull(domainConfig.getArtifactInfo().get("type2").get().getPreProcessorPath());
        assertNull(domainConfig.getArtifactInfo().get("type2").get().getPreProcessorOutputExtension());
        assertNull(domainConfig.getArtifactInfo().get("type2").get().getExternalArtifactPreProcessorPath());
        assertNull(domainConfig.getArtifactInfo().get("type2").get().getExternalArtifactPreProcessorOutputExtension());
        assertEquals(ValidationArtifactCombinationApproach.ALL, domainConfig.getArtifactInfo().get("type2").get().getExternalArtifactCombinationApproach());
        assertEquals(ValidationArtifactCombinationApproach.ANY, domainConfig.getArtifactInfo().get("type2").get().getArtifactCombinationApproach());
    }
}
