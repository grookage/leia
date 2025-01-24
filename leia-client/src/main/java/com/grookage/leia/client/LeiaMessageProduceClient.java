/*
 * Copyright (c) 2024. Koushik R <rkoushik.14@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.grookage.leia.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.grookage.leia.models.mux.LeiaMessage;
import com.grookage.leia.models.mux.MessageRequest;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.transformer.TransformationTarget;
import com.grookage.leia.models.utils.SchemaUtils;
import com.grookage.leia.mux.processors.MessageProcessor;
import com.grookage.leia.mux.processors.TargetValidator;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;


@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@Slf4j
public class LeiaMessageProduceClient extends AbstractSchemaClient {

    private static final Configuration configuration = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build();
    private final Map<SchemaKey, Map<String, JsonPath>> compiledPaths = new HashMap<>();
    private final Supplier<MessageProcessor> messageProcessor;
    private final Supplier<TargetValidator> targetValidator;

    /*
        Multiplexes from source and generates the list of messages as applicable
        a) Fetches the schemaDetails from schemaKey of source
        b) Checks if there is a transformationTarget, if none, returns source as is.
     */
    @SneakyThrows
    private Optional<LeiaMessage> createMessage(DocumentContext sourceContext,
                                                TransformationTarget transformationTarget) {
        final var registeredKlass = getSchemaValidator()
                .getKlass(transformationTarget.getSchemaKey()).orElse(null);
        if (null == registeredKlass) {
            return Optional.empty();
        }
        final var responseObject = JsonNodeFactory.instance.objectNode();
        transformationTarget.getTransformers().forEach(transformer -> {
            final var jsonPath = getJsonPath(transformationTarget.getSchemaKey(), transformer.getAttributeName())
                    .orElse(null);
            if (null != jsonPath) {
                final JsonNode attribute = sourceContext.read(jsonPath);
                responseObject.set(transformer.getAttributeName(), attribute);
            }
        });
        getMapper().convertValue(responseObject, registeredKlass); //Do this to do the schema validation of if the conversion is right or not.
        return Optional.of(
                LeiaMessage.builder()
                        .schemaKey(transformationTarget.getSchemaKey())
                        .tags(transformationTarget.getTags())
                        .message(responseObject)
                        .build()
        );
    }

    private Optional<JsonPath> getJsonPath(SchemaKey schemaKey, String attributeName) {
        return compiledPaths.containsKey(schemaKey) ?
                Optional.ofNullable(compiledPaths.get(schemaKey).get(attributeName)) : Optional.empty();
    }

    public Map<SchemaKey, LeiaMessage> getMessages(MessageRequest messageRequest,
                                                   TargetValidator tValidator) {
        final var messages = new HashMap<SchemaKey, LeiaMessage>();
        if (messageRequest.isIncludeSource()) {
            messages.put(messageRequest.getSchemaKey(), LeiaMessage.builder()
                    .schemaKey(messageRequest.getSchemaKey())
                    .message(messageRequest.getMessage())
                    .build()
            );
        }
        final var sourceSchemaDetails = SchemaUtils.getMatchingSchema(super.getSchemaDetails(), messageRequest.getSchemaKey())
                .orElse(null);
        final var transformationTargets = null == sourceSchemaDetails ? null :
                sourceSchemaDetails.getTransformationTargets();
        if (null == transformationTargets) {
            return messages;
        }
        final var validator = null != tValidator ? tValidator : targetValidator.get();
        final var documentContext = JsonPath.using(configuration).parse(messageRequest.getMessage());
        transformationTargets.
                stream().filter(each -> null == validator || validator.validate(
                        each, messageRequest, sourceSchemaDetails
                ))
                .forEach(transformationTarget ->
                        createMessage(documentContext, transformationTarget).ifPresent(message ->
                                messages.put(message.getSchemaKey(), message)));
        return messages;
    }

    public void processMessages(MessageRequest messageRequest,
                                MessageProcessor mProcessor,
                                TargetValidator retriever) {
        final var processor = null != mProcessor ? mProcessor : messageProcessor.get();
        processor.processMessages(getMessages(messageRequest, retriever));
    }

    @Override
    public void start() {
        super.getSchemaDetails().forEach(schemaDetails -> {
            final var schemaKey = schemaDetails.getSchemaKey();
            final var validSchema = super.valid(schemaKey);
            if (!validSchema) {
                log.error("The source schema doesn't seem to be valid for schemaKey {}. Please check the schema bindings provided",
                        schemaKey);
                throw new IllegalStateException("Invalid source schema");
            }
            final var transformationTargets = schemaDetails.getTransformationTargets();
            transformationTargets.forEach(transformationTarget -> {
                final var valid = super.valid(transformationTarget.getSchemaKey());
                if (!valid) {
                    log.error("The transformationSchema schema doesn't seem to be valid for schemaKey {}. Please check the schema bindings provided",
                            transformationTarget.getSchemaKey());
                    throw new IllegalStateException("Invalid transformation schema");
                }
                final var paths = new HashMap<String, JsonPath>();
                transformationTarget.getTransformers().forEach(transformer -> paths.put(transformer.getAttributeName(),
                        JsonPath.compile(transformer.getTransformationPath())));
                compiledPaths.put(transformationTarget.getSchemaKey(), paths);
            });
        });
    }
}
