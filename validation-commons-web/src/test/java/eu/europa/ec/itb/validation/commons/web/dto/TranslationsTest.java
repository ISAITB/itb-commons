package eu.europa.ec.itb.validation.commons.web.dto;

import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TranslationsTest {

    @Test
    void testTranslations() {
        var localiser = mock(LocalisationHelper.class);
        when(localiser.localise(anyString())).then(call -> "TRANSLATED["+call.getArgument(0)+"]");
        when(localiser.localise(anyString(), any())).then(call -> "TRANSLATED["+call.getArgument(0)+"]");
        var report = mock(TAR.class);
        when(report.getResult()).thenReturn(TestResultType.SUCCESS);
        var translations = new Translations(localiser, report, mock(DomainConfig.class));
        assertEquals("TRANSLATED[validator.label.resultSectionTitle]", translations.getResultSectionTitle());
        assertEquals("TRANSLATED[validator.label.result.success]", translations.getResultValue());
        assertEquals("TRANSLATED[validator.label.resultErrorsLabel]", translations.getResultErrorsLabel());
        assertEquals("TRANSLATED[validator.label.resultWarningsLabel]", translations.getResultWarningsLabel());
        assertEquals("TRANSLATED[validator.label.resultMessagesLabel]", translations.getResultMessagesLabel());
        assertEquals("TRANSLATED[validator.label.resultDateLabel]", translations.getResultDateLabel());
        assertEquals("TRANSLATED[validator.label.resultFileNameLabel]", translations.getResultFileNameLabel());
        assertEquals("TRANSLATED[validator.label.resultValidationTypeLabel]", translations.getResultValidationTypeLabel());
        assertEquals("TRANSLATED[validator.label.resultResultLabel]", translations.getResultResultLabel());
        assertEquals("TRANSLATED[validator.label.resultSubSectionDetailsTitle]", translations.getResultSubSectionDetailsTitle());
        assertEquals("TRANSLATED[validator.label.resultSubSectionOverviewTitle]", translations.getResultSubSectionOverviewTitle());
        assertEquals("TRANSLATED[validator.label.viewReportItemsShowAll]", translations.getViewReportItemsShowAll());
        assertEquals("TRANSLATED[validator.label.viewReportItemsShowErrors]", translations.getViewReportItemsShowErrors());
        assertEquals("TRANSLATED[validator.label.viewReportItemsShowWarnings]", translations.getViewReportItemsShowWarnings());
        assertEquals("TRANSLATED[validator.label.viewReportItemsShowMessages]", translations.getViewReportItemsShowMessages());
        assertEquals("TRANSLATED[validator.label.viewReportItemsDetailed]", translations.getViewReportItemsDetailed());
        assertEquals("TRANSLATED[validator.label.viewReportItemsAggregated]", translations.getViewReportItemsAggregated());
        assertEquals("TRANSLATED[validator.label.maximumReportsExceededForDetailedOutputMessage]", translations.getMaximumReportsExceededForDetailedOutputMessage());
        assertEquals("TRANSLATED[validator.label.resultLocationLabel]", translations.getResultLocationLabel());
        assertEquals("TRANSLATED[validator.label.resultTestLabel]", translations.getResultTestLabel());
        assertEquals("TRANSLATED[validator.label.additionalInfoLabel]", translations.getAdditionalInfoLabel());
        assertEquals("TRANSLATED[validator.label.popupCloseButton]", translations.getPopupCloseButton());
        assertEquals("TRANSLATED[validator.label.popupTitle]", translations.getPopupTitle());
        assertEquals("TRANSLATED[validator.label.popupCloseButton]", translations.getPopupCloseButton());
        assertEquals("TRANSLATED[validator.label.reportDetailedPDF]", translations.getReportDetailedPDF());
        assertEquals("TRANSLATED[validator.label.reportDetailedCSV]", translations.getReportDetailedCSV());
        assertEquals("TRANSLATED[validator.label.reportAggregatedPDF]", translations.getReportAggregatedPDF());
        assertEquals("TRANSLATED[validator.label.reportAggregatedCSV]", translations.getReportAggregatedCSV());
        assertEquals("TRANSLATED[validator.label.viewAnnotatedInputButton]", translations.getViewAnnotatedInputButton());
        assertEquals("TRANSLATED[validator.label.downloadReportButton]", translations.getDownloadReportButton());
        assertEquals("TRANSLATED[validator.label.reportDetailedXML]", translations.getReportDetailedXML());
        assertEquals("TRANSLATED[validator.label.reportAggregatedXML]", translations.getReportAggregatedXML());
        assertEquals("TRANSLATED[validator.label.maximumReportsExceededForXmlOutputMessage]", translations.getMaximumReportsExceededForXmlOutputMessage());
        assertEquals("TRANSLATED[validator.label.viewDetailsButton]", translations.getViewDetailsButton());
        assertEquals("TRANSLATED[validator.label.viewSummaryButton]", translations.getViewSummaryButton());
    }

}
