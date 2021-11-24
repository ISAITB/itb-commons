package eu.europa.ec.itb.validation.commons.report;

import com.gitb.tr.TAR;
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

}
