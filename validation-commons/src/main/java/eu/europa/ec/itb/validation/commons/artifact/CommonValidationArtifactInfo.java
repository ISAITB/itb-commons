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
