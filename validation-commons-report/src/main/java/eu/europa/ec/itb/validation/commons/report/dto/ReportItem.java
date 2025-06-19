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

package eu.europa.ec.itb.validation.commons.report.dto;

/**
 * DTO for a given report item to include in a report.
 */
public class ReportItem {

    private String level;
    private String description;
    private String test;
    private String location;
    private String assertionId;

    /**
     * @return The item's level.
     */
    public String getLevel() {
        return level;
    }

    /**
     * @param level The item's level.
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * @return The item's description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description The item's description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return The item's test.
     */
    public String getTest() {
        return test;
    }

    /**
     * @param test The item's test.
     */
    public void setTest(String test) {
        this.test = test;
    }

    /**
     * @return The item's location marker.
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location The item's location marker.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return The item's assertion ID.
     */
    public String getAssertionId() {
        return assertionId;
    }

    /**
     * @param assertionId The item's assertion ID.
     */
    public void setAssertionId(String assertionId) {
        this.assertionId = assertionId;
    }
}
