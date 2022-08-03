package eu.europa.ec.itb.validation.commons.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainConfigTest {

    @Test
    void testRemoteArtifactCacheCheckDefaults() {
        var config = new DomainConfig();
        config.setType(List.of("type1"));
        assertTrue(config.checkRemoteArtefactStatus("type1"));
        assertTrue(config.checkRemoteArtefactStatus("type2"));
    }

    @Test
    void testRemoteArtifactCacheCheckUpdates() {
        var config = new DomainConfig();
        config.setType(List.of("type1"));
        assertTrue(config.checkRemoteArtefactStatus("type1"));
        config.setRemoteArtefactStatus("type1", false);
        assertFalse(config.checkRemoteArtefactStatus("type1"));
        config.setRemoteArtefactStatus("type1", true);
        assertTrue(config.checkRemoteArtefactStatus("type1"));
    }

    @Test
    void testRemoteArtifactCacheCheckWithNoType() {
        var config = new DomainConfig();
        config.setType(List.of("type1"));
        config.setDefaultType("type1");
        assertTrue(config.checkRemoteArtefactStatus("type1"));
        assertTrue(config.checkRemoteArtefactStatus(null));
        config.setDefaultType(null);
        assertTrue(config.checkRemoteArtefactStatus(null));
    }
}
