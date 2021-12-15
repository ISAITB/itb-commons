package eu.europa.ec.itb.validation.commons.web;

import com.gitb.tr.TestResultType;
import eu.europa.ec.itb.validation.commons.BaseFileManager;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import eu.europa.ec.itb.validation.commons.report.ReportGeneratorBean;
import eu.europa.ec.itb.validation.commons.report.dto.ReportLabels;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import eu.europa.ec.itb.validation.commons.web.locale.CustomLocaleResolver;
import org.apache.commons.io.FileUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Locale;

/**
 * Base class for web controllers managing access to the validator's reports.
 */
public abstract class BaseFileController<T extends BaseFileManager, R extends ApplicationConfig, Z extends WebDomainConfigCache> {

    @Autowired
    protected R config;
    @Autowired
    protected T fileManager;
    @Autowired
    protected Z domainConfigCache;
    @Autowired
    protected ReportGeneratorBean reportGenerator;
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
     * @return The file name.
     */
    public abstract String getReportFileNameXml(String uuid);

    /**
     * Get the PDF report file name for a given file ID part.
     *
     * @param uuid The variable ID part.
     * @return The file name.
     */
    public abstract String getReportFileNamePdf(String uuid);

    /**
     * Get the input file that was used for the validation.
     *
     * @param domain The domain name.
     * @param id The unique ID for the input file.
     * @return The file.
     */
    @RequestMapping(value = "/{domain}/input/{id}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public FileSystemResource getInput(@PathVariable String domain, @PathVariable String id) {
        WebDomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
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
     * @param response the HTTP response.
     * @return The file.
     */
    @RequestMapping(value = "/{domain}/report/{id}/xml", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public FileSystemResource getReportXml(@PathVariable String domain, @PathVariable String id, HttpServletResponse response) {
        WebDomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        File reportFile = new File(fileManager.getReportFolder(), getReportFileNameXml(id));
        if (reportFile.exists() && reportFile.isFile()) {
            if (response != null) {
                response.setHeader("Content-Disposition", "attachment; filename=report_"+id+".xml");
            }
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
     * @param request the HTTP request.
     * @param response the HTTP response.
     * @return The file.
     */
    @RequestMapping(value = "/{domain}/report/{id}/pdf", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public FileSystemResource getReportPdf(@PathVariable String domain, @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
        WebDomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        File reportFile = new File(fileManager.getReportFolder(), getReportFileNamePdf(id));
        if (!(reportFile.exists() && reportFile.isFile())) {
            // Generate the PDF.
            File reportFileXml = new File(fileManager.getReportFolder(), getReportFileNameXml(id));
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
            response.setHeader("Content-Disposition", "attachment; filename=report_"+id+".pdf");
        }
        return new FileSystemResource(reportFile);
    }

    /**
     * Delete the XML and PDF validation reports matching a specific ID.
     *
     * @param domain The domain name.
     * @param id The report files' ID.
     */
    @RequestMapping(value = "/{domain}/report/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteReport(@PathVariable String domain, @PathVariable String id) {
        WebDomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        File reportFile = new File(fileManager.getReportFolder(), getReportFileNameXml(id));
        if (reportFile.exists() && reportFile.isFile()) {
            FileUtils.deleteQuietly(reportFile);
        }
        reportFile = new File(fileManager.getReportFolder(), getReportFileNamePdf(id));
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
    @RequestMapping(value = "/{domain}/input/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteInput(@PathVariable String domain, @PathVariable String id) {
        WebDomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        File reportFile = new File(fileManager.getReportFolder(), getInputFileName(id));
        if (reportFile.exists() && reportFile.isFile()) {
            FileUtils.deleteQuietly(reportFile);
        }
    }

}
