/*
 * Copyright (C) 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.validation.commons.report;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ReportProperties;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
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
     * @param properties The additional properties to consider when generating the report.
     * @param domainConfig The domain configuration.
     */
    public <R extends DomainConfig> void writeReport(File inFile, File outFile, LocalisationHelper helper, ReportProperties properties, R domainConfig) {
        writeReport(inFile, outFile, tar -> getReportLabels(helper, tar, properties, domainConfig), properties, domainConfig);
    }

    /**
     * Generate a TAR PDF report from the provided TAR XML report as input.
     *
     * @param inFile The input XML report.
     * @param outFile The output PDF report.
     * @param labelProvider A function to provide the labels to use in the report.
     * @param properties The additional properties to consider when generating the report.
     * @param domainConfig The domain configuration.
     */
    public <R extends DomainConfig> void writeReport(File inFile, File outFile, Function<TAR, ReportLabels> labelProvider, ReportProperties properties, R domainConfig) {
        try (FileInputStream fis = new FileInputStream(inFile); FileOutputStream fos = new FileOutputStream(outFile)) {
            reportGenerator.writeTARReport(fis, fos, labelProvider, properties, domainConfig);
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
     * @param properties The additional properties to consider when generating the report.
     * @param domainConfig The domain configuration.
     */
    public <R extends DomainConfig> void writeReport(TAR report, File outFile, LocalisationHelper helper, ReportProperties properties, R domainConfig) {
        writeReport(report, outFile, tar -> getReportLabels(helper, tar, properties, domainConfig), properties, domainConfig);
    }

    /**
     * Generate a TAR PDF report from the provided TAR XML report as input.
     *
     * @param report The TAR report object as input.
     * @param outFile The output PDF report.
     * @param labelProvider A function to provide the labels to use in the report.
     * @param properties The additional properties to consider when generating the report.
     * @param domainConfig The domain configuration.
     */
    public <R extends DomainConfig> void writeReport(TAR report, File outFile, Function<TAR, ReportLabels> labelProvider, ReportProperties properties, R domainConfig) {
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            reportGenerator.writeTARReport(report, fos, labelProvider, properties, domainConfig);
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
    public <R extends DomainConfig> ReportLabels getReportLabels(LocalisationHelper helper, TAR report, ReportProperties properties, R domainConfig) {
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
        reportLabels.setFileName(helper.localise("validator.label.resultFileNameLabel"));
        reportLabels.setValidationType(helper.localise("validator.label.resultValidationTypeLabel"));
        reportLabels.setValidationTypeName(domainConfig.getCompleteTypeOptionLabel(properties.validationType(), helper));
        String resultKey = report.getResult().value().toLowerCase();
        reportLabels.setCustomMessageOverview(getCustomReportMessage("validator.customReportMessage.overviewSection", properties.validationType(), resultKey, helper));
        reportLabels.setCustomMessageErrors(getCustomReportMessage("validator.customReportMessage.errorsSection", properties.validationType(), resultKey, helper));
        reportLabels.setCustomMessageWarnings(getCustomReportMessage("validator.customReportMessage.warningsSection", properties.validationType(), resultKey, helper));
        reportLabels.setCustomMessageMessages(getCustomReportMessage("validator.customReportMessage.messagesSection", properties.validationType(), resultKey, helper));
        reportLabels.setUniqueRule(helper.localise("validator.label.uniqueRule"));
        reportLabels.setUniqueRules(helper.localise("validator.label.uniqueRules"));
        reportLabels.setErrors(helper.localise("validator.label.errors"));
        reportLabels.setWarnings(helper.localise("validator.label.warnings"));
        reportLabels.setMessages(helper.localise("validator.label.messages"));
        reportLabels.setErrorSectionTitle(helper.localise("validator.label.errorSectionTitle"));
        reportLabels.setWarningSectionTitle(helper.localise("validator.label.warningSectionTitle"));
        reportLabels.setMessageSectionTitle(helper.localise("validator.label.messageSectionTitle"));
        return reportLabels;
    }

    /**
     * Generate the custom report message to use.
     *
     * @param basePropertyKey The base config property key.
     * @param typeOption The validation type.
     * @param resultKey The key part for the result.
     * @param helper The localisation helper.
     * @return The message.
     */
    public String getCustomReportMessage(String basePropertyKey, String typeOption, String resultKey, LocalisationHelper helper) {
        String propertyKeyToCheck = "%s.%s.%s".formatted(basePropertyKey, resultKey, typeOption);
        String propertyKeyToLookup = null;
        if (helper.propertyExists(propertyKeyToCheck)) {
            propertyKeyToLookup = propertyKeyToCheck;
        } else {
            propertyKeyToCheck = "%s.%s".formatted(basePropertyKey, resultKey);
            if (helper.propertyExists(propertyKeyToCheck)) {
                propertyKeyToLookup = propertyKeyToCheck;
            } else {
                propertyKeyToCheck = basePropertyKey;
                if (helper.propertyExists(propertyKeyToCheck)) {
                    propertyKeyToLookup = basePropertyKey;
                }
            }
        }
        if (propertyKeyToLookup != null) {
            return Utils.sanitizeCustomReportMessage(helper.localise(propertyKeyToLookup));
        } else {
            return null;
        }
    }

}
