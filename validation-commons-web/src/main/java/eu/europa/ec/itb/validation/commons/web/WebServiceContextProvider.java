package eu.europa.ec.itb.validation.commons.web;

import jakarta.xml.ws.WebServiceContext;

/**
 * A component capable of returning a context for a SOAP web service call
 */
public interface WebServiceContextProvider {

    /**
     * Get the current web service context for an ongoing SOAP call.
     *
     * @return The context.
     */
    WebServiceContext getWebServiceContext();

}
