package eu.europa.ec.itb.validation.commons.web;

import eu.europa.ec.itb.validation.commons.BaseFileManager;
import eu.europa.ec.itb.validation.commons.CsvReportGenerator;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import eu.europa.ec.itb.validation.commons.report.ReportGeneratorBean;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import eu.europa.ec.itb.validation.commons.web.locale.CustomLocaleResolver;
import org.apache.commons.io.FileUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

import static eu.europa.ec.itb.validation.commons.web.Constants.MDC_DOMAIN;

/**
 * Base class for web controllers managing access to the validator's reports.
 */
public abstract class BaseFileController<T extends BaseFileManager, R extends ApplicationConfig, Z extends WebDomainConfigCache> {

    private static final String CONTENT_DISPOSITION_ATTACHMENT_VALUE = "attachment; filename=report_%s.%s";

    @Autowired
    protected R config;
    @Autowired
    protected T fileManager;
    @Autowired
    protected Z domainConfigCache;
    @Autowired
    protected ReportGeneratorBean reportGenerator;
    @Autowired
    protected CsvReportGenerator csvReportGenerator;
    @Autowired
    protected CustomLocaleResolver localeResolver;
    @Autowired
    protected ApplicationConfig appConfig;

    /**
     * Get the input file name for a given file ID part.
     *
     * @param uuid The variable ID part.
     * @return The file name.
     */
    public abstract String getInputFileName(String uuid);

    /**
     * Get the XML report file name for a given file ID part.
     *
     * @param uuid The variable ID part.
     * @param aggregate Whether this is an aggregate report.
     * @return The file name.
     */
    public abstract String getReportFileNameXml(String uuid, boolean aggregate);

    /**
     * Get the PDF report file name for a given file ID part.
     *
     * @param uuid The variable ID part.
     * @param aggregate Whether this is an aggregate report.
     * @return The file name.
     */
    public abstract String getReportFileNamePdf(String uuid, boolean aggregate);

    /**
     * Get the CSV report file name for a given file ID part.
     *
     * @param uuid The variable ID part.
     * @param aggregate Whether this is an aggregate report.
     * @return The file name.
     */
    public abstract String getReportFileNameCsv(String uuid, boolean aggregate);

    /**
     * Get the input file that was used for the validation.
     *
     * @param domain The domain name.
     * @param id The unique ID for the input file.
     * @return The file.
     */
    @GetMapping(value = "/{domain}/input/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public FileSystemResource getInput(@PathVariable String domain, @PathVariable String id) {
        WebDomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put(MDC_DOMAIN, domain);
        File reportFile = new File(fileManager.getReportFolder(), getInputFileName(id));
        if (reportFile.exists() && reportFile.isFile()) {
            return new FileSystemResource(reportFile);
        } else {
            throw new NotFoundException();
        }
    }

