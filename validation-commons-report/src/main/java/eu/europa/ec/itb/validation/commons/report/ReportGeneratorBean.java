package eu.europa.ec.itb.validation.commons.report;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.report.dto.ReportLabels;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Locale;
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
     * @param helper The localisation helper to use for the report's labels.
     */
    public void writeReport(File inFile, File outFile, LocalisationHelper helper) {
        writeReport(inFile, outFile, tar -> getReportLabels(helper, tar));
    }

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
     * @param helper The localisation helper to use for the report's labels.
     */
    public void writeReport(TAR report, File outFile, LocalisationHelper helper) {
        writeReport(report, outFile, tar -> getReportLabels(helper, tar));
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

    /**
     * Get the labels to use in PDF reports.
     *
     * @param helper The localisation helper.
     * @param report The report to consider.
     * @return The labels.
     */
    public ReportLabels getReportLabels(LocalisationHelper helper, TAR report) {
        var reportLabels = new ReportLabels();
        reportLabels.setTitle(helper.localise("validator.reportTitle"));
        reportLabels.setOverview(helper.localise("validator.label.resultSubSectionOverviewTitle"));
        reportLabels.setDetails(helper.localise("validator.label.resultSubSectionDetailsTitle"));
        reportLabels.setDate(helper.localise("validator.label.resultDateLabel"));
        reportLabels.setResult(helper.localise("validator.label.resultResultLabel"));
        reportLabels.setFileName(helper.localise("validator.label.resultFileNameLabel"));
        reportLabels.setTest(helper.localise("validator.label.resultTestLabel"));
        reportLabels.setLocation(helper.localise("validator.label.resultLocationLabel"));
        reportLabels.setPage(helper.localise("validator.label.pageLabel"));
        reportLabels.setOf(helper.localise("validator.label.ofLabel"));
        reportLabels.setAssertionId(helper.localise("validator.label.additionalInfoLabel"));
        reportLabels.setResultType(helper.localise("validator.label.result."+report.getResult().value().toLowerCase(Locale.ROOT)));
        reportLabels.setFindings(helper.localise("validator.label.resultFindingsLabel"));
        reportLabels.setFindingsDetails(helper.localise("validator.label.resultFindingsDetailsLabel",
                ((report.getCounters() != null && report.getCounters().getNrOfErrors() != null)?report.getCounters().getNrOfErrors().intValue():0),
                ((report.getCounters() != null && report.getCounters().getNrOfWarnings() != null)?report.getCounters().getNrOfWarnings().intValue():0),
                ((report.getCounters() != null && report.getCounters().getNrOfAssertions() != null)?report.getCounters().getNrOfAssertions().intValue():0)
        ));
        return reportLabels;
    }

}
