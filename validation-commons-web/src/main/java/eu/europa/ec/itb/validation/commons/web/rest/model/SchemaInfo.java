package eu.europa.ec.itb.validation.commons.web.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.europa.ec.itb.validation.commons.FileContent;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A user-provided set of schemas to use in the validation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "A set of user-provided schemas to apply to the validation.")
public class SchemaInfo {

    @Schema(required = true, description = "The schema to apply.")
    private String schema;
    @Schema(description = "The way in which to interpret the value for schema. If not provided, the method will be determined from the schema value itself.", allowableValues = FileContent.EMBEDDING_STRING+","+FileContent.EMBEDDING_URL+","+FileContent.EMBEDDING_BASE_64)
    private String embeddingMethod;

    /**
     * @return The schema to apply.
     */
    public String getSchema() { return this.schema; }

    /**
     * @return The way in which to interpret the value for ruleSet. If not provided, the method will be determined from
     * the schema value itself.
     */
    public String getEmbeddingMethod() { return this.embeddingMethod; }

    /**
     * @param schema The schema to apply.
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * @param embeddingMethod The way in which to interpret the value for schema. If not provided, the method will be
     *                        determined from the schema value itself.
     */
    public void setEmbeddingMethod(String embeddingMethod) {
        this.embeddingMethod = embeddingMethod;
    }

    /**
     * Wrap the rule set's information metadata into a FileContent instance.
     *
     * @return The rule set information.
     */
    public FileContent toFileContent() {
        FileContent content = new FileContent();
        content.setContent(getSchema());
        content.setEmbeddingMethod(FileContent.embeddingMethodFromString(getEmbeddingMethod()));
        return content;
    }
}
