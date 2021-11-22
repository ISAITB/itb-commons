package eu.europa.ec.itb.validation.commons.error;

import eu.europa.ec.itb.validation.commons.LocalisationHelper;

import java.util.Locale;

/**
 * Exception class used to denote exceptions the messages of which should be directly presented to users.
 */
public class ValidatorException extends RuntimeException {

    /**
     * The default message.
     */
    public static final String MESSAGE_DEFAULT = "validator.label.exception.default";

    private final Object[] messageParams;
    private final boolean localised;

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
		this(message, null, (Object[]) null);
	}

    /**
     * @param message The message to display.
     * @param localised Whether the message is already localised.
     */
    public ValidatorException(String message, boolean localised) {
        this(message, null, localised, (Object[]) null);
    }

    /**
     * @param message The message to display.
     * @param messageParams The parameters for the message.
     */
    public ValidatorException(String message, Object... messageParams) {
        this(message, null, messageParams);
    }

    /**
     * @param message The message to display.
     * @param cause The root cause.
     * @param messageParams The parameters for the message.
     */
    public ValidatorException(String message, Throwable cause, Object... messageParams) {
        this(message, cause, false, messageParams);
    }

    /**
     * @param message The message to display.
     * @param cause The root cause.
     * @param localised Whether the message is already localised.
     * @param messageParams The parameters for the message.
     */
    public ValidatorException(String message, Throwable cause, boolean localised, Object... messageParams) {
        super(message, cause);
        this.messageParams = messageParams;
        this.localised = localised;
    }

    /**
     * @return The parameters for the message's customisation.
     */
    public Object[] getMessageParams() {
        return messageParams;
    }

    /**
     * @return Whether the message is already localised.
     */
    public boolean isLocalised() {
        return localised;
    }

    /**
     * @return The message to include in log files.
     */
    public String getMessageForLog() {
        return isLocalised()?getMessage():new LocalisationHelper(Locale.ENGLISH).localise(getMessage(), getMessageParams());
    }

    /**
     * @param helper The localisation helper to use.
     * @return The message to display to users.
     */
    public String getMessageForDisplay(LocalisationHelper helper) {
        return isLocalised()?getMessage():helper.localise(getMessage(), getMessageParams());
    }

}
