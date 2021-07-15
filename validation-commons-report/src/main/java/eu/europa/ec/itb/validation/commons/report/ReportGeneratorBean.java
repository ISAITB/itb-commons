package eu.europa.ec.itb.validation.commons.report;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Spring component wrapper for the report generator.
 */
@Component
public class ReportGeneratorBean {

    private final ReportGenerator reportGenerator = new ReportGenerator();

    /**
     * Generate a TAR PDF report from the provided TAR XML report as input.
     *
     * @param config The domain's configuration.
     * @param inFile The input XML report.
     * @param outFile The output PDF report.
     */
    public void writeReport(DomainConfig config, File inFile, File outFile) {
        try (FileInputStream fis = new FileInputStream(inFile); FileOutputStream fos = new FileOutputStream(outFile)) {
            reportGenerator.writeTARReport(fis, config.getReportTitle(), fos);
        } catch (Exception e) {
            throw new ValidatorException("Unable to generate PDF report", e);
        }
    }

    /**
     * Generate a TAR PDF report from the provided TAR XML report as input.
     *
     * @param config The domain's configuration.
     * @param report The TAR report object as input.
     * @param outFile The output PDF report.
     */
    public void writeReport(DomainConfig config, TAR report, File outFile) {
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            reportGenerator.writeTARReport(report, config.getReportTitle(), fos);
        } catch (Exception e) {
            throw new ValidatorException("Unable to generate PDF report", e);
        }
    }

}
