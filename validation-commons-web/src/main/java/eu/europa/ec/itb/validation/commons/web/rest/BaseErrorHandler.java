/*
 * Copyright (C) 2026 European Union
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

package eu.europa.ec.itb.validation.commons.web.rest;

import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Locale;

/**
 * Handle all errors linked to validator REST API calls.
 * <p>
 * The validator explicitly raises NotFoundException for cases of invalid requested domains. Once the validation
 * is allowed to proceed all errors that are expected can be reported back to users are raised as ValidatorExceptions.
 * The messages of these exceptions are localised according to the request's locale (see {@link #recordLocaliser(LocalisationHelper)}).
 * All other exceptions are considered unexpected and are raised with a generic error message.
 * <p>
 * This class is expected to be subclassed and set as {@link org.springframework.web.bind.annotation.ControllerAdvice}
 * on the REST endpoint implementation class.
 *
 * @see NotFoundException
 * @see ValidatorException
 */
public class BaseErrorHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BaseErrorHandler.class);
    private static final String LOCALISER_ATTRIBUTE = BaseErrorHandler.class.getName() + ".LOCALISER";

    /**
     * Handle the "not found" errors. These typically link to a domain being requested that is not configured or
     * using the API when not supported.
     *
     * @param ex The exception.
     * @param request The current request.
     * @return The response.
     */
    @ExceptionHandler(value = {NotFoundException.class})
    protected ResponseEntity<Object> handleNotFound(NotFoundException ex, WebRequest request) {
        if (LOG.isWarnEnabled()) {
            LOG.warn("Caught NotFoundException for domain [{}]", ex.getRequestedDomain(), ex);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return handleExceptionInternal(ex, new ErrorInfo("The requested resource could not be found"), headers, HttpStatus.NOT_FOUND, request);
    }

    /**
     * Handle errors that are expected and can be shared with users.
     *
     * @param ex The exception.
     * @param request The current request.
     * @return The response.
     */
    @ExceptionHandler(value = {ValidatorException.class})
    protected ResponseEntity<Object> handleValidatorException(ValidatorException ex, WebRequest request) {
        if (LOG.isErrorEnabled()) {
            LOG.error("Caught ValidatorException: {}", ex.getMessageForLog(), ex);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return handleExceptionInternal(ex, new ErrorInfo(ex.getMessageForDisplay(resolveLocaliser(request)), ex.getInputReference()), headers, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Record the helper to use to localise the error messages of the current request. This is expected to be called by the
     * REST controllers as soon as the request's locale is known (i.e. the "locale" input and the domain configuration).
     * If never called, the error messages are localised based on the request's "Accept-Language" header.
     *
     * @param localiser The helper to use for the current request.
     */
    public static void recordLocaliser(LocalisationHelper localiser) {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(LOCALISER_ATTRIBUTE, localiser, RequestAttributes.SCOPE_REQUEST);
        }
    }

    /**
     * Determine the helper to use to localise a request's error messages. This is, in order of preference, the helper
     * recorded by the controller (see {@link #recordLocaliser(LocalisationHelper)}), the locale of the "Accept-Language"
     * header, or English.
     *
     * @param request The current request.
     * @return The helper to use.
     */
    private LocalisationHelper resolveLocaliser(WebRequest request) {
        if (request.getAttribute(LOCALISER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) instanceof LocalisationHelper recorded) {
            return recorded;
        }
        var acceptLanguage = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            try {
                var ranges = Locale.LanguageRange.parse(acceptLanguage);
                if (!ranges.isEmpty() && !"*".equals(ranges.get(0).getRange())) {
                    return new LocalisationHelper(Locale.forLanguageTag(ranges.get(0).getRange()));
                }
            } catch (IllegalArgumentException e) {
                LOG.debug("Ignoring invalid Accept-Language header [{}]", acceptLanguage);
            }
        }
        return new LocalisationHelper(Locale.ENGLISH);
    }

    /**
     * Handle all other exceptions.
     *
     * @param ex The exception.
     * @param request The current request.
     * @return The response.
     */
    @ExceptionHandler(value = {Exception.class})
    protected ResponseEntity<Object> handleUnexpectedErrors(Exception ex, WebRequest request) {
        LOG.error("Caught Exception", ex);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return handleExceptionInternal(ex, new ErrorInfo("An unexpected error occurred during validation"), headers, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

}
