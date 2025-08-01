/*
 * Copyright (C) 2025 European Union
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

package eu.europa.ec.itb.validation.commons.jar;

import com.gitb.tr.TAR;

/**
 * Class used to summarise a TAR validation report.
 */
public class FileReport {

    private final String fileName;
    private final TAR report;
    private final boolean requireType;
    private final String validationType;

    /**
     * Constructor.
     *
     * @param fileName The report file name.
     * @param report The report contents.
     */
    public FileReport(String fileName, TAR report) {
        this(fileName, report, false, null);
    }

    /**
     * Constructor.
     *
     * @param fileName The report file name.
     * @param report The report contents.
     * @param requireType True to include the validation type in messages.
     * @param type The validation type.
     */
    public FileReport(String fileName, TAR report, boolean requireType, String type) {
        this.fileName = fileName;
        this.report = report;
        this.requireType = requireType;
        this.validationType = type;
    }

    /**
     * Convert the provided report to a command-line message.
     *
     * @return The text.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Validation report summary [").append(this.fileName).append("]:");
        if(requireType) {
        	sb.append("\n- Validation type: ").append(this.validationType);
        }
        sb.append("\n- Date: ").append(report.getDate());
        sb.append("\n- Result: ").append(report.getResult());
        sb.append("\n- Errors: ").append(report.getCounters().getNrOfErrors());
        sb.append("\n- Warnings: ").append(report.getCounters().getNrOfWarnings());
        sb.append("\n- Messages: ").append(report.getCounters().getNrOfAssertions());
        
        return sb.toString();
    }
}
