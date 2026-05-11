/*
 * Copyright (C) 2026 European Union
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

package eu.europa.ec.itb.validation.commons.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gitb.core.AnyContent;
import com.gitb.tr.BAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the serialisation of TAR reports to JSON.
 * <p> <p>
 * This is not a Spring Bean configuration class to avoid side effects to the
 * ObjectMapper used internally by Spring Boot.
 */
public final class JsonConfig {

    private JsonConfig() {
        // Private constructor to prevent instantiation.
    }

    /**
     * Create the Jackson ObjectMapper.
     *
     * @return The object mapper.
     */
    public static ObjectMapper objectMapper() {
        var module = new SimpleModule("TAR");
        module.addSerializer(TestAssertionGroupReportsType.class, new TestAssertionGroupReportsTypeSerializer());
        module.addSerializer(AnyContent.class, new AnyContentSerializer());
        return JsonMapper.builder()
                .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .addModule(module)
                .build();
    }

    /**
     * Serializer for TAR report items.
     */
    static class TestAssertionGroupReportsTypeSerializer extends ValueSerializer<TestAssertionGroupReportsType> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(TestAssertionGroupReportsType type, JsonGenerator json, SerializationContext serializerProvider) {
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
                json.writePOJOProperty("error", errors);
            }
            if (!warnings.isEmpty()) {
                json.writePOJOProperty("warning", warnings);
            }
            if (!messages.isEmpty()) {
                json.writePOJOProperty("info", messages);
            }
            json.writeEndObject();
        }

    }

    /**
     * Serializer for context map entries.
     */
    static class AnyContentSerializer extends ValueSerializer<AnyContent> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(AnyContent type, JsonGenerator json, SerializationContext serializerProvider) {
            json.writeStartObject();
            if (!type.getItem().isEmpty() || StringUtils.isNotEmpty(type.getValue())) {
                if (StringUtils.isNotEmpty(type.getName())) json.writeStringProperty("name", type.getName());
                if (StringUtils.isNotEmpty(type.getType())) json.writeStringProperty("type", type.getType());
                if (!type.getItem().isEmpty()) {
                    json.writePOJOProperty("items", type.getItem());
                } else {
                    if (StringUtils.isNotEmpty(type.getMimeType())) json.writeStringProperty("mimeType", type.getMimeType());
                    if (type.getEmbeddingMethod() != null) json.writePOJOProperty("embeddingMethod", type.getEmbeddingMethod());
                    if (StringUtils.isNotEmpty(type.getEncoding())) json.writeStringProperty("encoding", type.getEncoding());
                    json.writeStringProperty("value", type.getValue());
                }
            }
            json.writeEndObject();
        }
    }
}
