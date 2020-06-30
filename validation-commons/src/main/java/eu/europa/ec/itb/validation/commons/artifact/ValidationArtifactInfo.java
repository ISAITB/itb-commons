package eu.europa.ec.itb.validation.commons.artifact;

import java.util.List;

public class ValidationArtifactInfo extends CommonValidationArtifactInfo {

    private String localPath;
    private ValidationArtifactCombinationApproach artifactCombinationApproach;
    private ValidationArtifactCombinationApproach externalArtifactCombinationApproach;
    private ExternalArtifactSupport externalArtifactSupport;
    private String externalArtifactPreProcessorPath;
    private String externalArtifactPreProcessorOutputExtension;
    private List<RemoteValidationArtifactInfo> remoteArtifacts;

    public List<RemoteValidationArtifactInfo> getRemoteArtifacts() {
        return remoteArtifacts;
    }

    public void setRemoteArtifacts(List<RemoteValidationArtifactInfo> remoteArtifacts) {
        this.remoteArtifacts = remoteArtifacts;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public ValidationArtifactCombinationApproach getArtifactCombinationApproach() {
        return artifactCombinationApproach;
    }

    public void setArtifactCombinationApproach(ValidationArtifactCombinationApproach artifactCombinationApproach) {
        this.artifactCombinationApproach = artifactCombinationApproach;
    }

    public ExternalArtifactSupport getExternalArtifactSupport() {
        return externalArtifactSupport;
    }

    public void setExternalArtifactSupport(ExternalArtifactSupport externalArtifactSupport) {
        this.externalArtifactSupport = externalArtifactSupport;
    }

    public ValidationArtifactCombinationApproach getExternalArtifactCombinationApproach() {
        return externalArtifactCombinationApproach;
    }

    public void setExternalArtifactCombinationApproach(ValidationArtifactCombinationApproach externalArtifactCombinationApproach) {
        this.externalArtifactCombinationApproach = externalArtifactCombinationApproach;
    }

    public String getExternalArtifactPreProcessorPath() {
        return externalArtifactPreProcessorPath;
    }

    public void setExternalArtifactPreProcessorPath(String externalArtifactPreProcessorPath) {
        this.externalArtifactPreProcessorPath = externalArtifactPreProcessorPath;
    }

    public String getExternalArtifactPreProcessorOutputExtension() {
        return externalArtifactPreProcessorOutputExtension;
    }

    public void setExternalArtifactPreProcessorOutputExtension(String externalArtifactPreProcessorOutputExtension) {
        this.externalArtifactPreProcessorOutputExtension = externalArtifactPreProcessorOutputExtension;
    }
}
