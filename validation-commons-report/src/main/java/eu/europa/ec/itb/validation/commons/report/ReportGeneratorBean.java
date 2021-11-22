package eu.europa.ec.itb.validation.commons.report;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.report.dto.ReportLabels;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.function.Function;

/**
 * Spring component wrapper for the report generator.
 */
@Component
public class ReportGeneratorBean {

    private final ReportGenerator reportGenerator = new ReportGenerator();

    /**
     * Generate a TAR PDF report from the provided TAR XML report as input.
     *
     * @param inFile The input XML report.
     * @param outFile The output PDF report.
     * @param labelProvider A function to provide the labels to use in the report.
     */
    public void writeReport(File inFile, File outFile, Function<TAR, ReportLabels> labelProvider) {
        try (FileInputStream fis = new FileInputStream(inFile); FileOutputStream fos = new FileOutputStream(outFile)) {
            reportGenerator.writeTARReport(fis, fos, labelProvider);
        } catch (Exception e) {
            throw new ValidatorException("validator.label.exception.unableToGeneratePDFReport", e);
        }
    }

    /**
     * Generate a TAR PDF report from the provided TAR XML report as input.
     *
     * @param report The TAR report object as input.
     * @param outFile The output PDF report.
     * @param labelProvider A function to provide the labels to use in the report.
     */
    public void writeReport(TAR report, File outFile, Function<TAR, ReportLabels> labelProvider) {
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            reportGenerator.writeTARReport(report, fos, labelProvider);
        } catch (Exception e) {
            throw new ValidatorException("validator.label.exception.unableToGeneratePDFReport", e);
        }
    }

}
