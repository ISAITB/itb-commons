package eu.europa.ec.itb.validation.commons.artifact;

/**
 * Information on remotely loaded validation artifacts.
 */
public class RemoteValidationArtifactInfo extends CommonValidationArtifactInfo {

    private String url;

    /**
     * @return The URL to load the artifact from.
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url The URL to load the artifact from.
     */
    public void setUrl(String url) {
        this.url = url;
    }
}
