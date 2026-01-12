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

package eu.europa.ec.itb.validation.commons.report.dto;

/**
 * DTO class for items placed in reports' context maps.
 */
public class ContextItem {

    private String key;
    private String value;

    /**
     * @return The map key (item name).
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key The map key (item name).
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @return The item's value.
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value The item's value.
     */
    public void setValue(String value) {
        this.value = value;
    }
}
