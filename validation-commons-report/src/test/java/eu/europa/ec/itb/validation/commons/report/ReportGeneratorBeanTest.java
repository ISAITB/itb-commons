package eu.europa.ec.itb.validation.commons.report;

import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ReportGeneratorBeanTest {

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
        labels.setMessages("Messages");
        labels.setWarnings("Warnings");
        labels.setErrors("Errors");
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
        }).when(reportGenerator).writeTARReport(any(TAR.class), any(), any());
        var bean = createBean(reportGenerator);
        assertDoesNotThrow(() -> bean.writeReport(tar, outputFile.toFile(), (report) -> labels));
        verify(reportGenerator, times(1)).writeTARReport(any(TAR.class), any(), any());
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
        }).when(reportGenerator).writeTARReport(any(TAR.class), any(), any());
        var bean = createBean(reportGenerator);
        assertThrows(ValidatorException.class, () -> bean.writeReport(tar, outputFile.toFile(), (report) -> labels));
        verify(reportGenerator, times(1)).writeTARReport(any(TAR.class), any(), any());
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
        }).when(reportGenerator).writeTARReport(any(FileInputStream.class), any(), any());
        var bean = createBean(reportGenerator);
        assertDoesNotThrow(() -> bean.writeReport(inputFile.toFile(), outputFile.toFile(), (report) -> labels));
        verify(reportGenerator, times(1)).writeTARReport(any(FileInputStream.class), any(), any());
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
        }).when(reportGenerator).writeTARReport(any(FileInputStream.class), any(), any());
        var bean = createBean(reportGenerator);
        assertThrows(ValidatorException.class, () -> bean.writeReport(inputFile.toFile(), outputFile.toFile(), (report) -> labels));
        verify(reportGenerator, times(1)).writeTARReport(any(FileInputStream.class), any(), any());
    }

    @Test
    void testGetReportLabels() throws Exception {
        var reportGenerator = mock(ReportGenerator.class);
        var successText = TestResultType.SUCCESS.value().toLowerCase(Locale.ROOT);
        var bean = createBean(reportGenerator);
        var helper = getDefaultLabelMockHelper("validator.reportTitle", "validator.label.resultSubSectionOverviewTitle", "validator.label.resultSubSectionDetailsTitle",
                "validator.label.resultDateLabel", "validator.label.resultResultLabel", "validator.label.resultFileNameLabel",
                "validator.label.resultErrorsLabel", "validator.label.resultWarningsLabel", "validator.label.resultMessagesLabel" ,
                "validator.label.resultTestLabel", "validator.label.resultLocationLabel", "validator.label.pageLabel",
                "validator.label.ofLabel", "validator.label.result."+successText);
        var result = bean.getReportLabels(helper, TestResultType.SUCCESS);
        assertNotNull(result);
        assertEquals("validator.reportTitle_translated", result.getTitle());
        assertEquals("validator.label.resultSubSectionOverviewTitle_translated", result.getOverview());
        assertEquals("validator.label.resultSubSectionDetailsTitle_translated", result.getDetails());
        assertEquals("validator.label.resultDateLabel_translated", result.getDate());
        assertEquals("validator.label.resultResultLabel_translated", result.getResult());
        assertEquals("validator.label.resultFileNameLabel_translated", result.getFileName());
        assertEquals("validator.label.resultErrorsLabel_translated", result.getErrors());
        assertEquals("validator.label.resultWarningsLabel_translated", result.getWarnings());
        assertEquals("validator.label.resultMessagesLabel_translated", result.getMessages());
        assertEquals("validator.label.resultTestLabel_translated", result.getTest());
        assertEquals("validator.label.resultLocationLabel_translated", result.getLocation());
        assertEquals("validator.label.pageLabel_translated", result.getPage());
        assertEquals("validator.label.ofLabel_translated", result.getOf());
        assertEquals("validator.label.result."+successText+"_translated", result.getResultType());
    }

    private LocalisationHelper getDefaultLabelMockHelper(String... keys) {
        var helper = mock(LocalisationHelper.class);
        for (var key: keys) {
            when(helper.localise(key)).thenReturn(key+"_translated");
        }
        return helper;
    }

}
