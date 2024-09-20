package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.RemoteValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactCombinationApproach;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DomainConfigCacheSupportMethodTest {

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
