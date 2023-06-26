package eu.europa.ec.itb.validation.commons.web.dto;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;

import java.util.Locale;

/**
 * Class to hold translations for display on the web UI following a validation.
 *
 * This class holds text translations that are common to all validators.
 */
public class Translations {

    private String resultSectionTitle;
    private String resultSubSectionOverviewTitle;
    private String resultDateLabel;
    private String resultFileNameLabel;
    private String resultValidationTypeLabel;
    private String resultResultLabel;
    private String resultValue;
    private String resultFindingsLabel;
    private String resultFindingsDetailsLabel;
    private String resultSubSectionDetailsTitle;
    private String viewReportItemsShowAll;
    private String viewReportItemsShowErrors;
    private String viewReportItemsShowWarnings;
    private String viewReportItemsShowMessages;
    private String viewReportItemsDetailed;
    private String viewReportItemsAggregated;
    private String maximumReportsExceededForDetailedOutputMessage;
    private String maximumReportsExceededForXmlOutputMessage;
    private String resultLocationLabel;
    private String resultTestLabel;
    private String additionalInfoLabel;
    private String popupCloseButton;
    private String popupTitle;
    private String reportDetailedPDF;
    private String reportDetailedCSV;
    private String reportAggregatedPDF;
    private String reportAggregatedCSV;
    private String viewAnnotatedInputButton;
    private String downloadReportButton;
    private String reportDetailedXML;
    private String reportAggregatedXML;
    private String viewDetailsButton;
    private String viewSummaryButton;

    /**
     * Constructor that set's all labels that are common for all validators.
     *
     * @param helper The helper class to facilitate localisation.
     * @param report The (detailed) TAR validation report.
     * @param domainConfig The relevant domain configuration.
     */
    public Translations(LocalisationHelper helper, TAR report, DomainConfig domainConfig) {
        setResultSectionTitle(helper.localise("validator.label.resultSectionTitle"));
        setResultValue(helper.localise("validator.label.result."+report.getResult().value().toLowerCase(Locale.ROOT)));
        setResultFindingsLabel(helper.localise("validator.label.resultFindingsLabel"));
        setResultFindingsDetailsLabel(helper.localise("validator.label.resultFindingsDetailsLabel", report.getCounters().getNrOfErrors().intValue(), report.getCounters().getNrOfAssertions().intValue(), report.getCounters().getNrOfAssertions().intValue()));
        setResultDateLabel(helper.localise("validator.label.resultDateLabel"));
        setResultFileNameLabel(helper.localise("validator.label.resultFileNameLabel"));
        setResultValidationTypeLabel(helper.localise("validator.label.resultValidationTypeLabel"));
        setResultResultLabel(helper.localise("validator.label.resultResultLabel"));
        setResultSubSectionDetailsTitle(helper.localise("validator.label.resultSubSectionDetailsTitle"));
        setResultSubSectionOverviewTitle(helper.localise("validator.label.resultSubSectionOverviewTitle"));
        setViewReportItemsShowAll(helper.localise("validator.label.viewReportItemsShowAll"));
        setViewReportItemsShowErrors(helper.localise("validator.label.viewReportItemsShowErrors"));
        setViewReportItemsShowWarnings(helper.localise("validator.label.viewReportItemsShowWarnings"));
        setViewReportItemsShowMessages(helper.localise("validator.label.viewReportItemsShowMessages"));
        setViewReportItemsDetailed(helper.localise("validator.label.viewReportItemsDetailed"));
        setViewReportItemsAggregated(helper.localise("validator.label.viewReportItemsAggregated"));
        setMaximumReportsExceededForDetailedOutputMessage(helper.localise("validator.label.maximumReportsExceededForDetailedOutputMessage"));
        setResultLocationLabel(helper.localise("validator.label.resultLocationLabel"));
        setResultTestLabel(helper.localise("validator.label.resultTestLabel"));
        setAdditionalInfoLabel(helper.localise("validator.label.additionalInfoLabel"));
        setPopupCloseButton(helper.localise("validator.label.popupCloseButton"));
        setPopupTitle(helper.localise("validator.label.popupTitle"));
        setReportDetailedPDF(helper.localise("validator.label.reportDetailedPDF"));
        setReportDetailedCSV(helper.localise("validator.label.reportDetailedCSV"));
        setReportAggregatedPDF(helper.localise("validator.label.reportAggregatedPDF"));
        setReportAggregatedCSV(helper.localise("validator.label.reportAggregatedCSV"));
        setViewAnnotatedInputButton(helper.localise("validator.label.viewAnnotatedInputButton"));
        setDownloadReportButton(helper.localise("validator.label.downloadReportButton"));
        setReportDetailedXML(helper.localise("validator.label.reportDetailedXML"));
        setReportAggregatedXML(helper.localise("validator.label.reportAggregatedXML"));
        setMaximumReportsExceededForXmlOutputMessage(helper.localise("validator.label.maximumReportsExceededForXmlOutputMessage", domainConfig.getMaximumReportsForXmlOutput()));
        setViewDetailsButton(helper.localise("validator.label.viewDetailsButton"));
        setViewSummaryButton(helper.localise("validator.label.viewSummaryButton"));
    }

