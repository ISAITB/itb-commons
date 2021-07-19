package eu.europa.ec.itb.validation.commons.error;

/**
 * Exception class used to denote exceptions the messages of which should be directly presented to users.
 */
public class ValidatorException extends RuntimeException {

    /**
     * The default message.
     */
    static final String MESSAGE_DEFAULT = "An unexpected error was raised during validation.";

    /**
     * @param cause The root cause.
     */
    public ValidatorException(Throwable cause) {
        this(MESSAGE_DEFAULT, cause);
    }

    /**
     * @param message The message to display.
     */
	public ValidatorException(String message) {
		this(message, null);
	}

    /**
     * @param message The message to display.
     * @param cause The root cause.
     */
    public ValidatorException(String message, Throwable cause) {
        super(message, cause);
    }

}
