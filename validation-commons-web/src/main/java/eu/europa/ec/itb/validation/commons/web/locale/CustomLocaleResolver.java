package eu.europa.ec.itb.validation.commons.web.locale;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;

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
     * @param appConfig The applicataion configuration object, which has the name of
     *                  the validator, used in the setting of the http only cookie.
     * @return The locale.
     */
    public Locale resolveLocale(HttpServletRequest request, HttpServletResponse response, DomainConfig config,
            ApplicationConfig appConfig) {
        String cookieName = appConfig.getIdentifier() + "." + config.getDomainName() +  "." + "locale";
        String requestedLanguage = request.getParameter("lang"); 
        if (requestedLanguage != null && !requestedLanguage.isEmpty() && !requestedLanguage.isBlank()) { 
            // case in which the locale has been inserted in the request.
            if (config.getAvailableLocales().contains(requestedLanguage)) {
                Cookie cookie = new Cookie(cookieName, requestedLanguage);
                cookie.setHttpOnly(true);
                response.addCookie(cookie);
                return new Locale(requestedLanguage);
            }
        } else {
            Cookie[] cookies = (request.getCookies() == null) ? new Cookie[] {} : request.getCookies();
            List<Cookie> localeCookies = Arrays.stream(cookies).filter(c -> c.getName().contentEquals(cookieName))
                    .collect(Collectors.toList());
            if (localeCookies.size() >= 1) { // case where the locale has been inserted in the cookie previously.
                return new Locale(localeCookies.get(0).getValue());
            }
        } // default case.
        return new Locale(config.getDefaultLocale());
    }

}
