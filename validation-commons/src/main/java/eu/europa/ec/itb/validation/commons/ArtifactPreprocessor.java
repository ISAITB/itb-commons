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

package eu.europa.ec.itb.validation.commons;

import java.io.File;

/**
 * Class that can preprocess validation artifacts before these are used for validation.
 */
public interface ArtifactPreprocessor {

    /**
     * Process the provided file to generate a new file next to the provided one.
     *
     * @param fileToProcess The file to proprocess.
     * @param preProcessorFile The path to the pre-processing artifact (e.g. an XSLT file).
     * @param outputFileExtension The file extension to use for the output file.
     * @return The file resulting from the processing (created next to the fileToProcess).
     */
    File preprocessFile(File fileToProcess, File preProcessorFile, String outputFileExtension);

}
