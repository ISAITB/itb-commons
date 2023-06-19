package eu.europa.ec.itb.validation.commons.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gitb.core.AnyContent;
import com.gitb.tr.BAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the serialisation of TAR reports to JSON.
 *
 * This is not a Spring Bean configuration class to avoid side effects to the
 * ObjectMapper used internally by Spring Boot.
 */
public class JsonConfig {

    /**
     * Create the Jackson ObjectMapper.
     *
     * @return The object mapper.
     */
    public static ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
        var module = new SimpleModule("TAR");
        module.addSerializer(TestAssertionGroupReportsType.class, new TestAssertionGroupReportsTypeSerializer());
        module.addSerializer(AnyContent.class, new AnyContentSerializer());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.registerModule(module);
        return mapper;
    }

    /**
     * Serializer for TAR report items.
     */
    static class TestAssertionGroupReportsTypeSerializer extends JsonSerializer<TestAssertionGroupReportsType> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(TestAssertionGroupReportsType type, JsonGenerator json, SerializerProvider serializerProvider) throws IOException {
            json.writeStartObject();
            List<BAR> errors = new ArrayList<>();
            List<BAR> warnings = new ArrayList<>();
            List<BAR> messages = new ArrayList<>();
            for (var item: type.getInfoOrWarningOrError()) {
                if (item.getValue() instanceof BAR itemValue) {
                    if ("error".equals(item.getName().getLocalPart())) {
                        errors.add(itemValue);
                    } else if ("warning".equals(item.getName().getLocalPart())) {
                        warnings.add(itemValue);
                    } else {
                        messages.add(itemValue);
                    }
                }
            }
            if (!errors.isEmpty()) {
                json.writeObjectField("error", errors);
            }
            if (!warnings.isEmpty()) {
                json.writeObjectField("warning", warnings);
            }
            if (!messages.isEmpty()) {
                json.writeObjectField("info", messages);
            }
            json.writeEndObject();
        }

    }

    /**
     * Serializer for context map entries.
     */
    static class AnyContentSerializer extends JsonSerializer<AnyContent> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(AnyContent type, JsonGenerator json, SerializerProvider serializerProvider) throws IOException {
            json.writeStartObject();
            if (!type.getItem().isEmpty() || StringUtils.isNotEmpty(type.getValue())) {
                if (StringUtils.isNotEmpty(type.getName())) json.writeStringField("name", type.getName());
                if (StringUtils.isNotEmpty(type.getType())) json.writeStringField("type", type.getType());
                if (!type.getItem().isEmpty()) {
                    json.writeObjectField("items", type.getItem());
                } else {
                    if (StringUtils.isNotEmpty(type.getMimeType())) json.writeStringField("mimeType", type.getMimeType());
                    if (type.getEmbeddingMethod() != null) json.writeObjectField("embeddingMethod", type.getEmbeddingMethod());
                    if (StringUtils.isNotEmpty(type.getEncoding())) json.writeStringField("encoding", type.getEncoding());
                    json.writeStringField("value", type.getValue());
                }
            }
            json.writeEndObject();
        }
    }
}
