package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WebDomainConfigCacheTest {

    private WebDomainConfig createWebDomainConfig() {
        return new WebDomainConfig();
    }

    private WebDomainConfigCache<WebDomainConfig> createWebDomainConfigCache() {
        return new WebDomainConfigCache<>() {
            @Override
            protected WebDomainConfig newDomainConfig() {
                return createWebDomainConfig();
            }

            @Override
            protected ValidatorChannel[] getSupportedChannels() {
                return new ValidatorChannel[] { ValidatorChannel.FORM };
            }
        };
    }

    @Test
    void testAddDomainConfiguration() {
        var cache = createWebDomainConfigCache();
        var config = createWebDomainConfig();
        config.setDeclaredType(List.of("type1"));
        config.setType(List.of("type1.option1", "type1.option2"));
        config.setValidationTypeOptions(Map.of("type1", List.of("option1", "option2", "option3")));
        var configProperties = new MapConfiguration(Map.ofEntries(
                Map.entry("validator.uploadTitle", "title"),
                Map.entry("validator.reportTitle", "report"),
                Map.entry("validator.webServiceId", "service"),
                Map.entry("validator.typeLabel.type1", "typeLabel1"),
                Map.entry("validator.optionLabel.option2", "defaultOptionLabel1"),
                Map.entry("validator.typeOptionLabel.type1.option1", "optionLabel1"),
                Map.entry("validator.completeTypeOptionLabel.type1.option1", "typeOptionLabel1"),
                Map.entry("validator.webServiceDescription.key", "description"),
                Map.entry("validator.showAbout", false),
                Map.entry("validator.supportMinimalUserInterface", true),
                Map.entry("validator.bannerHtml", "banner"),
                Map.entry("validator.footerHtml", "footer"),
                Map.entry("validator.locale.default", "en"),
                Map.entry("validator.locale.available","en,es,fr")
        ));
        cache.addDomainConfiguration(config, configProperties);
        cache.addResourceBundlesConfiguration(config, configProperties);
        var localisationHelper = new LocalisationHelper(config, Locale.ENGLISH);
        assertEquals("title", localisationHelper.localise("validator.uploadTitle"));
        assertEquals("report", localisationHelper.localise("validator.reportTitle"));
        assertEquals("service", config.getWebServiceId());
        assertEquals("typeLabel1", config.getValidationTypeLabel("type1", localisationHelper));
        assertEquals("typeOptionLabel1", config.getCompleteTypeOptionLabel("type1.option1", localisationHelper));
        assertEquals("typeLabel1 - defaultOptionLabel1", config.getCompleteTypeOptionLabel("type1.option2", localisationHelper));
        assertEquals("typeLabel1 - option3", config.getCompleteTypeOptionLabel("type1.option3", localisationHelper));
        assertEquals("optionLabel1", config.getValidationTypeOptionLabel("type1", "option1", localisationHelper));
        assertEquals("defaultOptionLabel1", config.getValidationTypeOptionLabel("type1", "option2", localisationHelper));
        assertEquals("option3", config.getValidationTypeOptionLabel("type1", "option3", localisationHelper));
        assertEquals(1, config.getWebServiceDescription().size());
        assertEquals("description", config.getWebServiceDescription().get("key"));
        assertFalse(config.isShowAbout());
        assertTrue(config.isSupportMinimalUserInterface());
        assertEquals("banner", localisationHelper.localise("validator.bannerHtml"));
        assertEquals("footer", localisationHelper.localise("validator.footerHtml"));
        assertFalse(config.isSupportUserInterfaceEmbedding());
    }

    @Test
    void testHiddenValidationTypes() {
        // Define configuration
        // Two types, type1 has 2 options, type1, has no options
        var config = createWebDomainConfig();
        var configCache = createWebDomainConfigCache();
        configCache.addDomainConfiguration(config, new MapConfiguration(new HashMap<>()));
        config.setType(List.of("type1", "type2"));
        config.setValidationTypeOptions(Map.of("type1", List.of("option1", "option2")));

        // Check the options have been added correctly and are not hidden
        assertEquals(2, config.getType().size());
        assertTrue(config.hasMultipleNonHiddenValidationTypes());
        assertEquals(2, config.getVisibleValidationTypeOptions("type1").size());
        assertEquals(0, config.getVisibleValidationTypeOptions("type2").size());
        assertFalse(config.isHiddenType("type2"));

        //Hide option1 in type1 and check it hides correctly and the types are still both visible
        config.setHiddenTypes(List.of("type1.option1"));
        assertEquals(1, config.getHiddenTypes().size());
        assertTrue(config.hasMultipleNonHiddenValidationTypes());
        assertFalse(config.isHiddenType("type1.option2"));
        assertTrue(config.isHiddenType("type1.option1"));
        assertEquals(1, config.getVisibleValidationTypeOptions("type1").size());

        //Remove hidden types and check they get removed and both types are visible
        config.setHiddenTypes(new ArrayList<>());
        assertTrue(config.hasMultipleNonHiddenValidationTypes());
        assertFalse(config.isHiddenType("type1.option1"));
        assertFalse(config.isHiddenType("type1.option2"));
        assertFalse(config.isHiddenType("type1"));
        assertEquals(2, config.getVisibleValidationTypeOptions("type1").size());

        //Add both options of type1 and check the entire type hides
        config.setHiddenTypes(List.of("type1.option1", "type1.option2"));
        assertFalse(config.hasMultipleNonHiddenValidationTypes());
        assertTrue(config.isHiddenType("type1"));
        assertEquals(0, config.getVisibleValidationTypeOptions("type1").size());
    }

    @Test
    void testSetLabels() {
        var cache = createWebDomainConfigCache();
        var config = createWebDomainConfig();
        var configProperties = new MapConfiguration(Map.ofEntries(
                Map.entry("validator.label.resultSectionTitle", "resultSectionTitle"),
                Map.entry("validator.label.fileInputLabel", "fileInputLabel"),
                Map.entry("validator.label.fileInputPlaceholder", "fileInputPlaceholder"),
                Map.entry("validator.label.typeLabel", "typeLabel"),
                Map.entry("validator.label.optionLabel", "optionLabel"),
                Map.entry("validator.label.uploadButton", "uploadButton"),
                Map.entry("validator.label.resultSubSectionOverviewTitle", "resultSubSectionOverviewTitle"),
                Map.entry("validator.label.resultDateLabel", "resultDateLabel"),
                Map.entry("validator.label.resultFileNameLabel", "resultFileNameLabel"),
                Map.entry("validator.label.resultResultLabel", "resultResultLabel"),
                Map.entry("validator.label.resultErrorsLabel", "resultErrorsLabel"),
                Map.entry("validator.label.resultWarningsLabel", "resultWarningsLabel"),
                Map.entry("validator.label.resultMessagesLabel", "resultMessagesLabel"),
                Map.entry("validator.label.viewAnnotatedInputButton", "viewAnnotatedInputButton"),
                Map.entry("validator.label.downloadReportButton", "downloadReportButton"),
                Map.entry("validator.label.resultSubSectionDetailsTitle", "resultSubSectionDetailsTitle"),
                Map.entry("validator.label.resultTestLabel", "resultTestLabel"),
                Map.entry("validator.label.resultLocationLabel", "resultLocationLabel"),
                Map.entry("validator.label.popupTitle", "popupTitle"),
                Map.entry("validator.label.popupCloseButton", "popupCloseButton"),
                Map.entry("validator.label.optionContentFile", "optionContentFile"),
                Map.entry("validator.label.optionContentURI", "optionContentURI"),
                Map.entry("validator.label.optionContentDirectInput", "optionContentDirectInput"),
                Map.entry("validator.label.resultValidationTypeLabel", "resultValidationTypeLabel"),
                Map.entry("validator.label.includeExternalArtefacts", "includeExternalArtefacts"),
                Map.entry("validator.label.externalArtefactsTooltip", "externalArtefactsTooltip"),
                Map.entry("validator.label.maximumReportsExceededForDetailedOutputMessage", "maximumReportsExceededForDetailedOutputMessage"),
                Map.entry("validator.label.maximumReportsExceededForXmlOutputMessage", "maximumReportsExceededForXmlOutputMessage")
        ));
        cache.addDomainConfiguration(config, configProperties);
        cache.addResourceBundlesConfiguration(config, configProperties);
        var localisationHelper = new LocalisationHelper(config, Locale.ENGLISH);
        assertEquals("resultSectionTitle", localisationHelper.localise("validator.label.resultSectionTitle"));
        assertEquals("fileInputLabel", localisationHelper.localise("validator.label.fileInputLabel"));
        assertEquals("fileInputPlaceholder", localisationHelper.localise("validator.label.fileInputPlaceholder"));
        assertEquals("typeLabel", localisationHelper.localise("validator.label.typeLabel"));
        assertEquals("optionLabel", localisationHelper.localise("validator.label.optionLabel"));
        assertEquals("uploadButton", localisationHelper.localise("validator.label.uploadButton"));
        assertEquals("resultSubSectionOverviewTitle", localisationHelper.localise("validator.label.resultSubSectionOverviewTitle"));
        assertEquals("resultDateLabel", localisationHelper.localise("validator.label.resultDateLabel"));
        assertEquals("resultFileNameLabel", localisationHelper.localise("validator.label.resultFileNameLabel"));
        assertEquals("resultResultLabel", localisationHelper.localise("validator.label.resultResultLabel"));
        assertEquals("resultErrorsLabel", localisationHelper.localise("validator.label.resultErrorsLabel"));
        assertEquals("resultWarningsLabel", localisationHelper.localise("validator.label.resultWarningsLabel"));
        assertEquals("resultMessagesLabel", localisationHelper.localise("validator.label.resultMessagesLabel"));
        assertEquals("viewAnnotatedInputButton", localisationHelper.localise("validator.label.viewAnnotatedInputButton"));
        assertEquals("downloadReportButton", localisationHelper.localise("validator.label.downloadReportButton"));
        assertEquals("resultSubSectionDetailsTitle", localisationHelper.localise("validator.label.resultSubSectionDetailsTitle"));
        assertEquals("resultTestLabel", localisationHelper.localise("validator.label.resultTestLabel"));
        assertEquals("resultLocationLabel", localisationHelper.localise("validator.label.resultLocationLabel"));
        assertEquals("popupTitle", localisationHelper.localise("validator.label.popupTitle"));
        assertEquals("popupCloseButton", localisationHelper.localise("validator.label.popupCloseButton"));
        assertEquals("optionContentFile", localisationHelper.localise("validator.label.optionContentFile"));
        assertEquals("optionContentURI", localisationHelper.localise("validator.label.optionContentURI"));
        assertEquals("optionContentDirectInput", localisationHelper.localise("validator.label.optionContentDirectInput"));
        assertEquals("resultValidationTypeLabel", localisationHelper.localise("validator.label.resultValidationTypeLabel"));
        assertEquals("includeExternalArtefacts", localisationHelper.localise("validator.label.includeExternalArtefacts"));
        assertEquals("externalArtefactsTooltip", localisationHelper.localise("validator.label.externalArtefactsTooltip"));
        assertEquals("maximumReportsExceededForDetailedOutputMessage", localisationHelper.localise("validator.label.maximumReportsExceededForDetailedOutputMessage"));
        assertEquals("maximumReportsExceededForXmlOutputMessage", localisationHelper.localise("validator.label.maximumReportsExceededForXmlOutputMessage"));
   }

}
