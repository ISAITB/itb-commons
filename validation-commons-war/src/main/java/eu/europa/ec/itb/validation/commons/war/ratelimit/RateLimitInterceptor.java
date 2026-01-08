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

import eu.europa.ec.itb.validation.commons.RateLimited;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Web MVC interceptor used to apply rate limit checks for REST and web UI validation calls.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String RATE_LIMIT_FLAG = "RATE_LIMIT_HANDLED";
    private final RateLimiterService rateLimiter;
    private final RateLimitKeyGenerator keyGenerator;

    /**
     * Constructor.
     *
     * @param rateLimiter The rate limiter to use.
     * @param keyGenerator The component used to generate the keys to check.
     */
    public RateLimitInterceptor(RateLimiterService rateLimiter, RateLimitKeyGenerator keyGenerator) {
        this.rateLimiter = rateLimiter;
        this.keyGenerator = keyGenerator;
    }

    /**
     * Check to see that the request can proceed.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @param handler  The component handler method.
     * @return Whether the call can proceed.
     * @throws Exception In case of unexpected error.
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // Only apply to controller methods
        if (handler instanceof HandlerMethod handlerMethod && request.getAttribute(RATE_LIMIT_FLAG) == null) {
            request.setAttribute(RATE_LIMIT_FLAG, Boolean.TRUE);
            RateLimited rateLimit = handlerMethod.getMethodAnnotation(RateLimited.class);
            if (rateLimit != null) {
                String key = keyGenerator.generate(request, rateLimit.policy());
                RateLimitCheckResult result = rateLimiter.tryConsume(key, rateLimit.policy());
                if (result.proceed()) {
                    return true;
                } else {
                    response.setStatus(429); // Too many requests
                    response.setHeader("Retry-After", String.valueOf(result.secondsToWaitForRetry()));
                    response.getWriter().write("Validation rate limit exceeded. Try again after %s second(s).".formatted(result.secondsToWaitForRetry()));
                    response.getWriter().flush();
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return true;
        }
    }
}
