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

import java.util.List;

/**
 * Information on a type of validation artifact.
 */
public class ValidationArtifactInfo extends CommonValidationArtifactInfo {

    private String localPath;
    private ValidationArtifactCombinationApproach artifactCombinationApproach;
    private ValidationArtifactCombinationApproach externalArtifactCombinationApproach;
    private ExternalArtifactSupport externalArtifactSupport;
    private String externalArtifactPreProcessorPath;
    private String externalArtifactPreProcessorOutputExtension;
    private List<RemoteValidationArtifactInfo> remoteArtifacts;

    /**
     * @return The list of remotely loaded artifacts.
     */
    public List<RemoteValidationArtifactInfo> getRemoteArtifacts() {
        return remoteArtifacts;
    }

    /**
     * @param remoteArtifacts The list of remotely loaded artifacts.
     */
    public void setRemoteArtifacts(List<RemoteValidationArtifactInfo> remoteArtifacts) {
        this.remoteArtifacts = remoteArtifacts;
    }

    /**
     * @return The filesystem path relative to the domain's folder to load local validation artifacts from.
     */
    public String getLocalPath() {
        return localPath;
    }

    /**
     * @param localPath The filesystem path relative to the domain's folder to load local validation artifacts from.
     */
    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    /**
     * @return The way in which multiple artifacts are to be combined.
     */
    public ValidationArtifactCombinationApproach getArtifactCombinationApproach() {
        return artifactCombinationApproach;
    }

    /**
     * @param artifactCombinationApproach The way in which multiple artifacts are to be combined.
     */
    public void setArtifactCombinationApproach(ValidationArtifactCombinationApproach artifactCombinationApproach) {
        this.artifactCombinationApproach = artifactCombinationApproach;
    }

    /**
     * @return The support for external (user-provided) validation artifacts.
     */
    public ExternalArtifactSupport getExternalArtifactSupport() {
        return externalArtifactSupport;
    }

    /**
     * @param externalArtifactSupport The support for external (user-provided) validation artifacts.
     */
    public void setExternalArtifactSupport(ExternalArtifactSupport externalArtifactSupport) {
        this.externalArtifactSupport = externalArtifactSupport;
    }

    /**
     * @return The way in which multiple externally provided validation artifacts are to be combined.
     */
    public ValidationArtifactCombinationApproach getExternalArtifactCombinationApproach() {
        return externalArtifactCombinationApproach;
    }

    /**
     * @param externalArtifactCombinationApproach The way in which multiple externally provided validation artifacts are to be combined.
     */
    public void setExternalArtifactCombinationApproach(ValidationArtifactCombinationApproach externalArtifactCombinationApproach) {
        this.externalArtifactCombinationApproach = externalArtifactCombinationApproach;
    }

    /**
     * @return The filesystem path relative to the domain folder where the preprocessing file is located.
     */
    public String getExternalArtifactPreProcessorPath() {
        return externalArtifactPreProcessorPath;
    }

    /**
     * @param externalArtifactPreProcessorPath The filesystem path relative to the domain folder where the preprocessing file is located.
     */
    public void setExternalArtifactPreProcessorPath(String externalArtifactPreProcessorPath) {
        this.externalArtifactPreProcessorPath = externalArtifactPreProcessorPath;
    }

    /**
     * @return The file extension (without the dot) for the file produced after preprocessing external validation artifacts.
     */
    public String getExternalArtifactPreProcessorOutputExtension() {
        return externalArtifactPreProcessorOutputExtension;
    }

    /**
     * @param externalArtifactPreProcessorOutputExtension The file extension (without the dot) for the file produced after preprocessing external validation artifacts.
     */
    public void setExternalArtifactPreProcessorOutputExtension(String externalArtifactPreProcessorOutputExtension) {
        this.externalArtifactPreProcessorOutputExtension = externalArtifactPreProcessorOutputExtension;
    }
}
