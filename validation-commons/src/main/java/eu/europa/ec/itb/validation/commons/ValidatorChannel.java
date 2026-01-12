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

package eu.europa.ec.itb.validation.commons;

/**
 * Enum used to identify a given validator channel.
 */
public enum ValidatorChannel {

    /** Web form for manual user-driven uploads via UI. */
    FORM("form"),
    /** Email submission. */
    EMAIL("email"),
    /** REST API call. */
	REST_API("rest_api"),
    /** SOAP API call. */
	SOAP_API("soap_api");

    private final String name;

    /**
     * Constructor
     *
     * @param name The channel name.
     */
    ValidatorChannel(String name) {
        this.name = name;
    }

    /**
     * @return The channel's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the validation channel corresponding to the given name.
     *
     * @param name The name.
     * @return The channel enum instance.
     * @throws IllegalArgumentException If the provided name does not match a specific channel.
     */
    public static ValidatorChannel byName(String name) {
        if (FORM.getName().equals(name)) {
            return FORM;
        } else if (EMAIL.getName().equals(name)) {
            return EMAIL;
        } else if (REST_API.getName().equals(name)) {
            return REST_API;
        } else if (SOAP_API.getName().equals(name)) {
            return SOAP_API;            
        } else {
            throw new IllegalArgumentException("Unknown validator channel ["+name+"]");
        }
    }
}
