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

import java.util.Properties;

/**
 * Record class to hold validation metadata used for the generation of reports.
 *
 * @param inputFileName The input file name.
 * @param validationType The selected validation type.
 */
public record ReportProperties(String inputFileName, String validationType) {

    public Properties toProperties() {
        var props = new Properties();
        props.setProperty("validationType", validationType());
        props.setProperty("inputFileName", inputFileName());
        return props;
    }

    /**
     * Create an instance from a property object.
     *
     * @param props The properties to read.
     * @return The generated instance.
     */
    public static ReportProperties fromProperties(Properties props) {
        return new ReportProperties(props.getProperty("inputFileName"), props.getProperty("validationType"));
    }

}
