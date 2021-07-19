package eu.europa.ec.itb.validation.commons.artifact;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Artifact information
 */
public class TypedValidationArtifactInfo {

    /**
     * Default artifact type used when there is only a single, unnamed type.
     */
    public static final String DEFAULT_TYPE = "default";

    private final Map<String, ValidationArtifactInfo> perArtifactTypeMap = new HashMap<>();

    /**
     * @return The default or single artifact information.
     */
    public ValidationArtifactInfo get() {
        return perArtifactTypeMap.values().iterator().next();
    }

    /**
     * @param artifactType The artifact type.
     * @return The artifact's information.
     */
    public ValidationArtifactInfo get(String artifactType) {
        return perArtifactTypeMap.get(StringUtils.defaultString(artifactType, DEFAULT_TYPE));
    }

    /**
     * Record a new artifact type to the configuration.
     *
     * @param artifactType The type.
     * @param artifactInfo The type's information.
     */
    public void add(String artifactType, ValidationArtifactInfo artifactInfo) {
        perArtifactTypeMap.put(StringUtils.defaultString(artifactType, DEFAULT_TYPE), artifactInfo);
    }

    /**
     * @return Get the validation types (not artifact types) recorded in the configuration.
     */
    public Set<String> getTypes() {
        return perArtifactTypeMap.keySet();
    }

    /**
     * @return true in case any artifact type has preconfigured artifacts, either local or remote.
     */
    public boolean hasPreconfiguredArtifacts() {
        for (ValidationArtifactInfo artifactInfo: perArtifactTypeMap.values()) {
            if (StringUtils.isNotBlank(artifactInfo.getLocalPath()) || (artifactInfo.getRemoteArtifacts() != null && !artifactInfo.getRemoteArtifacts().isEmpty())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return The overall support for externally-provided (user-provided) artifacts for all validation types and artifact types.
     */
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
