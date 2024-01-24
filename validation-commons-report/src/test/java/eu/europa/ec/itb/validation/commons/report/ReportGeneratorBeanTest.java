package eu.europa.ec.itb.validation.commons.report;

import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.ValidationCounters;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.report.dto.ReportLabels;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReportGeneratorBeanTest {

    private Path tmpPath;

    @BeforeEach
    void setup() throws IOException {
        tmpPath = Files.createTempDirectory("itb");
    }

    @AfterEach
    void teardown() {
        FileUtils.deleteQuietly(tmpPath.toFile());
    }

    ReportGeneratorBean createBean(ReportGenerator generator) throws Exception {
        ReportGeneratorBean bean = new ReportGeneratorBean();
        var field = ReportGeneratorBean.class.getDeclaredField("reportGenerator");
        field.setAccessible(true);
        field.set(bean, generator);
        return bean;
    }

    private ReportLabels createLabels() {
        var labels = new ReportLabels();
        labels.setTitle("Title1");
        labels.setPage("Page");
        labels.setTest("Test");
        labels.setResultType("ResultType");
        labels.setResult("Result");
        labels.setLocation("Location");
        labels.setFindings("Findings");
        labels.setFindingsDetails("FindingsDetails");
        labels.setDate("Date");
        labels.setFileName("FileName");
        labels.setDetails("Details");
        labels.setOverview("Overview");
        labels.setOf("Of");
        labels.setAssertionId("Assertion");
        return labels;
    }

    @Test
    void testWriteReportFromObject() throws Exception {
        var tar = new TAR();
        var labels = createLabels();
        var outputFile = Path.of(tmpPath.toString(), "report.pdf");
        var reportGenerator = mock(ReportGenerator.class);
        doAnswer((Answer<?>) invocation -> {
            assertSame(tar, invocation.getArgument(0));
            assertTrue(invocation.getArgument(1) instanceof FileOutputStream);
            assertTrue(invocation.getArgument(2) instanceof Function);
            var result = ((Function)invocation.getArgument(2)).apply(null);
            assertTrue(result instanceof ReportLabels);
            assertEquals("Title1", ((ReportLabels) result).getTitle());
            return null;
        }).when(reportGenerator).writeTARReport(any(TAR.class), any(), any(), anyBoolean());
        var bean = createBean(reportGenerator);
        assertDoesNotThrow(() -> bean.writeReport(tar, outputFile.toFile(), (report) -> labels, false));
        verify(reportGenerator, times(1)).writeTARReport(any(TAR.class), any(), any(), anyBoolean());
        // Alternate call.
        reset(reportGenerator);
        var localiser = mock(LocalisationHelper.class);
        when(localiser.localise(anyString())).thenReturn("Label");
        assertDoesNotThrow(() -> bean.writeReport(tar, outputFile.toFile(), localiser, false));
        verify(reportGenerator, times(1)).writeTARReport(any(TAR.class), any(), any(), anyBoolean());
    }

    @Test
    void testWriteReportFromObjectError() throws Exception {
        var labels = createLabels();
        var tar = new TAR();
        var outputFile = Path.of(tmpPath.toString(), "report.pdf");
        var reportGenerator = mock(ReportGenerator.class);
        doAnswer((Answer<?>) invocation -> {
            assertSame(tar, invocation.getArgument(0));
            assertTrue(invocation.getArgument(1) instanceof FileOutputStream);
            assertTrue(invocation.getArgument(2) instanceof Function);
            var result = ((Function)invocation.getArgument(2)).apply(null);
            assertTrue(result instanceof ReportLabels);
            assertEquals("Title1", ((ReportLabels) result).getTitle());
            throw new IllegalStateException();
        }).when(reportGenerator).writeTARReport(any(TAR.class), any(), any(), anyBoolean());
        var bean = createBean(reportGenerator);
        assertThrows(ValidatorException.class, () -> bean.writeReport(tar, outputFile.toFile(), (report) -> labels, false));
        verify(reportGenerator, times(1)).writeTARReport(any(TAR.class), any(), any(), anyBoolean());
    }


    @Test
    void testWriteReportFromFile() throws Exception {
        var labels = createLabels();
        var inputFile = Path.of(tmpPath.toString(), "report.xml");
        Files.createFile(inputFile);
        Files.writeString(inputFile, "<tar></tar>");
        var outputFile = Path.of(tmpPath.toString(), "report.pdf");
        var reportGenerator = mock(ReportGenerator.class);
        doAnswer((Answer<?>) invocation -> {
            assertTrue(invocation.getArgument(0) instanceof FileInputStream);
            assertTrue(invocation.getArgument(1) instanceof FileOutputStream);
            assertTrue(invocation.getArgument(2) instanceof Function);
            var result = ((Function)invocation.getArgument(2)).apply(null);
            assertTrue(result instanceof ReportLabels);
            assertEquals("Title1", ((ReportLabels) result).getTitle());
            return null;
        }).when(reportGenerator).writeTARReport(any(FileInputStream.class), any(), any(), anyBoolean());
        var bean = createBean(reportGenerator);
        assertDoesNotThrow(() -> bean.writeReport(inputFile.toFile(), outputFile.toFile(), (report) -> labels, false));
        verify(reportGenerator, times(1)).writeTARReport(any(FileInputStream.class), any(), any(), anyBoolean());
        // Alternate call.
        reset(reportGenerator);
        var localiser = mock(LocalisationHelper.class);
        when(localiser.localise(anyString())).thenReturn("Label");
        assertDoesNotThrow(() -> bean.writeReport(inputFile.toFile(), outputFile.toFile(), localiser, false));
        verify(reportGenerator, times(1)).writeTARReport(any(FileInputStream.class), any(), any(), anyBoolean());
    }

    @Test
    void testWriteReportFromFileError() throws Exception {
        var labels = createLabels();
        var inputFile = Path.of(tmpPath.toString(), "report.xml");
        Files.createFile(inputFile);
        Files.writeString(inputFile, "<tar></tar>");
        var outputFile = Path.of(tmpPath.toString(), "report.pdf");
        var reportGenerator = mock(ReportGenerator.class);
        doAnswer((Answer<?>) invocation -> {
            assertTrue(invocation.getArgument(0) instanceof FileInputStream);
            assertTrue(invocation.getArgument(1) instanceof FileOutputStream);
            assertTrue(invocation.getArgument(2) instanceof Function);
            var result = ((Function)invocation.getArgument(2)).apply(null);
            assertTrue(result instanceof ReportLabels);
            assertEquals("Title1", ((ReportLabels) result).getTitle());
            throw new IllegalStateException();
        }).when(reportGenerator).writeTARReport(any(FileInputStream.class), any(), any(), anyBoolean());
        var bean = createBean(reportGenerator);
        assertThrows(ValidatorException.class, () -> bean.writeReport(inputFile.toFile(), outputFile.toFile(), (report) -> labels, false));
        verify(reportGenerator, times(1)).writeTARReport(any(FileInputStream.class), any(), any(), anyBoolean());
    }

    @Test
    void testGetReportLabels() throws Exception {
        var reportGenerator = mock(ReportGenerator.class);
        var successText = TestResultType.SUCCESS.value().toLowerCase(Locale.ROOT);
        var bean = createBean(reportGenerator);
        var helper = getDefaultLabelMockHelper(List.of("validator.reportTitle", "validator.label.resultSubSectionOverviewTitle", "validator.label.resultSubSectionDetailsTitle",
                "validator.label.resultDateLabel", "validator.label.resultResultLabel", "validator.label.resultFileNameLabel",
                "validator.label.resultFindingsLabel",
                "validator.label.resultTestLabel", "validator.label.resultLocationLabel", "validator.label.pageLabel",
                "validator.label.ofLabel", "validator.label.result."+successText), List.of("validator.label.resultFindingsDetailsLabel"));
        var tar = new TAR();
        tar.setResult(TestResultType.SUCCESS);
        tar.setCounters(new ValidationCounters());
        tar.getCounters().setNrOfErrors(BigInteger.ZERO);
        tar.getCounters().setNrOfWarnings(BigInteger.ZERO);
        tar.getCounters().setNrOfAssertions(BigInteger.ZERO);
        var result = bean.getReportLabels(helper, tar);
        assertNotNull(result);
        assertEquals("validator.reportTitle_translated", result.getTitle());
        assertEquals("validator.label.resultSubSectionOverviewTitle_translated", result.getOverview());
        assertEquals("validator.label.resultSubSectionDetailsTitle_translated", result.getDetails());
        assertEquals("validator.label.resultDateLabel_translated", result.getDate());
        assertEquals("validator.label.resultResultLabel_translated", result.getResult());
        assertEquals("validator.label.resultFileNameLabel_translated", result.getFileName());
        assertEquals("validator.label.resultFindingsLabel_translated", result.getFindings());
        assertEquals("validator.label.resultFindingsDetailsLabel_translated", result.getFindingsDetails());
        assertEquals("validator.label.resultTestLabel_translated", result.getTest());
        assertEquals("validator.label.resultLocationLabel_translated", result.getLocation());
        assertEquals("validator.label.pageLabel_translated", result.getPage());
        assertEquals("validator.label.ofLabel_translated", result.getOf());
        assertEquals("validator.label.result."+successText+"_translated", result.getResultType());
    }

    private LocalisationHelper getDefaultLabelMockHelper(List<String> keys, List<String> keysWithArgs) {
        var helper = mock(LocalisationHelper.class);
        for (var key: keys) {
            when(helper.localise(eq(key))).thenReturn(key+"_translated");
        }
        for (var key: keysWithArgs) {
            when(helper.localise(eq(key), any(Object[].class))).thenReturn(key+"_translated");
        }
        return helper;
    }

}
