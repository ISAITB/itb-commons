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

package eu.europa.ec.itb.validation.commons;

/**
 * The enumeration of rate limit policies.
 */
public enum RateLimitPolicy {

    /** Validation through the UI. */
    UI_VALIDATE("uiValidate", 60),
    /** Validation through the REST API. */
    REST_VALIDATE("restValidate", 60),
    /** Validation through the REST API (multiple validations in a single call). */
    REST_VALIDATE_MULTIPLE("restValidateMultiple", 30),
    /** Validation through the SOAP API. */
    SOAP_VALIDATE("soapValidate", 60);

    private final String configurationKey;
    private final long defaultCapacity;

    /**
     * Constructor.
     *
     * @param configurationKey The key used to identify this policy in the configuration file.
     * @param defaultCapacity The default capacity to apply if missing.
     */
    RateLimitPolicy(String configurationKey, long defaultCapacity) {
        this.configurationKey = configurationKey;
        this.defaultCapacity = defaultCapacity;
    }

    /**
     * @return The key used to identify this policy in the configuration file.
     */
    public String getConfigurationKey() {
        return configurationKey;
    }

    /**
     * @return The default capacity to apply if missing.
     */
    public long getDefaultCapacity() {
        return defaultCapacity;
    }
}
