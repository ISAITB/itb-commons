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

package eu.europa.ec.itb.validation.commons.web.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Object providing the information on the validator's supported domains and validation types.
 */
@Schema(description = "The application's version information.")
public class VersionInfo {

    @Schema(description = "The version number for the validator software.")
    private String versionNumber;
    @Schema(description = "The version's build timestamp.")
    private String buildTimestamp;

    /**
     * @return The version number.
     */
    public String getVersionNumber() {
        return versionNumber;
    }

    /**
     * @param versionNumber The version number.
     */
    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    /**
     * @return The build timestamp.
     */
    public String getBuildTimestamp() {
        return buildTimestamp;
    }

    /**
     * @param buildTimestamp The build timestamp.
     */
    public void setBuildTimestamp(String buildTimestamp) {
        this.buildTimestamp = buildTimestamp;
    }
}
