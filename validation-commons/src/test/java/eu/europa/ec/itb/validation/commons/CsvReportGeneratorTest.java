package eu.europa.ec.itb.validation.commons;


import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static eu.europa.ec.itb.validation.commons.CsvReportGenerator.UTF8_BOM;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CsvReportGeneratorTest extends BaseTest {

    private CsvReportGenerator reportGenerator;

    private CsvReportGenerator createBean(ApplicationConfig appConfig) {
        var bean = new CsvReportGenerator();
        bean.appConfig = appConfig;
        return bean;
    }

    private LocalisationHelper createLocaliser() {
        var localiser = mock(LocalisationHelper.class);
        when(localiser.localise("validator.label.csvHeaderLevel")).thenReturn("Level");
        when(localiser.localise("validator.label.csvHeaderDescription")).thenReturn("Description");
        when(localiser.localise("validator.label.csvHeaderLocation")).thenReturn("Location");
        when(localiser.localise("validator.label.csvHeaderTest")).thenReturn("Test");
        when(localiser.localise("validator.label.csvHeaderAdditionalInfo")).thenReturn("Additional info");
        return localiser;
    }

    private DomainConfig createDomainConfig(boolean addBOM) {
        var domainConfig = new DomainConfig();
        domainConfig.setAddBOMToCSVExports(addBOM);
        return domainConfig;
    }

    private ApplicationConfig createAppConfig(boolean supportTests, boolean supportAdditionalInfo) {
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.isSupportsAdditionalInformationInReportItems()).thenReturn(supportAdditionalInfo);
        when(appConfig.isSupportsTestDefinitionInReportItems()).thenReturn(supportTests);
        return appConfig;
    }

    @Test
    void testWriteReportWithFile() throws IOException {
        var tarFile = Path.of(tmpFolder.toString(), "tarFile.xml");
        Files.copy(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/tarFile.xml")), tarFile);
        var reporter = createBean(createAppConfig(true, true));
        var csvFile = Path.of(tmpFolder.toString(), "report.csv");
        // Test
        reporter.writeReport(tarFile.toFile(), csvFile.toFile(), createLocaliser(), createDomainConfig(true));
        assertTrue(Files.exists(csvFile));
        var lines = Files.readAllLines(csvFile);
        assertEquals(6, lines.size());
        assertEquals(UTF8_BOM + "Level,Description,Location,Test,Additional info", lines.get(0));
    }

    @Test
    void testWriteReportWithFileNoExtraFieldsSupported() throws IOException {
        var tarFile = Path.of(tmpFolder.toString(), "tarFile.xml");
        Files.copy(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/tarFile.xml")), tarFile);
        var reporter = createBean(createAppConfig(false, false));
        var csvFile = Path.of(tmpFolder.toString(), "report.csv");
        // Test
        reporter.writeReport(tarFile.toFile(), csvFile.toFile(), createLocaliser(), createDomainConfig(true));
        assertTrue(Files.exists(csvFile));
        var lines = Files.readAllLines(csvFile);
        assertEquals(6, lines.size());
        assertEquals(UTF8_BOM + "Level,Description,Location", lines.get(0));
   }

    @Test
    void testWriteReportWithFileNoExtraFieldsPresent() throws IOException {
        var tarFile = Path.of(tmpFolder.toString(), "tarFile.xml");
        Files.copy(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/tarFileNoTestsNoAssertions.xml")), tarFile);
        var reporter = createBean(createAppConfig(true, true));
        var csvFile = Path.of(tmpFolder.toString(), "report.csv");
        // Test
        reporter.writeReport(tarFile.toFile(), csvFile.toFile(), createLocaliser(), createDomainConfig(true));
        assertTrue(Files.exists(csvFile));
        var lines = Files.readAllLines(csvFile);
        assertEquals(6, lines.size());
        assertEquals(UTF8_BOM + "Level,Description,Location", lines.get(0));
    }

    @Test
    void testWriteReportWithFileNoExtraFieldsPresentNoBOM() throws IOException {
        var tarFile = Path.of(tmpFolder.toString(), "tarFile.xml");
        Files.copy(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/tarFileNoTestsNoAssertions.xml")), tarFile);
        var reporter = createBean(createAppConfig(true, true));
        var csvFile = Path.of(tmpFolder.toString(), "report.csv");
        // Test
        reporter.writeReport(tarFile.toFile(), csvFile.toFile(), createLocaliser(), createDomainConfig(false));
        assertTrue(Files.exists(csvFile));
        var lines = Files.readAllLines(csvFile);
        assertEquals(6, lines.size());
        assertEquals("Level,Description,Location", lines.get(0));
    }

    @Test
    void testWriteError() throws IOException {
        var tarFile = Path.of(tmpFolder.toString(), "tarFile.xml");
        Files.copy(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/tarFile.xml")), tarFile);
        var reporter = createBean(createAppConfig(true, true));
        var tarFileAsFile = tarFile.toFile();
        var tmpFolderAsFile = tmpFolder.toFile();
        var localiser = createLocaliser();
        var domainConfig = createDomainConfig(true);
        var exception = assertThrows(ValidatorException.class, () -> reporter.writeReport(tarFileAsFile, tmpFolderAsFile, localiser, domainConfig));
        assertEquals("validator.label.exception.unableToGenerateCSVReport", exception.getMessage());
    }

}