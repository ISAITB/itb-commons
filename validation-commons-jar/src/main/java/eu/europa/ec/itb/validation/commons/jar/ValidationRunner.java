package eu.europa.ec.itb.validation.commons.jar;

import java.io.File;

/**
 * Common interface for classes responsible for executing a validation run.
 *
 * This interface is used primarily as a marker to lookup the appropriate Spring bean to use.
 * @see CommandLineValidator#start(Class, String[], String)
 */
public interface ValidationRunner {

    /**
     * Run the validation via command line.
     *
     * @param args The command-line arguments.
     * @param parentFolder The temporary folder to use for this validator's run.
     */
    void bootstrap(String[] args, File parentFolder);

}
