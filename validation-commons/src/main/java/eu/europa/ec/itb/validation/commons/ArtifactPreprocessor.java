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
