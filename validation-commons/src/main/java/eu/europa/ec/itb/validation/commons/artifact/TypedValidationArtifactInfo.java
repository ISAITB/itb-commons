package eu.europa.ec.itb.validation.commons.artifact;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypedValidationArtifactInfo {

    public static final String DEFAULT_TYPE = "default";

    private Map<String, ValidationArtifactInfo> perArtifactTypeMap = new HashMap<>();

    public ValidationArtifactInfo get() {
        return perArtifactTypeMap.values().iterator().next();
    }

    public ValidationArtifactInfo get(String artifactType) {
        return perArtifactTypeMap.get(StringUtils.defaultString(artifactType, DEFAULT_TYPE));
    }

    public void add(String artifactType, ValidationArtifactInfo artifactInfo) {
        perArtifactTypeMap.put(StringUtils.defaultString(artifactType, DEFAULT_TYPE), artifactInfo);
    }

    public Set<String> getTypes() {
        return perArtifactTypeMap.keySet();
    }

    public boolean hasPreconfiguredArtifacts() {
        for (ValidationArtifactInfo artifactInfo: perArtifactTypeMap.values()) {
            if (StringUtils.isNotBlank(artifactInfo.getLocalPath()) || !(artifactInfo.getRemoteArtifacts() != null && artifactInfo.getRemoteArtifacts().isEmpty())) {
                return true;
            }
        }
        return false;
    }

    public ExternalArtifactSupport getOverallExternalArtifactSupport() {
        ExternalArtifactSupport supportToReturn = ExternalArtifactSupport.NONE;
        for (ValidationArtifactInfo artifactInfo: perArtifactTypeMap.values()) {
            if (artifactInfo.getExternalArtifactSupport() == ExternalArtifactSupport.REQUIRED) {
                supportToReturn = ExternalArtifactSupport.REQUIRED;
                break;
            } else if (artifactInfo.getExternalArtifactSupport() == ExternalArtifactSupport.OPTIONAL) {
                supportToReturn = ExternalArtifactSupport.OPTIONAL;
            }
        }
        return supportToReturn;
    }

}
