package eu.europa.ec.itb.validation.commons.artifact;

public abstract class CommonValidationArtifactInfo {

    private String type;
    private String preProcessorPath;
    private String preProcessorOutputExtension;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPreProcessorPath() {
        return preProcessorPath;
    }

    public void setPreProcessorPath(String preProcessorPath) {
        this.preProcessorPath = preProcessorPath;
    }

    public String getPreProcessorOutputExtension() {
        return preProcessorOutputExtension;
    }

    public void setPreProcessorOutputExtension(String preProcessorOutputExtension) {
        this.preProcessorOutputExtension = preProcessorOutputExtension;
    }
}
