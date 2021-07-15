package eu.europa.ec.itb.validation.commons;

import com.gitb.core.ValueEmbeddingEnumeration;

/**
 * Class used to record the information on a specific file.
 */
public class FileContent {

    /** Content is to be treated as a URL. */
    public static final String embedding_URL     	= "URL" ;
    /** Content is to be treated as a BASE64 encoded string. */
    public static final String embedding_BASE64		= "BASE64" ;
    /** Content is to be treated as a plain string. */
    public static final String embedding_STRING		= "STRING" ;

    private String content;
    private ValueEmbeddingEnumeration embeddingMethod;
    private String contentType;

    /**
     * @return The content string.
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content The content string.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @return The embedding method to consider.
     */
    public ValueEmbeddingEnumeration getEmbeddingMethod() {
        return embeddingMethod;
    }

    /**
     * @param embeddingMethod The embedding method to consider.
     */
    public void setEmbeddingMethod(ValueEmbeddingEnumeration embeddingMethod) {
        this.embeddingMethod = embeddingMethod;
    }

    /**
     * @return The content type.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @param contentType The content type.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Get the embedding method that corresponds to the provided string value.
     *
     * @param method The corresponding embedding method.
     * @return The resolved method.
     */
    public static ValueEmbeddingEnumeration embeddingMethodFromString(String method) {
        if (method == null) {
            return null;
        } else {
            if (embedding_URL.equals(method)) {
                return ValueEmbeddingEnumeration.URI;
            }
            return ValueEmbeddingEnumeration.fromValue(method);
        }
    }

    /**
     * Check to see if the provided embedding method string is valid.
     *
     * @param type The type to check.
     * @return The check result.
     */
    public static boolean isValidEmbeddingMethod(String type) {
        return embedding_URL.equals(type) || ValueEmbeddingEnumeration.BASE_64.name().equals(type) || ValueEmbeddingEnumeration.URI.name().equals(type) || ValueEmbeddingEnumeration.STRING.name().equals(type);
    }

}
