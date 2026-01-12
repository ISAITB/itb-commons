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

package eu.europa.ec.itb.validation.commons;

import com.gitb.tr.BAR;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionReportType;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Component used to Generate a CSV version of a TAR report.
 */
@Component
public class CsvReportGenerator {

    @Autowired
    ApplicationConfig appConfig;

    /**
     * BOM constant.
     */
    public static final String UTF8_BOM = new String(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, StandardCharsets.UTF_8);

    /**
     * Write the CSV report for the provided TAR input report and locale.
     *
     * @param reportFile The TAR input report's file acting as the CSV report's datasource.
     * @param outFile The target output file for the CSV report.
     * @param helper The localisation helper.
     * @param domainConfig The domain configuration.
     */
    public void writeReport(File reportFile, File outFile, LocalisationHelper helper, DomainConfig domainConfig) {
        writeReport(Utils.toTAR(reportFile), outFile, helper, domainConfig);
    }

    /**
     * Write the CSV report for the provided TAR input report and locale.
     *
     * @param report The TAR input report acting as the CSV report's datasource.
     * @param outFile The target output file for the CSV report.
     * @param helper The localisation helper.
     * @param domainConfig The domain configuration.
     */
    public void writeReport(TAR report, File outFile, LocalisationHelper helper, DomainConfig domainConfig) {
        var hasAdditionalInformation = hasAdditionalInformation(report);
        var hasTest = hasTest(report);
        var csvFormat = CSVFormat.DEFAULT.builder().setHeader(getHeaders(hasAdditionalInformation, hasTest, helper, domainConfig)).build();
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(outFile), csvFormat)) {
            for (var reportItem: report.getReports().getInfoOrWarningOrError()) {
                printer.printRecord(toRecord(reportItem, helper, hasTest, hasAdditionalInformation));
            }
        } catch (IOException e) {
            throw new ValidatorException("validator.label.exception.unableToGenerateCSVReport", e);
        }
    }

    /**
     * Get the CSV record for a report item when there is no additional information.
     *
     * @param element The item's element.
     * @param helper The localisation helper.
     * @param hasTest Whether test definitions should be included.
     * @param hasAdditionalInformation Whether additional information should be included.
     * @return The report's CSV record data.
     */
    private Object[] toRecord(JAXBElement<TestAssertionReportType> element, LocalisationHelper helper, boolean hasTest, boolean hasAdditionalInformation) {
        var fields = new ArrayList<>();
        fields.add(getLevel(element, helper));
        fields.add(StringUtils.defaultString(((BAR)element.getValue()).getDescription()));
        fields.add(StringUtils.defaultString(((BAR)element.getValue()).getLocation()));
        if (hasTest) {
            fields.add(StringUtils.defaultString(((BAR)element.getValue()).getTest()));
        }
        if (hasAdditionalInformation) {
            fields.add(StringUtils.defaultString(((BAR)element.getValue()).getAssertionID()));
        }
        return fields.toArray(new Object[0]);
    }

    /**
     * Get the CSV headers to use.
     *
     * @param hasAdditionalInformation True if additional information is included.
     * @param hasTest True if test information is included.
     * @param helper The localisation helper.
     * @param domainConfig The domain configuration.
     * @return The set of headers.
     */
    private String[] getHeaders(boolean hasAdditionalInformation, boolean hasTest, LocalisationHelper helper, DomainConfig domainConfig) {
        var headers = new ArrayList<String>();
        headers.add((domainConfig.isAddBOMToCSVExports()?UTF8_BOM:"")+helper.localise("validator.label.csvHeaderLevel"));
        headers.add(helper.localise("validator.label.csvHeaderDescription"));
        headers.add(helper.localise("validator.label.csvHeaderLocation"));
        if (hasTest) {
            headers.add(helper.localise("validator.label.csvHeaderTest"));
        }
        if (hasAdditionalInformation) {
            headers.add(helper.localise("validator.label.csvHeaderAdditionalInfo"));
        }
        return headers.toArray(new String[0]);
    }

    /**
     * Get the severity level text to include in the CSV report for a given report item element.
     *
     * @param element The report item element.
     * @param helper The localisation helper.
     * @return The severity level textx
     */
    private String getLevel(JAXBElement<TestAssertionReportType> element, LocalisationHelper helper) {
        if ("error".equalsIgnoreCase(element.getName().getLocalPart())) {
            return helper.localise("validator.label.csvLevelError");
        } else if ("warning".equalsIgnoreCase(element.getName().getLocalPart())) {
            return helper.localise("validator.label.csvLevelWarning");
        } else {
            return helper.localise("validator.label.csvLevelMessage");
        }
    }

    /**
     * Check to see whether the report includes an executed test or not.
     *
     * @param report The report to check.
     * @return The check result.
     */
    protected boolean hasTest(TAR report) {
        if (appConfig.isSupportsTestDefinitionInReportItems()) {
            for (var reportItem: report.getReports().getInfoOrWarningOrError()) {
                if (((BAR)reportItem.getValue()).getTest() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check to see whether the report includes additional information or not.
     *
     * @param report The report to check.
     * @return The check result.
     */
    private boolean hasAdditionalInformation(TAR report) {
        if (appConfig.isSupportsAdditionalInformationInReportItems()) {
            for (var reportItem: report.getReports().getInfoOrWarningOrError()) {
                if (((BAR)reportItem.getValue()).getAssertionID() != null) {
                    return true;
                }
            }
        }
        return false;
    }
}
