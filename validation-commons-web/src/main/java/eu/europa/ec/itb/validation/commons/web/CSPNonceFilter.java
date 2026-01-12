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

package eu.europa.ec.itb.validation.commons.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Filter used to generate a unique nonce per request for use in a CSP directive.
 */
public class CSPNonceFilter extends GenericFilterBean {

    public static final String CSP_NONCE_ATTRIBUTE = "cspNonce";
    public static final String CSP_NONCE_HEADER_PLACEHOLDER = "{nonce}";
    private static final int NONCE_SIZE = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        var request = (HttpServletRequest)servletRequest;
        var response = (HttpServletResponse) servletResponse;
        byte[] nonceArray = new byte[NONCE_SIZE];
        secureRandom.nextBytes(nonceArray);

        String nonce = Base64.getEncoder().encodeToString(nonceArray);
        request.setAttribute(CSP_NONCE_ATTRIBUTE, nonce);

        filterChain.doFilter(request, new CSPNonceResponseWrapper(response, nonce));
    }

    /**
     * Wrapper to fill the nonce value
     */
    public static class CSPNonceResponseWrapper extends HttpServletResponseWrapper {
        private final String nonce;

        public CSPNonceResponseWrapper(HttpServletResponse response, String nonce) {
            super(response);
            this.nonce = nonce;
        }

        @Override
        public void setHeader(String name, String value) {
            if ((name.equals("Content-Security-Policy") || name.equals("Content-Security-Policy-Report-Only")) && StringUtils.isNotBlank(value)) {
                super.setHeader(name, value.replace(CSP_NONCE_HEADER_PLACEHOLDER, nonce));
            } else {
                super.setHeader(name, value);
            }
        }

        @Override
        public void addHeader(String name, String value) {
            if ((name.equals("Content-Security-Policy") || name.equals("Content-Security-Policy-Report-Only")) && StringUtils.isNotBlank(value)) {
                super.addHeader(name, value.replace(CSP_NONCE_HEADER_PLACEHOLDER, nonce));
            } else {
                super.addHeader(name, value);
            }
        }
    }
}
