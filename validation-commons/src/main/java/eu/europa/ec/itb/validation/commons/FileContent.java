package eu.europa.ec.itb.validation.commons;

import com.gitb.core.ValueEmbeddingEnumeration;

public class FileContent {

    public static final String embedding_URL     	= "URL" ;
    public static final String embedding_BASE64		= "BASE64" ;
    public static final String embedding_STRING		= "STRING" ;

    private String content;
    private ValueEmbeddingEnumeration embeddingMethod;
    private String contentType;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ValueEmbeddingEnumeration getEmbeddingMethod() {
        return embeddingMethod;
    }

    public void setEmbeddingMethod(ValueEmbeddingEnumeration embeddingMethod) {
        this.embeddingMethod = embeddingMethod;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

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

    public static boolean isValidEmbeddingMethod(String type) {
        return embedding_URL.equals(type) || ValueEmbeddingEnumeration.BASE_64.name().equals(type) || ValueEmbeddingEnumeration.URI.name().equals(type) || ValueEmbeddingEnumeration.STRING.name().equals(type);
    }

}
