package eu.europa.ec.itb.validation.commons.web.dto;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.config.ErrorResponseTypeEnum;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Class used to collect the values for the result of a validation via the user interface. This class is serialised
 * to JSON as the response to the validation call.
 *
 * @param <T> The specific translation type for the validator in question.
 */
public class UploadResult <T extends Translations> {

    private String message;
    private boolean messageIsError = true;
    private String reportId;
    private String fileName;
    private TAR report;
    private TAR aggregateReport;
    private boolean showAggregateReport;
    private String date;
    private String validationTypeLabel;
    private Long maximumReportsForDetailedOutput;
    private Long maximumReportsForXmlOutput;
    private String resultValue;
    private T translations;
    private List<String> additionalErrorMessages;

    /**
     * @return The (optional) error message to display.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message The (optional) error message to display.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return The identifier of the validation report to be used in relevant REST calls.
     */
    public String getReportId() {
        return reportId;
    }

    /**
     * @param reportId The identifier of the validation report to be used in relevant REST calls.
     */
    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    /**
     * @return The file name to display on the user interface for the validated input.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName The file name to display on the user interface for the validated input.
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return The detailed TAR validation report.
     */
    public TAR getReport() {
        return report;
    }

    /**
     * @param report The detailed TAR validation report.
     */
    public void setReport(TAR report) {
        this.report = report;
    }

    /**
     * @return The aggregated TAR validation report.
     */
    public TAR getAggregateReport() {
        return aggregateReport;
    }

    /**
     * @param aggregateReport The aggregated TAR validation report.
     */
    public void setAggregateReport(TAR aggregateReport) {
        this.aggregateReport = aggregateReport;
    }

    /**
     * @return Whether to display the aggregate report.
     */
    public boolean isShowAggregateReport() {
        return showAggregateReport;
    }

    /**
     * @param showAggregateReport Whether to display the aggregate report.
     */
    public void setShowAggregateReport(boolean showAggregateReport) {
        this.showAggregateReport = showAggregateReport;
    }

    /**
     * @return The validation timestamp.
     */
    public String getDate() {
        return date;
    }

    /**
     * @param date The validation timestamp.
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * @return The complete label to display for the type of validation performed.
     */
    public String getValidationTypeLabel() {
        return validationTypeLabel;
    }

    /**
     * @param validationTypeLabel The complete label to display for the type of validation performed.
     */
    public void setValidationTypeLabel(String validationTypeLabel) {
        this.validationTypeLabel = validationTypeLabel;
    }

    /**
     * @return The maximum number of report items allowed in the on-screen report.
     */
    public Long getMaximumReportsForDetailedOutput() {
        return maximumReportsForDetailedOutput;
    }

    /**
     * @param maximumReportsForDetailedOutput The maximum number of report items allowed in the on-screen report.
     */
    public void setMaximumReportsForDetailedOutput(Long maximumReportsForDetailedOutput) {
        this.maximumReportsForDetailedOutput = maximumReportsForDetailedOutput;
    }

    /**
     * @return The maximum number of report items included in the XML validation report.
     */
    public Long getMaximumReportsForXmlOutput() {
        return maximumReportsForXmlOutput;
    }

    /**
     * @param maximumReportsForXmlOutput The maximum number of report items included in the XML validation report.
     */
    public void setMaximumReportsForXmlOutput(Long maximumReportsForXmlOutput) {
        this.maximumReportsForXmlOutput = maximumReportsForXmlOutput;
    }

    /**
     * @return The translation texts to use in the user interface.
     */
    public T getTranslations() {
        return translations;
    }

    /**
     * @param translations The translation texts to use in the user interface.
     */
    public void setTranslations(T translations) {
        this.translations = translations;
    }

    /**
     * @return Whether the configured message is at error level.
     */
    public boolean isMessageIsError() {
        return messageIsError;
    }

    /**
     * @param messageIsError Whether the configured message is at error level.
     */
    public void setMessageIsError(boolean messageIsError) {
        this.messageIsError = messageIsError;
    }

    /**
     * @return The list of additional (hidden) error messages to report.
     */
    public List<String> getAdditionalErrorMessages() {
        return additionalErrorMessages;
    }

    /**
     * @param additionalErrorMessages The list of additional (hidden) error messages to report.
     */
    public void setAdditionalErrorMessages(List<String> additionalErrorMessages) {
        this.additionalErrorMessages = additionalErrorMessages;
    }

    /**
     * @return The result value (non-translated).
     */
    public String getResultValue() {
        return resultValue;
    }

    /**
     * @param resultValue The result value (non-translated).
     */
    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    /**
     * Populate all result properties that are common to all validator types.
     *
     * @param helper The localisation helper to use to lookup translations.
     * @param validationType The current validation type.
     * @param domainConfig The domain configuration.
     * @param reportId The report identifier.
     * @param fileName The fime name to display.
     * @param detailedReport The detailed TAR report.
     * @param aggregateReport The aggregated report.
     * @param translations The translations to use.
     */
    public void populateCommon(LocalisationHelper helper, String validationType, WebDomainConfig domainConfig, String reportId,
                               String fileName, TAR detailedReport, TAR aggregateReport, T translations) {
        if (StringUtils.isNotBlank(validationType)) {
            setValidationTypeLabel(domainConfig.getCompleteTypeOptionLabel(validationType, helper));
        }
        setMaximumReportsForDetailedOutput(domainConfig.getMaximumReportsForDetailedOutput());
        setMaximumReportsForXmlOutput(domainConfig.getMaximumReportsForXmlOutput());
        setReportId(reportId);
        setFileName(fileName);
        setReport(detailedReport);
        setAggregateReport(aggregateReport);
        setShowAggregateReport(Utils.aggregateDiffers(detailedReport, aggregateReport));
        setDate(detailedReport.getDate().toString());
        setResultValue(detailedReport.getResult().value());
        setTranslations(translations);
        if (message == null && !domainConfig.checkRemoteArtefactStatus(validationType) && domainConfig.getResponseForRemoteArtefactLoadFailure(validationType) == ErrorResponseTypeEnum.WARN) {
            // We only treat the case where we need to report a warning. When needing to respond with an error this has already
            // been done before validation took place.
            setMessage(helper.localise("validator.label.exception.failureToLoadRemoteArtefacts"));
            setMessageIsError(false);
        }
    }
}
