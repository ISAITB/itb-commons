package eu.europa.ec.itb.validation.commons.config;

import com.gitb.tr.TAR;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void testApplyMetadata() {
        var report = new TAR();
        var config = new DomainConfig();
        config.setValidationServiceName("ServiceName");
        config.setValidationServiceVersion("ServiceVersion");
        config.setReportId("ID");
        config.setReportName("ReportName");
        config.applyMetadata(report, "type1");
        assertEquals("ID", report.getId());
        assertEquals("ReportName", report.getName());
        assertNotNull(report.getOverview());
        assertEquals("ServiceName", report.getOverview().getValidationServiceName());
        assertEquals("ServiceVersion", report.getOverview().getValidationServiceVersion());
        assertEquals("type1", report.getOverview().getProfileID());
        assertNull(report.getOverview().getCustomizationID());

        report = new TAR();
        config.setReportProfileIds(Map.of("type1", "profile1", "type2", "profile2"));
        config.setReportCustomisationIds(Map.of("type1", "customisation1", "type2", "customisation2"));
        config.applyMetadata(report, "type1");
        assertEquals("profile1", report.getOverview().getProfileID());
        assertEquals("customisation1", report.getOverview().getCustomizationID());

        report = new TAR();
        config.applyMetadata(report, "typeX");
        assertEquals("typeX", report.getOverview().getProfileID());
        assertNull(report.getOverview().getCustomizationID());

        report = new TAR();
        config.setReportProfileIdDefault("defaultProfile");
        config.setReportCustomisationIdDefault("defaultCustomisation");
        config.applyMetadata(report, "typeX");
        assertEquals("defaultProfile", report.getOverview().getProfileID());
        assertEquals("defaultCustomisation", report.getOverview().getCustomizationID());
    }

    @Test
    void testTypeAlias() {
        var config = new DomainConfig();
        config.setType(List.of("type1.option1", "type1.option2", "type2"));

        assertNull(config.resolveAlias("type1_latest"));
        config.setValidationTypeAlias(Map.of("type1_latest", "type1.option2"));
        assertEquals("type1.option2", config.resolveAlias("type1_latest"));

        config.setValidationTypeAlias(Map.of("type1_latest", "type.option3"));
        assertNull(config.resolveAlias("type1_latest"));
    }
}