    /**
     * Get the XML validation report file that was generated from the validation.
     *
     * @param domain The domain name.
     * @param id The unique ID for the input file.
     * @param aggregate Whether it is the aggregate version of the report that should be downloaded.
     * @param response the HTTP response.
     * @return The file.
     */
    @GetMapping(value = "/{domain}/report/{id}/xml", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public FileSystemResource getReportXml(@PathVariable String domain,
                                           @PathVariable String id,
                                           @RequestParam(defaultValue = "false") Boolean aggregate,
                                           HttpServletResponse response) {
        WebDomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put(MDC_DOMAIN, domain);
        File reportFile = new File(fileManager.getReportFolder(), getReportFileNameXml(id, aggregate));
        if (reportFile.exists() && reportFile.isFile()) {
            if (response != null) {
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_ATTACHMENT_VALUE, id, "xml"));
            }
            return new FileSystemResource(reportFile);
        } else {
            throw new NotFoundException();
        }
    }

    /**
     * Get the PDF validation report file that was generated from the validation.
     *
     * @param domain The domain name.
     * @param id The unique ID for the input file.
     * @param aggregate Whether it is the aggregate version of the report that should be downloaded.
     * @param request the HTTP request.
     * @param response the HTTP response.
     * @return The file.
     */
    @GetMapping(value = "/{domain}/report/{id}/pdf", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public FileSystemResource getReportPdf(@PathVariable String domain,
                                           @PathVariable String id,
                                           @RequestParam(defaultValue = "false") Boolean aggregate,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        WebDomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put(MDC_DOMAIN, domain);
        File reportFile = new File(fileManager.getReportFolder(), getReportFileNamePdf(id, aggregate));
        if (!(reportFile.exists() && reportFile.isFile())) {
            // Generate the PDF.
            File reportFileXml = new File(fileManager.getReportFolder(), getReportFileNameXml(id, aggregate));
            if (reportFileXml.exists() && reportFileXml.isFile()) {
                reportGenerator.writeReport(
                        reportFileXml,
                        reportFile,
                        new LocalisationHelper(domainConfig, localeResolver.resolveLocale(request, response, domainConfig, appConfig))
                );
            } else {
                throw new NotFoundException();
            }
        }
        if (response != null) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_ATTACHMENT_VALUE, id, "pdf"));
        }
        return new FileSystemResource(reportFile);
    }

    /**
     * Get the CSV validation report file that was generated from the validation.
     *
     * @param domain The domain name.
     * @param id The unique ID for the input file.
     * @param aggregate Whether it is the aggregate version of the report that should be downloaded.
     * @param request the HTTP request.
     * @param response the HTTP response.
     * @return The file.
     */
    @GetMapping(value = "/{domain}/report/{id}/csv", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public FileSystemResource getReportCsv(@PathVariable String domain,
                                           @PathVariable String id,
                                           @RequestParam(defaultValue = "false") Boolean aggregate,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        WebDomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put(MDC_DOMAIN, domain);
        File reportFile = new File(fileManager.getReportFolder(), getReportFileNameCsv(id, aggregate));
        if (!(reportFile.exists() && reportFile.isFile())) {
            // Generate the PDF.
            File reportFileXml = new File(fileManager.getReportFolder(), getReportFileNameXml(id, aggregate));
            if (reportFileXml.exists() && reportFileXml.isFile()) {
                csvReportGenerator.writeReport(reportFileXml, reportFile, new LocalisationHelper(domainConfig, localeResolver.resolveLocale(request, response, domainConfig, appConfig)), domainConfig);
            } else {
                throw new NotFoundException();
            }
        }
        if (response != null) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_ATTACHMENT_VALUE, id, "csv"));
        }
        return new FileSystemResource(reportFile);
    }

    /**
     * Delete the XML and PDF validation reports matching a specific ID.
     *
     * @param domain The domain name.
     * @param id The report files' ID.
     */
    @DeleteMapping(value = "/{domain}/report/{id}")
    @ResponseBody
    public void deleteReport(@PathVariable String domain,
                             @PathVariable String id) {
        WebDomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put(MDC_DOMAIN, domain);
        deleteSpecificFile(new File(fileManager.getReportFolder(), getReportFileNameXml(id, false)));
        deleteSpecificFile(new File(fileManager.getReportFolder(), getReportFileNameXml(id, true)));
        deleteSpecificFile(new File(fileManager.getReportFolder(), getReportFileNamePdf(id, false)));
        deleteSpecificFile(new File(fileManager.getReportFolder(), getReportFileNamePdf(id, true)));
        deleteSpecificFile(new File(fileManager.getReportFolder(), getReportFileNameCsv(id, false)));
        deleteSpecificFile(new File(fileManager.getReportFolder(), getReportFileNameCsv(id, true)));
    }

    /**
     * Delete the provided file.
     *
     * @param reportFile The file to delete.
     */
    private void deleteSpecificFile(File reportFile) {
        if (reportFile.exists() && reportFile.isFile()) {
            FileUtils.deleteQuietly(reportFile);
        }
    }

    /**
     * Delete the input file used for the validation.
     *
     * @param domain The domain name.
     * @param id The input file ID.
     */
    @DeleteMapping(value = "/{domain}/input/{id}")
    @ResponseBody
    public void deleteInput(@PathVariable String domain, @PathVariable String id) {
        WebDomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put(MDC_DOMAIN, domain);
        deleteSpecificFile(new File(fileManager.getReportFolder(), getInputFileName(id)));
    }

}
