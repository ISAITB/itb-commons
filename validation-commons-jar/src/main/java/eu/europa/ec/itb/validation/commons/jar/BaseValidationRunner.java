package eu.europa.ec.itb.validation.commons.jar;

import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfigCache;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Base class for the components used to carry out a command line driven validation run.
 *
 * @param <X> The specific domain configuration class.
 */
public abstract class BaseValidationRunner<X extends DomainConfig> implements ValidationRunner {

    /** Regular logger for application output. */
    protected static final Logger LOGGER = LoggerFactory.getLogger(ValidationRunner.class);
    /** Logger specifically for command line feedback to the user. */
    protected static final Logger LOGGER_FEEDBACK = LoggerFactory.getLogger("FEEDBACK");
    /** Logger specifically for feedback for the user added in the log file. */
    protected static final Logger LOGGER_FEEDBACK_FILE = LoggerFactory.getLogger("VALIDATION_RESULT");
    /** The fixed pad to ensure consistent messages by validators. */
    protected static final String PAD = "   ";
    /** Flag to switch off console output. */
    public static final String FLAG_NO_OUTPUT = "-nooutput";
    /** Flag to switch off file log output. */
    public static final String FLAG_NO_LOG = "-nolog";

    protected X domainConfig;

    @Autowired
    protected DomainConfigCache<X> domainConfigCache;

    /**
     * Initialisation method to determine if the domain configurations are well-defined.
     *
     * When used via the command line a validator must define only one domain.
     */
    @PostConstruct
    public void init() {
        // Determine the domain configuration.
        List<X> domainConfigurations = domainConfigCache.getAllDomainConfigurations();
        if (domainConfigurations.size() == 1) {
            this.domainConfig = domainConfigurations.get(0);
        } else if (domainConfigurations.size() > 1) {
            StringBuilder message = new StringBuilder();
            message.append("A specific validation domain needs to be selected. Do this by supplying the -Dvalidator.domain argument. Possible values for this are [");
            for (DomainConfig dc: domainConfigurations) {
                message.append(dc.getDomainName());
                message.append("|");
            }
            message.delete(message.length()-1, message.length()).append("].");
            LOGGER_FEEDBACK.error(message.toString());
            LOGGER.error(message.toString());
            throw new IllegalArgumentException();
        } else {
            String message = "No validation domains could be found.";
            LOGGER_FEEDBACK.error(message);
            LOGGER.error(message);
            throw new IllegalStateException(message);
        }
    }

    /**
     * Extract a string from the provided list of arguments.
     *
     * @param args The arguments.
     * @param argCounter The index of the argument to extract.
     * @return The argument value.
     */
    protected String argumentAsString(String[] args, int argCounter) {
        if (args.length > argCounter + 1) {
            return args[++argCounter];
        }
        return null;
    }

    /**
     * Check to see if the provided value is a valid URL.
     *
     * @param value The value to check.
     * @return True if it is a URL.
     */
    protected boolean isValidURL(String value) {
        try {
            new URL(value);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * Run the validation. This method ensures that the temporary folder created is removed once the validator completes.
     *
     * @param args The command-line arguments.
     * @param parentFolder The temporary folder to use for this validator's run.
     */
    @Override
    public void bootstrap(String[] args, File parentFolder) {
        try {
            bootstrapInternal(args, parentFolder);
        } finally {
            FileUtils.deleteQuietly(parentFolder);
        }
    }

    /**
     * Carry out the parsing of arguments and launch a validation run.
     *
     * @param args The command-line arguments.
     * @param parentFolder The temporary folder to use for this validator's run.
     */
    protected abstract void bootstrapInternal(String[] args, File parentFolder);
}
