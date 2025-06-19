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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * A supported validation type for a specific domain.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Information on an available validation type that can be requested.")
public class ValidationType {

    @Schema(description = "The value to use when requesting the validation type.")
    private String type;
    @Schema(description = "The validation type's description.")
    private String description;
    @Schema(description = "The validation type aliases for this type.")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> aliases;

    /**
     * @return The validation type identifier.
     */
    public String getType() {
        return type;
    }

    /**
     * @param type The validation type identifier.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return The validation type description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description The validation type description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return The validation type aliases.
     */
    public List<String> getAliases() {
        return this.aliases;
    }

    /**
      * @param aliases The list of aliases for the configured validation types.
     */
    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }
}
