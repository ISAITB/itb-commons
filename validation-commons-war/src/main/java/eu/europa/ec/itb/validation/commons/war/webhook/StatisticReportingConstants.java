package eu.europa.ec.itb.validation.commons.war.webhook;

/**
 * Constants linked to statistics reporting.
 */
public class StatisticReportingConstants {

    /**
     * Constructor to prevent instantiation.
     */
    private StatisticReportingConstants() { throw new IllegalStateException("Utility class"); }

    /** The web API. */
    public static final String WEB_API = "web";
    /** The web (minimal) API. */
    public static final String WEB_MINIMAL_API = "web_minimal";
    /** The SOAP API. */
    public static final String SOAP_API = "soap";
    /** The REST API. */
    public static final String REST_API = "rest";
    /** The email API. */
    public static final String EMAIL_API = "email";

    /** The context parameter name for the IP address. */
    public static final String PARAM_IP = "ip";
    /** The context parameter name for the API used. */
    public static final String PARAM_API = "api";

}
