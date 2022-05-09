package eu.europa.ec.itb.validation.commons.web.dto;

import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UploadResultTest {

    @Test
    void testPopulateCommon() {
        var helper = mock(LocalisationHelper.class);
        var domainConfig = mock(WebDomainConfig.class);
        when(domainConfig.getCompleteTypeOptionLabel(anyString(), any(LocalisationHelper.class))).then(call -> "LABEL["+call.getArgument(0)+"]");
        when(domainConfig.getMaximumReportsForDetailedOutput()).thenReturn(1L);
        when(domainConfig.getMaximumReportsForXmlOutput()).thenReturn(2L);
        var reports = new TestAssertionGroupReportsType();
        var detailedReport = mock(TAR.class);
        var timestamp = Utils.getXMLGregorianCalendarDateTime();
        when(detailedReport.getDate()).thenReturn(timestamp);
        when(detailedReport.getReports()).thenReturn(reports);
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

        // Check message setting.
        var result2 = new UploadResult<>();
        result2.populateCommon(helper, "type1", domainConfig, "report1", "file1", detailedReport, aggregateReport, translations);
        result2.setMessage("Message");
        assertEquals("Message", result2.getMessage());

        // Check empty validation type.
        var result3 = new UploadResult<>();
        result3.populateCommon(helper, null, domainConfig, "report1", "file1", detailedReport, aggregateReport, translations);
        assertNull(result3.getValidationTypeLabel());
    }

}
