package eu.europa.ec.itb.validation.commons.web;

import eu.europa.ec.itb.validation.commons.BaseFileManager;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import eu.europa.ec.itb.validation.commons.report.ReportGeneratorBean;
import eu.europa.ec.itb.validation.commons.test.BaseTest;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import eu.europa.ec.itb.validation.commons.web.locale.CustomLocaleResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BaseFileControllerTest extends BaseTest {

    private ApplicationConfig appConfig;
    private BaseFileManager<ApplicationConfig> fileManager;
    private WebDomainConfigCache<WebDomainConfig> configCache;
    private ReportGeneratorBean reportGenerator;
    private Path reportFolder;
    private WebDomainConfig domainConfig;

    BaseFileController<BaseFileManager<ApplicationConfig>, ApplicationConfig, WebDomainConfigCache<WebDomainConfig>> createController(ApplicationConfig appConfig, BaseFileManager<ApplicationConfig> fileManager, WebDomainConfigCache<WebDomainConfig> domainConfigCache, ReportGeneratorBean reportGenerator) {
        var controller = new BaseFileController<BaseFileManager<ApplicationConfig>, ApplicationConfig, WebDomainConfigCache<WebDomainConfig>>() {
            @Override
            public String getInputFileName(String uuid) {
                return uuid + ".txt";
            }
            @Override
            public String getReportFileNameXml(String uuid) {
                return uuid + ".xml";
            }
            @Override
            public String getReportFileNamePdf(String uuid) {
                return uuid + ".pdf";
            }
            @Override
            public String getReportFileNameCsv(String uuid) {
                return uuid + ".csv";
            }
        };
        controller.config = appConfig;
        controller.fileManager = fileManager;
        controller.domainConfigCache = domainConfigCache;
        controller.reportGenerator = reportGenerator;
        controller.localeResolver = mock(CustomLocaleResolver.class);
        return controller;
    }

    @BeforeEach
    @Override
    protected void setup() throws IOException {
        super.setup();
        domainConfig = mock(WebDomainConfig.class);
        when(domainConfig.getChannels()).thenReturn(Set.of(ValidatorChannel.FORM));
        appConfig = mock(ApplicationConfig.class);
        fileManager = mock(BaseFileManager.class);
        configCache = mock(WebDomainConfigCache.class);
        when(configCache.getConfigForDomainName(any())).thenAnswer((Answer<?>) invocation -> domainConfig);
        reportGenerator = mock(ReportGeneratorBean.class);
        reportFolder = Path.of(tmpFolder.toString(), "reports");
        when(fileManager.getReportFolder()).thenReturn(reportFolder.toFile());
    }

    @AfterEach
    @Override
    protected void teardown() {
        super.teardown();
        reset(appConfig, fileManager, configCache, reportGenerator);
    }

    @Test
    void testGetInput() throws IOException {
        var reportUuid = "UUID1";
        var inputFile = createFileWithContents(Path.of(reportFolder.toString(), reportUuid+".txt"), "");
        var controller = createController(appConfig, fileManager, configCache, reportGenerator);
        var result = controller.getInput("domain1", reportUuid);
        assertNotNull(result);
        assertEquals(inputFile, result.getFile().toPath());
    }

    @Test
    void testGetInputFileNotExists() {
        var controller = createController(appConfig, fileManager, configCache, reportGenerator);
        assertThrows(NotFoundException.class, () -> controller.getInput("domain1", "UUID"));
    }

    @Test
    void testGetReportXml() throws IOException {
        var servletResponse = mock(HttpServletResponse.class);
        var reportUuid = "UUID1";
        var inputFile = createFileWithContents(Path.of(reportFolder.toString(), reportUuid+".xml"), "");
        var controller = createController(appConfig, fileManager, configCache, reportGenerator);
        var result = controller.getReportXml("domain1", reportUuid, servletResponse);
        assertNotNull(result);
        assertEquals(inputFile, result.getFile().toPath());
        verify(servletResponse, times(1)).setHeader("Content-Disposition", "attachment; filename=report_"+reportUuid+".xml");
    }

    @Test
    void testGetReportXmlFileNotExists() {
        var servletResponse = mock(HttpServletResponse.class);
        var controller = createController(appConfig, fileManager, configCache, reportGenerator);
        assertThrows(NotFoundException.class, () -> controller.getReportXml("domain1", "UUID", servletResponse));
    }

    @Test
    void testGetReportPdf() throws IOException {
        var servletRequest = mock(HttpServletRequest.class);
        var servletResponse = mock(HttpServletResponse.class);
        var reportUuid = "UUID1";
        var xmlFile = createFileWithContents(Path.of(reportFolder.toString(), reportUuid+".xml"), "");
        var expectedPdfFile = Path.of(fileManager.getReportFolder().toPath().toString(), reportUuid+".pdf");
        var controller = createController(appConfig, fileManager, configCache, reportGenerator);
        var result = controller.getReportPdf("domain1", reportUuid, servletRequest, servletResponse);
        assertNotNull(result);
        assertEquals(expectedPdfFile, result.getFile().toPath());
        verify(servletResponse, times(1)).setHeader("Content-Disposition", "attachment; filename=report_"+reportUuid+".pdf");
        verify(reportGenerator, times(1)).writeReport(eq(xmlFile.toFile()), eq(expectedPdfFile.toFile()), any(LocalisationHelper.class));
    }

    @Test
    void testGetReportPdfFileNotExists() {
        var servletRequest = mock(HttpServletRequest.class);
        var servletResponse = mock(HttpServletResponse.class);
        var controller = createController(appConfig, fileManager, configCache, reportGenerator);
        assertThrows(NotFoundException.class, () -> controller.getReportPdf("domain1", "UUID", servletRequest, servletResponse));
    }

    @Test
    void testDeleteReport() throws IOException {
        var xmlReport = createFileWithContents(Path.of(reportFolder.toString(), "uuid.xml"), "TEST");
        var pdfReport = createFileWithContents(Path.of(reportFolder.toString(), "uuid.pdf"), "TEST");
        var controller = createController(appConfig, fileManager, configCache, reportGenerator);
        controller.deleteReport("domain1", "uuid");
        assertTrue(Files.notExists(pdfReport));
        assertTrue(Files.notExists(xmlReport));
    }

    @Test
    void testDeleteInput() throws IOException {
        var inputFile = createFileWithContents(Path.of(reportFolder.toString(), "uuid.txt"), "TEST");
        var controller = createController(appConfig, fileManager, configCache, reportGenerator);
        controller.deleteInput("domain1", "uuid");
        assertTrue(Files.notExists(inputFile));
    }

    @Test
    void testDomainNotExists() {
        var configCache = mock(WebDomainConfigCache.class);
        when(configCache.getConfigForDomainName(any())).thenReturn(null);
        var controller = createController(appConfig, fileManager, configCache, reportGenerator);
        assertThrows(NotFoundException.class, () -> controller.getInput("domain1", "UUID"));
        assertThrows(NotFoundException.class, () -> controller.getReportXml("domain1", "UUID", mock(HttpServletResponse.class)));
        assertThrows(NotFoundException.class, () -> controller.getReportPdf("domain1", "UUID", mock(HttpServletRequest.class), mock(HttpServletResponse.class)));
        assertThrows(NotFoundException.class, () -> controller.deleteReport("domain1", "UUID"));
        assertThrows(NotFoundException.class, () -> controller.deleteInput("domain1", "UUID"));
    }

    @Test
    void testChannelNotSupported() {
        when(configCache.getConfigForDomainName(any())).thenAnswer((Answer<?>) invocation -> {
            var config = mock(WebDomainConfig.class);
            when(config.getChannels()).thenReturn(Set.of(ValidatorChannel.SOAP_API));
            return config;
        });
        var controller = createController(appConfig, fileManager, configCache, reportGenerator);
        assertThrows(NotFoundException.class, () -> controller.getInput("domain1", "UUID"));
        assertThrows(NotFoundException.class, () -> controller.getReportXml("domain1", "UUID", mock(HttpServletResponse.class)));
        assertThrows(NotFoundException.class, () -> controller.getReportPdf("domain1", "UUID", mock(HttpServletRequest.class), mock(HttpServletResponse.class)));
        assertThrows(NotFoundException.class, () -> controller.deleteReport("domain1", "UUID"));
        assertThrows(NotFoundException.class, () -> controller.deleteInput("domain1", "UUID"));
    }

}
