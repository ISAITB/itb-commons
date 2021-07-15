package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WebDomainConfigCacheTest {

    private WebDomainConfig<LabelConfig> createWebDomainConfig() {
        return new WebDomainConfig<>() {
            @Override
            protected LabelConfig newLabelConfig() {
                return new LabelConfig();
            }
        };
    }

    private WebDomainConfigCache<WebDomainConfig<LabelConfig>> createWebDomainConfigCache() {
        return new WebDomainConfigCache<>() {
            @Override
            protected WebDomainConfig<LabelConfig> newDomainConfig() {
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
                Map.entry("validator.footerHtml", "footer")
        ));
        cache.addDomainConfiguration(config, configProperties);
        assertEquals("title", config.getUploadTitle());
        assertEquals("report", config.getReportTitle());
        assertEquals("service", config.getWebServiceId());
        assertEquals(4, config.getTypeLabel().size());
        assertEquals("typeLabel1", config.getTypeLabel().get("type1"));
        assertEquals("typeOptionLabel1", config.getTypeLabel().get("type1.option1"));
        assertEquals("typeLabel1 - defaultOptionLabel1", config.getTypeLabel().get("type1.option2"));
        assertEquals("typeLabel1 - option3", config.getTypeLabel().get("type1.option3"));
        assertEquals(1, config.getTypeOptionLabel().size());
        assertEquals(3, config.getTypeOptionLabel().get("type1").size());
        assertEquals("optionLabel1", config.getTypeOptionLabel().get("type1").get("option1"));
        assertEquals("optionLabel1", config.getValidationTypeOptionLabel("type1", "option1"));
        assertEquals("defaultOptionLabel1", config.getTypeOptionLabel().get("type1").get("option2"));
        assertEquals("defaultOptionLabel1", config.getValidationTypeOptionLabel("type1", "option2"));
        assertEquals("option3", config.getTypeOptionLabel().get("type1").get("option3"));
        assertEquals("option3", config.getValidationTypeOptionLabel("type1", "option3"));
        assertEquals(1, config.getWebServiceDescription().size());
        assertEquals("description", config.getWebServiceDescription().get("key"));
        assertFalse(config.isShowAbout());
        assertTrue(config.isSupportMinimalUserInterface());
        assertEquals("banner", config.getHtmlBanner());
        assertEquals("footer", config.getHtmlFooter());
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
                Map.entry("validator.label.downloadXMLReportButton", "downloadXMLReportButton"),
                Map.entry("validator.label.downloadPDFReportButton", "downloadPDFReportButton"),
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
        cache.setLabels(config, configProperties);
        assertEquals("resultSectionTitle", config.getLabel().getResultSectionTitle());
        assertEquals("fileInputLabel", config.getLabel().getFileInputLabel());
        assertEquals("fileInputPlaceholder", config.getLabel().getFileInputPlaceholder());
        assertEquals("typeLabel", config.getLabel().getTypeLabel());
        assertEquals("optionLabel", config.getLabel().getOptionLabel());
        assertEquals("uploadButton", config.getLabel().getUploadButton());
        assertEquals("resultSubSectionOverviewTitle", config.getLabel().getResultSubSectionOverviewTitle());
        assertEquals("resultDateLabel", config.getLabel().getResultDateLabel());
        assertEquals("resultFileNameLabel", config.getLabel().getResultFileNameLabel());
        assertEquals("resultResultLabel", config.getLabel().getResultResultLabel());
        assertEquals("resultErrorsLabel", config.getLabel().getResultErrorsLabel());
        assertEquals("resultWarningsLabel", config.getLabel().getResultWarningsLabel());
        assertEquals("resultMessagesLabel", config.getLabel().getResultMessagesLabel());
        assertEquals("viewAnnotatedInputButton", config.getLabel().getViewAnnotatedInputButton());
        assertEquals("downloadXMLReportButton", config.getLabel().getDownloadXMLReportButton());
        assertEquals("downloadPDFReportButton", config.getLabel().getDownloadPDFReportButton());
        assertEquals("resultSubSectionDetailsTitle", config.getLabel().getResultSubSectionDetailsTitle());
        assertEquals("resultTestLabel", config.getLabel().getResultTestLabel());
        assertEquals("resultLocationLabel", config.getLabel().getResultLocationLabel());
        assertEquals("popupTitle", config.getLabel().getPopupTitle());
        assertEquals("popupCloseButton", config.getLabel().getPopupCloseButton());
        assertEquals("optionContentFile", config.getLabel().getOptionContentFile());
        assertEquals("optionContentURI", config.getLabel().getOptionContentURI());
        assertEquals("optionContentDirectInput", config.getLabel().getOptionContentDirectInput());
        assertEquals("resultValidationTypeLabel", config.getLabel().getResultValidationTypeLabel());
        assertEquals("includeExternalArtefacts", config.getLabel().getIncludeExternalArtefacts());
        assertEquals("externalArtefactsTooltip", config.getLabel().getExternalArtefactsTooltip());
        assertEquals("maximumReportsExceededForDetailedOutputMessage", config.getLabel().getMaximumReportsExceededForDetailedOutputMessage());
        assertEquals("maximumReportsExceededForXmlOutputMessage", config.getLabel().getMaximumReportsExceededForXmlOutputMessage());
   }

}
