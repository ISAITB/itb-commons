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
    /** Constant for the name of the domain config Thymeleaf template parameter. */
    public static final String PARAM_DOMAIN_CONFIG = "config";
    /** Constant for the name of the app config Thymeleaf template parameter. */
    public static final String PARAM_APP_CONFIG = "appConfig";
    /** Constant for the name of the minimal UI Thymeleaf template parameter. */
    public static final String PARAM_MINIMAL_UI = "minimalUI";
    /** Constant for the name of the external artifact info Thymeleaf template parameter. */
    public static final String PARAM_EXTERNAL_ARTIFACT_INFO = "externalArtifactInfo";
    /** Constant for the name of the localiser Thymeleaf template parameter. */
    public static final String PARAM_LOCALISER = "localiser";
    /** Constant for the name of the HTML banner exists Thymeleaf template parameter. */
    public static final String PARAM_HTML_BANNER_EXISTS = "htmlBannerExists";
    /** Constant for the name of the validation type label Thymeleaf template parameter. */
    public static final String PARAM_VALIDATION_TYPE_LABEL = "validationTypeLabel";
    /** Constant for the name of the message Thymeleaf template parameter. */
    public static final String PARAM_MESSAGE = "message";
    /** Constant for the name of the detailed report Thymeleaf template parameter. */
    public static final String PARAM_REPORT = "report";
    /** Constant for the name of the aggregated report Thymeleaf template parameter. */
    public static final String PARAM_AGGREGATE_REPORT = "aggregateReport";
    /** Constant for the name of the Thymeleaf template parameter for the flag to show or not aggregated report controls. */
    public static final String PARAM_SHOW_AGGREGATE_REPORT = "showAggregateReport";
    /** Constant for the name of the date Thymeleaf template parameter. */
    public static final String PARAM_DATE = "date";
    /** Constant for the name of the file name Thymeleaf template parameter. */
    public static final String PARAM_FILE_NAME = "fileName";
    /** Constant for the name of the input ID Thymeleaf template parameter. */
    public static final String PARAM_INPUT_ID = "inputID";
    /** Constant for the name of the Thymeleaf view for the upload form. */
    public static final String VIEW_UPLOAD_FORM = "uploadForm";
    /** Constant for the name of the MDC logging parameter for the domain name. */
    public static final String MDC_DOMAIN = "domain";

}