    /**
     * @return The label value.
     */
    public String getResultSectionTitle() {
        return resultSectionTitle;
    }

    /**
     * @param resultSectionTitle The label value to set.
     */
    public void setResultSectionTitle(String resultSectionTitle) {
        this.resultSectionTitle = resultSectionTitle;
    }

    /**
     * @return The label value.
     */
    public String getResultSubSectionOverviewTitle() {
        return resultSubSectionOverviewTitle;
    }

    /**
     * @param resultSubSectionOverviewTitle The label value to set.
     */
    public void setResultSubSectionOverviewTitle(String resultSubSectionOverviewTitle) {
        this.resultSubSectionOverviewTitle = resultSubSectionOverviewTitle;
    }

    /**
     * @return The label value.
     */
    public String getResultDateLabel() {
        return resultDateLabel;
    }

    /**
     * @param resultDateLabel The label value to set.
     */
    public void setResultDateLabel(String resultDateLabel) {
        this.resultDateLabel = resultDateLabel;
    }

    /**
     * @return The label value.
     */
    public String getResultFileNameLabel() {
        return resultFileNameLabel;
    }

    /**
     * @param resultFileNameLabel The label value to set.
     */
    public void setResultFileNameLabel(String resultFileNameLabel) {
        this.resultFileNameLabel = resultFileNameLabel;
    }

    /**
     * @return The label value.
     */
    public String getResultValidationTypeLabel() {
        return resultValidationTypeLabel;
    }

    /**
     * @param resultValidationTypeLabel The label value to set.
     */
    public void setResultValidationTypeLabel(String resultValidationTypeLabel) {
        this.resultValidationTypeLabel = resultValidationTypeLabel;
    }

    /**
     * @return The label value.
     */
    public String getResultResultLabel() {
        return resultResultLabel;
    }

    /**
     * @param resultResultLabel The label value to set.
     */
    public void setResultResultLabel(String resultResultLabel) {
        this.resultResultLabel = resultResultLabel;
    }

    /**
     * @return The label value.
     */
    public String getResultValue() {
        return resultValue;
    }

    /**
     * @param resultValue The label value to set.
     */
    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    /**
     * @return The findings label.
     */
    public String getResultFindingsLabel() {
        return resultFindingsLabel;
    }

    /**
     * @param resultFindingsLabel The findings label.
     */
    public void setResultFindingsLabel(String resultFindingsLabel) {
        this.resultFindingsLabel = resultFindingsLabel;
    }

    /**
     * @return The findings value.
     */
    public String getResultFindingsDetailsLabel() {
        return resultFindingsDetailsLabel;
    }

    /**
     * @param resultFindingsDetailsLabel The findings value.
     */
    public void setResultFindingsDetailsLabel(String resultFindingsDetailsLabel) {
        this.resultFindingsDetailsLabel = resultFindingsDetailsLabel;
    }

    /**
     * @return The label value.
     */
    public String getResultSubSectionDetailsTitle() {
        return resultSubSectionDetailsTitle;
    }

