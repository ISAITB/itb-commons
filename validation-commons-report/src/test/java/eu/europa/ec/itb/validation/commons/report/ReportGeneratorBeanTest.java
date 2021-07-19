package eu.europa.ec.itb.validation.commons.report;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.VoidAnswer1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    void testWriteReportFromObject() throws Exception {
        var domainConfig = new DomainConfig();
        domainConfig.setReportTitle("Title1");
        var tar = new TAR();
        var outputFile = Path.of(tmpPath.toString(), "report.pdf");
        var reportGenerator = mock(ReportGenerator.class);
        doAnswer((Answer<?>) invocation -> {
            assertSame(tar, invocation.getArgument(0));
            assertEquals(domainConfig.getReportTitle(), invocation.getArgument(1));
            assertTrue(invocation.getArgument(2) instanceof FileOutputStream);
            return null;
        }).when(reportGenerator).writeTARReport(any(TAR.class), any(), any());
        var bean = createBean(reportGenerator);
        assertDoesNotThrow(() -> bean.writeReport(domainConfig, tar, outputFile.toFile()));
        verify(reportGenerator, times(1)).writeTARReport(any(TAR.class), any(), any());
    }

    @Test
    void testWriteReportFromObjectError() throws Exception {
        var domainConfig = new DomainConfig();
        domainConfig.setReportTitle("Title1");
        var tar = new TAR();
        var outputFile = Path.of(tmpPath.toString(), "report.pdf");
        var reportGenerator = mock(ReportGenerator.class);
        doAnswer((Answer<?>) invocation -> {
            assertSame(tar, invocation.getArgument(0));
            assertEquals(domainConfig.getReportTitle(), invocation.getArgument(1));
            assertTrue(invocation.getArgument(2) instanceof FileOutputStream);
            throw new IllegalStateException();
        }).when(reportGenerator).writeTARReport(any(TAR.class), any(), any());
        var bean = createBean(reportGenerator);
        assertThrows(ValidatorException.class, () -> bean.writeReport(domainConfig, tar, outputFile.toFile()));
        verify(reportGenerator, times(1)).writeTARReport(any(TAR.class), any(), any());
    }


    @Test
    void testWriteReportFromFile() throws Exception {
        var domainConfig = new DomainConfig();
        domainConfig.setReportTitle("Title1");
        var inputFile = Path.of(tmpPath.toString(), "report.xml");
        Files.createFile(inputFile);
        Files.writeString(inputFile, "<tar></tar>");
        var outputFile = Path.of(tmpPath.toString(), "report.pdf");
        var reportGenerator = mock(ReportGenerator.class);
        doAnswer((Answer<?>) invocation -> {
            assertTrue(invocation.getArgument(0) instanceof FileInputStream);
            assertEquals(domainConfig.getReportTitle(), invocation.getArgument(1));
            assertTrue(invocation.getArgument(2) instanceof FileOutputStream);
            return null;
        }).when(reportGenerator).writeTARReport(any(FileInputStream.class), any(), any());
        var bean = createBean(reportGenerator);
        assertDoesNotThrow(() -> bean.writeReport(domainConfig, inputFile.toFile(), outputFile.toFile()));
        verify(reportGenerator, times(1)).writeTARReport(any(FileInputStream.class), any(), any());
    }

    @Test
    void testWriteReportFromFileError() throws Exception {
        var domainConfig = new DomainConfig();
        domainConfig.setReportTitle("Title1");
        var inputFile = Path.of(tmpPath.toString(), "report.xml");
        Files.createFile(inputFile);
        Files.writeString(inputFile, "<tar></tar>");
        var outputFile = Path.of(tmpPath.toString(), "report.pdf");
        var reportGenerator = mock(ReportGenerator.class);
        doAnswer((Answer<?>) invocation -> {
            assertTrue(invocation.getArgument(0) instanceof FileInputStream);
            assertEquals(domainConfig.getReportTitle(), invocation.getArgument(1));
            assertTrue(invocation.getArgument(2) instanceof FileOutputStream);
            throw new IllegalStateException();
        }).when(reportGenerator).writeTARReport(any(FileInputStream.class), any(), any());
        var bean = createBean(reportGenerator);
        assertThrows(ValidatorException.class, () -> bean.writeReport(domainConfig, inputFile.toFile(), outputFile.toFile()));
        verify(reportGenerator, times(1)).writeTARReport(any(FileInputStream.class), any(), any());
    }

}
