package eu.europa.ec.itb.validation.commons.config;

/**
 * Enumeration holding possible response types for different events.
 */
public enum ErrorResponseTypeEnum {

    /** Fail. log the issue and stop all subsequent processing. */
    FAIL("fail"),
    /** Continue processing but present a warning to the user and log the issue. */
    WARN("warn"),
    /** Log the issue and continue processing without notifying the user. */
    LOG("log");

    private final String value;

    /**
     * Constructor.
     *
     * @param value The enum's underlying value.
     */
    ErrorResponseTypeEnum(String value) {
        this.value = value;
    }

    /**
     * Get the enum type that corresponds to the provided value.
     *
     * @param value The value to process.
     * @return The resulting enum.
     * @throws IllegalArgumentException If the provided value is unknown.
     */
    public static ErrorResponseTypeEnum fromValue(String value) {
        if (FAIL.value.equals(value)) {
            return FAIL;
        } else if (WARN.value.equals(value)) {
            return WARN;
        } else if (LOG.value.equals(value)) {
            return LOG;
        }
        throw new IllegalArgumentException("Unknown response type ["+value+"]");
    }

}
