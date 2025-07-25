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
    /** The flag determining if a form submission came from the validator's own UI. */
    public static final String IS_SELF_SUBMITTED = "isSelfSubmitted";
    /** The header that signifies whether a received request was an ajax one. */
    public static final String AJAX_REQUEST_HEADER = "X-Requested-With";
    /** Custom header to identify the source of a form submission ('self' or other). */
    public static final String SUBMIT_SOURCE_HEADER = "X-Submit-Source";
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
    /** Constant for the name of the JavaScript extension exists Thymeleaf template parameter. */
    public static final String PARAM_JAVASCRIPT_EXTENSION_EXISTS = "javascriptExtensionExists";
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
    /** Constant for the name of the report date Thymeleaf template parameter. */
    public static final String PARAM_REPORT_DATA = "reportData";
    /** Constant for the name of the label configuration Thymeleaf template parameter. */
    public static final String PARAM_LABEL_CONFIG = "labelConfig";
    /** Constant for the name of the CSP nonce Thymeleaf template parameter. */
    public static final String PARAM_NONCE = "nonce";
    /** Constant for the name of the Thymeleaf view for the upload form. */
    public static final String VIEW_UPLOAD_FORM = "uploadForm";
    /** Constant for the name of the MDC logging parameter for the domain name. */
    public static final String MDC_DOMAIN = "domain";

}
