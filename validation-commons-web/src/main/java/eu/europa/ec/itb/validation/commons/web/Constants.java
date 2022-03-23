package eu.europa.ec.itb.validation.commons.web;

/**
 * Constants for web attributes.
 */
public class Constants {

    /**
     * Constructor to prevent instantiation.
     */
    private Constants() { throw new IllegalStateException("Utility class"); }

    /** The flag determining if the UI is a minimal one or a normal one. */
    public static final String IS_MINIMAL = "isMinimal";
    /** The header that signifies whether a received request was an ajax one. */
    public static final String AJAX_REQUEST_HEADER = "X-Requested-With";
}
