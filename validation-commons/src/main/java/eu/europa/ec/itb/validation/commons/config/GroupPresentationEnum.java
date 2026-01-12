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

package eu.europa.ec.itb.validation.commons.config;

import java.util.Objects;
import java.util.Optional;

/**
 * Enumeration for the different ways to present a validation type group.
 */
public enum GroupPresentationEnum {

    /**
     * Presentation inline as select option groups.
     */
    INLINE("inline"),
    /**
     * Presentation as separate select.
     */
    SPLIT("split");

    private final String identifier;

    /**
     * Constructor.
     *
     * @param identifier The identifier used to configure and match the presentation type.
     */
    GroupPresentationEnum(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return The presentation type's identifier.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Parse the presentation type from the provided value.
     *
     * @param value The value to parse.
     * @return The presentation type of empty if invalid.
     */
    public static Optional<GroupPresentationEnum> of(String value) {
        if (value != null) {
            value = value.toLowerCase();
        }
        if (Objects.equals(value, INLINE.identifier)) {
          return Optional.of(INLINE);
        } else if (Objects.equals(value, SPLIT.identifier)) {
            return Optional.of(SPLIT);
        } else {
            return Optional.empty();
        }
    }

}