    /**
     * @param resultSubSectionDetailsTitle The label value to set.
     */
    public void setResultSubSectionDetailsTitle(String resultSubSectionDetailsTitle) {
        this.resultSubSectionDetailsTitle = resultSubSectionDetailsTitle;
    }

    /**
     * @return The label value.
     */
    public String getViewReportItemsShowAll() {
        return viewReportItemsShowAll;
    }

    /**
     * @param viewReportItemsShowAll The label value to set.
     */
    public void setViewReportItemsShowAll(String viewReportItemsShowAll) {
        this.viewReportItemsShowAll = viewReportItemsShowAll;
    }

    /**
     * @return The label value.
     */
    public String getViewReportItemsShowErrors() {
        return viewReportItemsShowErrors;
    }

    /**
     * @param viewReportItemsShowErrors The label value to set.
     */
    public void setViewReportItemsShowErrors(String viewReportItemsShowErrors) {
        this.viewReportItemsShowErrors = viewReportItemsShowErrors;
    }

    /**
     * @return The label value.
     */
    public String getViewReportItemsShowWarnings() {
        return viewReportItemsShowWarnings;
    }

    /**
     * @param viewReportItemsShowWarnings The label value to set.
     */
    public void setViewReportItemsShowWarnings(String viewReportItemsShowWarnings) {
        this.viewReportItemsShowWarnings = viewReportItemsShowWarnings;
    }

    /**
     * @return The label value.
     */
    public String getViewReportItemsShowMessages() {
        return viewReportItemsShowMessages;
    }

    /**
     * @param viewReportItemsShowMessages The label value to set.
     */
    public void setViewReportItemsShowMessages(String viewReportItemsShowMessages) {
        this.viewReportItemsShowMessages = viewReportItemsShowMessages;
    }

    /**
     * @return The label value.
     */
    public String getViewReportItemsDetailed() {
        return viewReportItemsDetailed;
    }

    /**
     * @param viewReportItemsDetailed The label value to set.
     */
    public void setViewReportItemsDetailed(String viewReportItemsDetailed) {
        this.viewReportItemsDetailed = viewReportItemsDetailed;
    }

    /**
     * @return The label value.
     */
    public String getViewReportItemsAggregated() {
        return viewReportItemsAggregated;
    }

    /**
     * @param viewReportItemsAggregated The label value to set.
     */
    public void setViewReportItemsAggregated(String viewReportItemsAggregated) {
        this.viewReportItemsAggregated = viewReportItemsAggregated;
    }

    /**
     * @return The label value.
     */
    public String getMaximumReportsExceededForDetailedOutputMessage() {
        return maximumReportsExceededForDetailedOutputMessage;
    }

    /**
     * @param maximumReportsExceededForDetailedOutputMessage The label value to set.
     */
    public void setMaximumReportsExceededForDetailedOutputMessage(String maximumReportsExceededForDetailedOutputMessage) {
        this.maximumReportsExceededForDetailedOutputMessage = maximumReportsExceededForDetailedOutputMessage;
    }

    /**
     * @return The label value.
     */
    public String getResultLocationLabel() {
        return resultLocationLabel;
    }

    /**
     * @param resultLocationLabel The label value to set.
     */
    public void setResultLocationLabel(String resultLocationLabel) {
        this.resultLocationLabel = resultLocationLabel;
    }

    /**
     * @return The label value.
     */
    public String getResultTestLabel() {
        return resultTestLabel;
    }

    /**
     * @param resultTestLabel The label value to set.
     */
    public void setResultTestLabel(String resultTestLabel) {
        this.resultTestLabel = resultTestLabel;
    }

    /**
     * @return The label value.
     */
    public String getAdditionalInfoLabel() {
        return additionalInfoLabel;
    }

    /**
     * @param additionalInfoLabel The label value to set.
     */
    public void setAdditionalInfoLabel(String additionalInfoLabel) {
        this.additionalInfoLabel = additionalInfoLabel;
    }

    /**
     * @return The label value.
     */
    public String getPopupCloseButton() {
        return popupCloseButton;
    }

