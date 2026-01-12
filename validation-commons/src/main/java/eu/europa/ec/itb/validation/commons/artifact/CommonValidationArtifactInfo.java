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

package eu.europa.ec.itb.validation.commons.artifact;

/**
 * Holds the common properties linked to all validation artifacts.
 */
public abstract class CommonValidationArtifactInfo {

    private String type;
    private String preProcessorPath;
    private String preProcessorOutputExtension;

    /**
     * @return The artifact type.
     */
    public String getType() {
        return type;
    }

    /**
     * @param type The artifact type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return The relative path to load the preprocessing artifact.
     */
    public String getPreProcessorPath() {
        return preProcessorPath;
    }

    /**
     * @param preProcessorPath The relative path to load the preprocessing artifact.
     */
    public void setPreProcessorPath(String preProcessorPath) {
        this.preProcessorPath = preProcessorPath;
    }

    /**
     * @return The file extension (without the dot) of the file resulting from preprocessing.
     */
    public String getPreProcessorOutputExtension() {
        return preProcessorOutputExtension;
    }

    /**
     * @param preProcessorOutputExtension The file extension (without the dot) of the file resulting from preprocessing.
     */
    public void setPreProcessorOutputExtension(String preProcessorOutputExtension) {
        this.preProcessorOutputExtension = preProcessorOutputExtension;
    }
}
