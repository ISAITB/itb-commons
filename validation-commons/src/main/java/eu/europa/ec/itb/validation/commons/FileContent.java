/*
 * Copyright (C) 2025 European Union
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

package eu.europa.ec.itb.validation.commons;

import com.gitb.core.ValueEmbeddingEnumeration;

/**
 * Class used to record the information on a specific file.
 */
public class FileContent {

    /** Content is to be treated as a URL. */
    public static final String EMBEDDING_URL = "URL" ;
    /** Content is to be treated as a BASE64 encoded string. */
    public static final String EMBEDDING_BASE_64 = "BASE64" ;
    /** Content is to be treated as a plain string. */
    public static final String EMBEDDING_STRING = "STRING" ;

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
            if (EMBEDDING_URL.equals(method)) {
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
        return EMBEDDING_URL.equals(type) || ValueEmbeddingEnumeration.BASE_64.name().equals(type) || ValueEmbeddingEnumeration.URI.name().equals(type) || ValueEmbeddingEnumeration.STRING.name().equals(type);
    }

}
