package eu.europa.ec.itb.validation.commons.web;

import com.gitb.reports.ReportGenerator;
import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

@Component
public class ReportGeneratorBean {

    private ReportGenerator reportGenerator = new ReportGenerator();

    public void writeReport(WebDomainConfig config, File inFile, File outFile) {
        try (FileInputStream fis = new FileInputStream(inFile); FileOutputStream fos = new FileOutputStream(outFile)) {
            reportGenerator.writeTARReport(fis, config.getReportTitle(), fos);
        } catch (Exception e) {
            throw new ValidatorException("Unable to generate PDF report", e);
        }
    }

    public void writeReport(WebDomainConfig config, TAR report, File outFile) {
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            reportGenerator.writeTARReport(report, config.getReportTitle(), fos);
        } catch (Exception e) {
            throw new ValidatorException("Unable to generate PDF report", e);
        }
    }

}
