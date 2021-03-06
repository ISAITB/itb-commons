package eu.europa.ec.itb.validation.commons;

import com.gitb.tr.TAR;

import java.util.Objects;

/**
 * Wrapper class to hold the pair of a validation's TAR reports.
 */
public class ReportPair {

    private final TAR detailedReport;
    private final TAR aggregateReport;

    /**
     * Constructor.
     *
     * @param detailedReport The detailed TAR report.
     * @param aggregateReport The aggregate TAR report.
     */
    public ReportPair(TAR detailedReport, TAR aggregateReport) {
        Objects.requireNonNull(detailedReport, "Detailed report must always be provided");
        this.detailedReport = detailedReport;
        this.aggregateReport = aggregateReport;
    }

    /**
     * @return The detailed TAR report.
     */
    public TAR getDetailedReport() {
        return detailedReport;
    }

    /**
     * @return The aggregate TAR report.
     */
    public TAR getAggregateReport() {
        return aggregateReport;
    }
}
