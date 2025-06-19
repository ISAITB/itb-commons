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

package eu.europa.ec.itb.validation.commons.web;

/**
 * DTO to record a key and label pair.
 */
public class KeyWithLabel {

    private final String key;
    private final String label;

    /**
     * Constructor.
     *
     * @param key The key.
     * @param label The label.
     */
    public KeyWithLabel(String key, String label) {
        this.key = key;
        this.label = label;
    }

    /**
     * @return The key.
     */
    public String getKey() {
        return key;
    }

    /**
     * @return The label.
     */
    public String getLabel() {
        return label;
    }

}
