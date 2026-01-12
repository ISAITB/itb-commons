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

package eu.europa.ec.itb.validation.commons.web.locale;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Class that controls the locale resolution from a given HTTP request.
 */
@Component
public class CustomLocaleResolver {

    /**
     * Method that resolves a locale based on a request, response, and domain and
     * application configuration.
     * 
     * @param request   The http request.
     * @param response  The http response.
     * @param config    The domain configuration, which holds the default locale.
     * @param appConfig The application configuration object, which has the name of
     *                  the validator, used in the setting of the http only cookie.
     * @return The locale.
     */
    public Locale resolveLocale(HttpServletRequest request, HttpServletResponse response, WebDomainConfig config, ApplicationConfig appConfig) {
        if (config == null) {
            return Locale.ENGLISH;
        } else {
            if (config.getAvailableLocales().size() > 1 && appConfig != null && request != null) {
                String cookieName = appConfig.getIdentifier() + "." + config.getDomainName() +  "." + "locale";
                String requestedLanguage = request.getParameter("lang");
                if (requestedLanguage != null && !requestedLanguage.isBlank()) {
                    // case in which the locale has been inserted in the request.
                    var requestedLocale = LocaleUtils.toLocale(requestedLanguage);
                    if (config.getAvailableLocales().contains(requestedLocale)) {
                        if (response != null) {
                            if (config.isSupportUserInterfaceEmbedding()) {
                                // Need to set as SameSite=None (and also secure).
                                response.addHeader("Set-Cookie", String.format("%s=%s; HttpOnly; SameSite=None; Secure", cookieName, requestedLocale.getLanguage()));
                            } else {
                                Cookie cookie = new Cookie(cookieName, requestedLocale.getLanguage());
                                cookie.setHttpOnly(true);
                                response.addCookie(cookie);
                            }
                        }
                        return requestedLocale;
                    }
                } else {
                    Cookie[] cookies = (request.getCookies() == null) ? new Cookie[] {} : request.getCookies();
                    List<Cookie> localeCookies = Arrays.stream(cookies).filter(c -> c.getName().contentEquals(cookieName)).toList();
                    if (!localeCookies.isEmpty()) { // case where the locale has been inserted in the cookie previously.
                        return LocaleUtils.toLocale(localeCookies.get(0).getValue());
                    }
                }
            }
            return config.getDefaultLocale();
        }
    }

}
