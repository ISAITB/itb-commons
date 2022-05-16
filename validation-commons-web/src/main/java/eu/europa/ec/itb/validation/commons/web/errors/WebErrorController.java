package eu.europa.ec.itb.validation.commons.web.errors;

import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.web.Constants;
import eu.europa.ec.itb.validation.commons.web.locale.CustomLocaleResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Error handling for the validator's web application.
 */
@Controller
public class WebErrorController implements ErrorController {

	private static final Logger logger = LoggerFactory.getLogger(WebErrorController.class);

    @Autowired
    private CustomLocaleResolver localeResolver;
    @Autowired
    private ApplicationConfig appConfig;

    /**
     * Handle a web error. The handling here forwards to a common error page or, in case of errors
     * in ajax calls, returns a JSON error payload.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @return The model data.
     */
    @RequestMapping(value = "/error", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView handleError(HttpServletRequest request, HttpServletResponse response) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        DomainConfig domainConfig = (DomainConfig) request.getAttribute(WebDomainConfig.DOMAIN_CONFIG_REQUEST_ATTRIBUTE);
        String xRequestedWith = request.getHeader(Constants.AJAX_REQUEST_HEADER);
        if(xRequestedWith != null && xRequestedWith.equalsIgnoreCase("XmlHttpRequest")) {
        	return ajaxError(status, response);
        }
        Boolean isMinimalUI = (Boolean)request.getAttribute(Constants.IS_MINIMAL);
        if (isMinimalUI == null) {
            isMinimalUI = false;
        }
        boolean previousPage = (request.getHeader("referer") != null)? Boolean.TRUE: Boolean.FALSE;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("minimalUI", isMinimalUI);
        attributes.put("previousPage", previousPage);	
        attributes.put("errorMessage", getErrorMessage(status));
        if (domainConfig == null) {
            attributes.put("localiser", new LocalisationHelper(Locale.ENGLISH));

        } else {
            var localeToUse = localeResolver.resolveLocale(request, response, domainConfig, appConfig);
            attributes.put("localiser", new LocalisationHelper(domainConfig, localeToUse));
        }
        return new ModelAndView("errorPage", attributes);
    }

    /**
     * Extract the error message depending on the type of error.
     *
     * @param status The received error status.
     * @return The message to display.
     */
    private String getErrorMessage(Object status){
    	String errorMessage = "-";
    	if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            if(statusCode == HttpStatus.NOT_FOUND.value()) {
            	errorMessage = "The requested path or resource does not exist.";
            } else {
            	errorMessage = "An internal server error occurred.";
            }
        } 
    	return errorMessage;
    }

    /**
     * Handle an error for an ajax call.
     *
     * @param status The error information.
     * @param response The HTTP response.
     * @return The model data.
     */
    private ModelAndView ajaxError(Object status, HttpServletResponse response) {
    	MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
    	MediaType jsonMimeType = MediaType.APPLICATION_JSON;
    	Map<String, String> responseMessage = new HashMap<>();
    	responseMessage.put("errorMessage", getErrorMessage(status));
    	try {
    		jsonConverter.write(responseMessage, jsonMimeType, new ServletServerHttpResponse(response));
    	} catch (HttpMessageNotWritableException | IOException e) {
    		logger.error("Error generating the error response to ajax request", e);
    	}
    	return null;
    }

}