    /**
     * @param popupCloseButton The label value to set.
     */
    public void setPopupCloseButton(String popupCloseButton) {
        this.popupCloseButton = popupCloseButton;
    }

    /**
     * @return The label value.
     */
    public String getPopupTitle() {
        return popupTitle;
    }

    /**
     * @param popupTitle The label value to set.
     */
    public void setPopupTitle(String popupTitle) {
        this.popupTitle = popupTitle;
    }

    /**
     * @return The label value.
     */
    public String getReportDetailedPDF() {
        return reportDetailedPDF;
    }

    /**
     * @param reportDetailedPDF The label value to set.
     */
    public void setReportDetailedPDF(String reportDetailedPDF) {
        this.reportDetailedPDF = reportDetailedPDF;
    }

    /**
     * @return The label value.
     */
    public String getReportAggregatedPDF() {
        return reportAggregatedPDF;
    }

    /**
     * @param reportAggregatedPDF The label value to set.
     */
    public void setReportAggregatedPDF(String reportAggregatedPDF) {
        this.reportAggregatedPDF = reportAggregatedPDF;
    }

    /**
     * @return The label value.
     */
    public String getReportDetailedCSV() {
        return reportDetailedCSV;
    }

    /**
     * @param reportDetailedCSV The label value to set.
     */
    public void setReportDetailedCSV(String reportDetailedCSV) {
        this.reportDetailedCSV = reportDetailedCSV;
    }

    /**
     * @return The label value.
     */
    public String getReportAggregatedCSV() {
        return reportAggregatedCSV;
    }

    /**
     * @param reportAggregatedCSV The label value to set.
     */
    public void setReportAggregatedCSV(String reportAggregatedCSV) {
        this.reportAggregatedCSV = reportAggregatedCSV;
    }

    /**
     * @return The label value.
     */
    public String getMaximumReportsExceededForXmlOutputMessage() {
        return maximumReportsExceededForXmlOutputMessage;
    }

    /**
     * @param maximumReportsExceededForXmlOutputMessage The label value to set.
     */
    public void setMaximumReportsExceededForXmlOutputMessage(String maximumReportsExceededForXmlOutputMessage) {
        this.maximumReportsExceededForXmlOutputMessage = maximumReportsExceededForXmlOutputMessage;
    }

    /**
     * @return The label value.
     */
    public String getViewAnnotatedInputButton() {
        return viewAnnotatedInputButton;
    }

    /**
     * @param viewAnnotatedInputButton The label value to set.
     */
    public void setViewAnnotatedInputButton(String viewAnnotatedInputButton) {
        this.viewAnnotatedInputButton = viewAnnotatedInputButton;
    }

    /**
     * @return The label value.
     */
    public String getDownloadReportButton() {
        return downloadReportButton;
    }

    /**
     * @param downloadReportButton The label value to set.
     */
    public void setDownloadReportButton(String downloadReportButton) {
        this.downloadReportButton = downloadReportButton;
    }

    /**
     * @return The label value.
     */
    public String getReportDetailedXML() {
        return reportDetailedXML;
    }

    /**
     * @param reportDetailedXML The label value to set.
     */
    public void setReportDetailedXML(String reportDetailedXML) {
        this.reportDetailedXML = reportDetailedXML;
    }

    /**
     * @return The label value.
     */
    public String getReportAggregatedXML() {
        return reportAggregatedXML;
    }

    /**
     * @param reportAggregatedXML The label value to set.
     */
    public void setReportAggregatedXML(String reportAggregatedXML) {
        this.reportAggregatedXML = reportAggregatedXML;
    }

    /**
     * @return The label value.
     */
    public String getViewDetailsButton() {
        return viewDetailsButton;
    }

    /**
     * @param viewDetailsButton The label value to set.
     */
    public void setViewDetailsButton(String viewDetailsButton) {
        this.viewDetailsButton = viewDetailsButton;
    }

    /**
     * @return The label value.
     */
    public String getViewSummaryButton() {
        return viewSummaryButton;
    }

    /**
     * @param viewSummaryButton The label value to set.
     */
    public void setViewSummaryButton(String viewSummaryButton) {
        this.viewSummaryButton = viewSummaryButton;
    }
}
