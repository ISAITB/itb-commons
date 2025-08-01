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

package eu.europa.ec.itb.validation.commons.web.rest.model;

import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.*;

/**
 * Object providing the information on the validator's supported domains and validation types.
 */
@Schema(description = "The information on how to call the API methods (domain and validation types).")
public class ApiInfo {

    @Schema(description = "The domain value to use in all calls.")
    private String domain;
    @Schema(description = "The supported validation types.")
    private List<ValidationType> validationTypes = new ArrayList<>();

    /**
     * @param domain The value to use to identify the domain.
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * @return The value to use to identify the domain.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return The list of supported validation types for the domain.
     */
    public List<ValidationType> getValidationTypes() {
        return validationTypes;
    }

    /**
     * @param validationTypes The list of supported validation types for the domain.
     */
    public void setValidationTypes(List<ValidationType> validationTypes) {
        this.validationTypes = validationTypes;
    }

    /**
     * Construct the information on a specific domain from the domain's configuration.
     *
     * @param config The domain configuration.
     * @return The domain's API information (domain and validation types).
     */
    public static <T extends WebDomainConfig> ApiInfo fromDomainConfig(T config) {
        ApiInfo info = new ApiInfo();
        info.setDomain(config.getDomainName());
        var localisationHelper = new LocalisationHelper(config, Locale.ENGLISH);
        for (String type: config.getType()) {
            ValidationType typeInfo = new ValidationType();
            List<String> typeAliases = config.getValidationTypeAlias()
                    .entrySet()
                    .stream()
                    .filter(entry -> Objects.equals(entry.getValue(), type))
                    .map(Map.Entry::getKey).toList();

            typeInfo.setType(type);
            typeInfo.setDescription(config.getCompleteTypeOptionLabel(type, localisationHelper));
            if (!typeAliases.isEmpty()) {
                typeInfo.setAliases(typeAliases);
            }
            info.getValidationTypes().add(typeInfo);
        }
        return info;
    }

}
