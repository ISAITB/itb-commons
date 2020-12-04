package eu.europa.ec.itb.validation.commons.web;

import eu.europa.ec.itb.validation.commons.BaseFileManager;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import eu.europa.ec.itb.validation.commons.report.ReportGeneratorBean;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * Created by simatosc on 08/03/2016.
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

    public abstract String getInputFileName(String uuid);
    public abstract String getReportFileNameXml(String uuid);
    public abstract String getReportFileNamePdf(String uuid);

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

    @RequestMapping(value = "/{domain}/report/{id}/pdf", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public FileSystemResource getReportPdf(@PathVariable String domain, @PathVariable String id, HttpServletResponse response) {
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
                reportGenerator.writeReport(domainConfig, reportFileXml, reportFile);
            } else {
                throw new NotFoundException();
            }
        }
        if (response != null) {
            response.setHeader("Content-Disposition", "attachment; filename=report_"+id+".pdf");
        }
        return new FileSystemResource(reportFile);
    }

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
