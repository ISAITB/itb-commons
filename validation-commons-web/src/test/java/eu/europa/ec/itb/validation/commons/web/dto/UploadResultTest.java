package eu.europa.ec.itb.validation.commons.web.dto;

import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestResultType;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.config.ErrorResponseTypeEnum;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UploadResultTest {

    @Test
    void testPopulateCommon() {
        var helper = mock(LocalisationHelper.class);
        when(helper.localise(anyString())).thenAnswer(call -> call.getArgument(0).toString());
        var domainConfig = mock(WebDomainConfig.class);
        when(domainConfig.getCompleteTypeOptionLabel(anyString(), any(LocalisationHelper.class))).then(call -> "LABEL["+call.getArgument(0)+"]");
        when(domainConfig.getMaximumReportsForDetailedOutput()).thenReturn(1L);
        when(domainConfig.getMaximumReportsForXmlOutput()).thenReturn(2L);
        when(domainConfig.checkRemoteArtefactStatus(anyString())).thenReturn(true);
        var reports = new TestAssertionGroupReportsType();
        var detailedReport = mock(TAR.class);
        var timestamp = Utils.getXMLGregorianCalendarDateTime();
        when(detailedReport.getDate()).thenReturn(timestamp);
        when(detailedReport.getReports()).thenReturn(reports);
        when(detailedReport.getResult()).thenReturn(TestResultType.SUCCESS);
        var aggregateReport = mock(TAR.class);
        when(aggregateReport.getReports()).thenReturn(reports);
        var translations = mock(Translations.class);

        var result = new UploadResult<>();
        result.populateCommon(helper, "type1", domainConfig, "report1", "file1", detailedReport, aggregateReport, translations);
        assertSame(aggregateReport, result.getAggregateReport());
        assertEquals("file1", result.getFileName());
        assertNull(result.getMessage());
        assertSame(detailedReport, result.getReport());
        assertEquals("report1", result.getReportId());
        assertEquals(timestamp.toString(), result.getDate());
        assertSame(translations, result.getTranslations());
        assertEquals(1L, result.getMaximumReportsForDetailedOutput());
        assertEquals(2L, result.getMaximumReportsForXmlOutput());
        assertEquals("LABEL[type1]", result.getValidationTypeLabel());
        assertFalse(result.isShowAggregateReport());
        assertFalse(result.isRichTextReports());

        // Check message setting.
        var result2 = new UploadResult<>();
        result2.populateCommon(helper, "type1", domainConfig, "report1", "file1", detailedReport, aggregateReport, translations);
        result2.setMessage("Message");
        assertEquals("Message", result2.getMessage());

        // Check empty validation type.
        var result3 = new UploadResult<>();
        result3.populateCommon(helper, null, domainConfig, "report1", "file1", detailedReport, aggregateReport, translations);
        assertNull(result3.getValidationTypeLabel());

        when(domainConfig.checkRemoteArtefactStatus(anyString())).thenReturn(false);
        when(domainConfig.getResponseForRemoteArtefactLoadFailure(anyString())).thenReturn(ErrorResponseTypeEnum.WARN);
        var result4 = new UploadResult<>();
        result4.populateCommon(helper, "type1", domainConfig, "report1", "file1", detailedReport, aggregateReport, translations);
        assertEquals("validator.label.exception.failureToLoadRemoteArtefacts", result4.getMessage());
        assertFalse(result4.isMessageIsError());
    }

    @Test
    void testAdditionalErrorMessages() {
        var result = new UploadResult<>();
        assertNull(result.getAdditionalErrorMessages());
        result.setAdditionalErrorMessages(List.of("Message"));
        assertNotNull(result.getAdditionalErrorMessages());
        assertEquals(1, result.getAdditionalErrorMessages().size());
        assertEquals("Message", result.getAdditionalErrorMessages().get(0));
    }

}
