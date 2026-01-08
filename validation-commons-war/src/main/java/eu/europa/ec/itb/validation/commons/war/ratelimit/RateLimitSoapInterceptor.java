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

package eu.europa.ec.itb.validation.commons.war.ratelimit;

import eu.europa.ec.itb.validation.commons.RateLimitPolicy;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

/**
 * CXF interceptor used to apply rate limit checks for SOAP validation calls.
 */
public class RateLimitSoapInterceptor extends AbstractPhaseInterceptor<SoapMessage> {

    private final RateLimiterService rateLimiter;
    private final RateLimitKeyGenerator keyGenerator;

    /**
     * Constructor.
     *
     * @param rateLimiter The rate limiter to use.
     * @param keyGenerator The component used to generate the keys to check.
     */
    public RateLimitSoapInterceptor(RateLimiterService rateLimiter, RateLimitKeyGenerator keyGenerator) {
        super(Phase.PRE_INVOKE); // Run before the SOAP service method
        this.rateLimiter = rateLimiter;
        this.keyGenerator = keyGenerator;
    }

    /**
     * Check to see that the SOAP request can proceed.
     *
     * @param soapMessage The SOAP message.
     * @throws Fault Raised in case the rate limit is exceeded.
     */
    @Override
    public void handleMessage(SoapMessage soapMessage) throws Fault {
        String operation = soapMessage.getExchange().getBindingOperationInfo().getOperationInfo().getName().getLocalPart();
        if ("validate".equals(operation)) {
            HttpServletRequest request = (HttpServletRequest) soapMessage.get(AbstractHTTPDestination.HTTP_REQUEST);
            if (request != null) {
                String key = keyGenerator.generate(request, RateLimitPolicy.SOAP_VALIDATE);
                RateLimitCheckResult result = rateLimiter.tryConsume(key, RateLimitPolicy.SOAP_VALIDATE);
                if (!result.proceed()) {
                    throw new Fault(new RuntimeException("Validation rate limit exceeded. Try again after %s second(s).".formatted(result.secondsToWaitForRetry())));
                }
            }
        }
    }
}
