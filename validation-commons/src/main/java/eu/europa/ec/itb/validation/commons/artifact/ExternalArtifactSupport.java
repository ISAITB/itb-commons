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

package eu.europa.ec.itb.validation.commons.artifact;

/**
 * Enum to specify whether external (user-provided) artifacts are supported.
 */
public enum ExternalArtifactSupport {

    /** External artifacts are mandatory. */
    REQUIRED("required"),
    /** External artifacts are optional. */
    OPTIONAL("optional"),
    /** External artifacts are not expected nor allowed. */
    NONE("none");

    private final String name;

    /**
     * @param name The enum instance's text value.
     */
    ExternalArtifactSupport(String name) {
        this.name = name;
    }

    /**
     * @return The enum's textual representation.
     */
    public String getName() {
        return name;
    }

    /**
     * Determine the enum instance corresponding to the provided text.
     *
     * @param name The enum text.
     * @return The enum instance.
     * @throws IllegalArgumentException if the provided name was invalid.
     */
    public static ExternalArtifactSupport byName(String name) {
        if (REQUIRED.name.equals(name)) {
            return REQUIRED;
        } else if (OPTIONAL.name.equals(name)) {
            return OPTIONAL;
        } else if (NONE.name.equals(name)) {
            return NONE;
        } else {
            throw new IllegalArgumentException("Unknown type name for external artifact support ["+name+"]");
        }
    }
}
