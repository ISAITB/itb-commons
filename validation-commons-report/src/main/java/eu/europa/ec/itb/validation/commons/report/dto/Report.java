package eu.europa.ec.itb.validation.commons.report.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for the information to include in a PDF report.
 */
public class Report {

    private String title;
    private String reportDate;
    private String reportResult;
    private String errorCount;
    private String warningCount;
    private String messageCount;
    private List<ReportItem> reportItems = new ArrayList<>();
    private List<ContextItem> contextItems = new ArrayList<>();

    /**
     * Constructor.
     */
    public Report() {
        this(null, null, null, null, null, null);
    }

    /**
     * Constructor.
     *
     * @param title The report's title.
     * @param reportDate The report's date.
     * @param reportResult The overall report result.
     * @param errorCount The number of errors.
     * @param warningCount The number of warnings.
     * @param messageCount The number of information messages.
     */
    public Report(String title, String reportDate, String reportResult, String errorCount, String warningCount, String messageCount) {
        this.title = title;
        this.reportDate = reportDate;
        this.reportResult = reportResult;
        this.errorCount = errorCount;
        this.warningCount = warningCount;
        this.messageCount = messageCount;
    }

    /**
     * @return The title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title The title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return The date.
     */
    public String getReportDate() {
        return reportDate;
    }

    /**
     * @param reportDate The date.
     */
    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    /**
     * @return The overall result.
     */
    public String getReportResult() {
        return reportResult;
    }

    /**
     * @param reportResult The overall result.
     */
    public void setReportResult(String reportResult) {
        this.reportResult = reportResult;
    }

    /**
     * @return The number of errors.
     */
    public String getErrorCount() {
        return errorCount;
    }

    /**
     * @param errorCount The number of errors.
     */
    public void setErrorCount(String errorCount) {
        this.errorCount = errorCount;
    }

    /**
     * @return The number of warnings.
     */
    public String getWarningCount() {
        return warningCount;
    }

    /**
     * @param warningCount The number of warnings.
     */
    public void setWarningCount(String warningCount) {
        this.warningCount = warningCount;
    }

    /**
     * @return The number of information messages.
     */
    public String getMessageCount() {
        return messageCount;
    }

    /**
     * @param messageCount The number of information messages.
     */
    public void setMessageCount(String messageCount) {
        this.messageCount = messageCount;
    }

    /**
     * @return The list of report items.
     */
    public List<ReportItem> getReportItems() {
        return reportItems;
    }

    /**
     * @param reportItems The list of report items.
     */
    public void setReportItems(List<ReportItem> reportItems) {
        this.reportItems = reportItems;
    }

    /**
     * @return The list of context items.
     */
    public List<ContextItem> getContextItems() {
        return contextItems;
    }

    /**
     * @param contextItems The list of context items.
     */
    public void setContextItems(List<ContextItem> contextItems) {
        this.contextItems = contextItems;
    }
}
