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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eu.europa.ec.itb.validation.commons.RateLimitPolicy;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumMap;

/**
 * Component used to enforce validation rate limits based on a rolling bucket system (requests per minute).
 */
public class RateLimiterService {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterService.class);
    private static final RateLimitCheckResult SUCCESS_RESULT = new RateLimitCheckResult(true, null);
    private final Cache<String, Bucket> buckets;
    private final EnumMap<RateLimitPolicy, Bandwidth> policyBandwidths;
    private final boolean enabled;
    private final boolean warnOnly;

    /**
     * Constructor.
     *
     * @param config The application's configuration.
     */
    public RateLimiterService(ApplicationConfig.RateLimit config) {
        boolean enabled = false;
        Cache<String, Bucket> buckets = null;
        EnumMap<RateLimitPolicy, Bandwidth> policyBandwidths;
        if (config.isEnabled()) {
            enabled = true;
            policyBandwidths = new EnumMap<>(RateLimitPolicy.class);
            // Eagerly process and validate the configured capacities to catch errors early on.
            if (config.getCapacity() != null) {
                config.getCapacity().forEach((policy, capacity) -> {
                    if (capacity == null) {
                        LOG.warn("Missing rate limit capacity for [{}]. A default limit of {} per minute will be applied.", policy.getConfigurationKey(), policy.getDefaultCapacity());
                        capacity = policy.getDefaultCapacity();
                    } else if (capacity <= 0) {
                        LOG.warn("Invalid rate limit capacity [{}] configured for [{}] (the configured limit must be a positive integer). A default limit of {} per minute will be applied.", capacity, policy.getConfigurationKey(), policy.getDefaultCapacity());
                        capacity = policy.getDefaultCapacity();
                    }
                    Bandwidth limit = Bandwidth.builder()
                            .capacity(capacity)
                            .refillIntervally(capacity, Duration.ofMinutes(1))
                            .build();
                    policyBandwidths.put(policy, limit);
                });
            }
            // Maintain the latest 10000 distinct addresses.
            buckets = Caffeine.newBuilder()
                    .maximumSize(10_000)
                    .expireAfterAccess(Duration.ofDays(1))
                    .build();
        } else {
            policyBandwidths = null;
        }
        this.enabled = enabled;
        this.buckets = buckets;
        this.policyBandwidths = policyBandwidths;
        this.warnOnly = config.isWarnOnly();
    }

    /**
     * Check to see if the request can proceed.
     *
     * @param key The key to check. This combines the client's IP address and requested operation.
     * @param policy The policy to apply.
     * @return The check result.
     */
    public RateLimitCheckResult tryConsume(String key, RateLimitPolicy policy) {
        if (enabled) {
            Bucket bucket = buckets.get(key, k -> newBucket(policy));
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                LOG.debug("OK to proceed for [{}].", key);
            } else {
                if (warnOnly) {
                    LOG.warn("Request flagged after exceeding rate limit [{}].", key);
                } else {
                    LOG.warn("Request blocked after exceeding rate limit [{}].", key);
                    return new RateLimitCheckResult(false, probe.getNanosToWaitForRefill() / 1_000_000_000);
                }
            }
        }
        return SUCCESS_RESULT;
    }

    /**
     * Create a new bucket for the provided policy.
     *
     * @param policy The policy to create the bucket for.
     * @return The created bucket.
     */
    private Bucket newBucket(RateLimitPolicy policy) {
        Bandwidth limit = policyBandwidths.computeIfAbsent(policy, (p) -> {
            // This will be absent if rate limiting is enabled but all endpoint types are not configured.
            LOG.warn("No rate limit found for [{}]. A default limit of {} per minute will be applied.", p.getConfigurationKey(), p.getDefaultCapacity());
            return Bandwidth.builder()
                    .capacity(p.getDefaultCapacity())
                    .refillIntervally(p.getDefaultCapacity(), Duration.ofMinutes(1))
                    .build();
        });
        return Bucket.builder()
                .addLimit(policyBandwidths.getOrDefault(policy, limit))
                .build();
    }

}
