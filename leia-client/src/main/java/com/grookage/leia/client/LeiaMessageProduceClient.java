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

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.grookage.leia.models.mux.LeiaMessage;
import com.grookage.leia.models.mux.MessageRequest;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.transformer.TransformationTarget;
import com.grookage.leia.models.utils.MetricUtils;
import com.grookage.leia.models.utils.SchemaUtils;
import com.grookage.leia.mux.MessageProcessor;
import com.grookage.leia.mux.targetvalidator.DefaultTargetValidator;
import com.grookage.leia.mux.targetvalidator.TargetValidator;
import com.jayway.jsonpath.Configuration;
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
import java.util.Objects;
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
    private static final TargetValidator DEFAULT_VALIDATOR = new DefaultTargetValidator();
    private final Map<SchemaKey, Map<String, JsonPath>> compiledPaths = new HashMap<>();
    private final Supplier<MessageProcessor> processorSupplier;
    private final Supplier<TargetValidator> targetValidator;
    private final MetricRegistry metricRegistry;

    /*
        Multiplexes from source and generates the list of messages as applicable
        a) Fetches the schemaDetails from schemaKey of source
        b) Checks if there is a transformationTarget, if none, returns source as is.
     */
    @SneakyThrows
    private Optional<LeiaMessage> createMessage(MessageRequest messageRequest,
                                                SchemaDetails schemaDetails,
                                                TransformationTarget transformationTarget,
                                                TargetValidator tValidator) {
        Preconditions.checkNotNull(metricRegistry,"Metric Registry can't be null");
        if (!validTarget(messageRequest, schemaDetails, transformationTarget, tValidator)) {
            log.debug("Criteria not matched for transformation target {} of message {} ", transformationTarget.getSchemaKey(), messageRequest.getSchemaKey());
            return Optional.empty();
        }
        final var registeredKlass = getSchemaValidator()
                .getKlass(transformationTarget.getSchemaKey()).orElse(null);
        if (null == registeredKlass) {
            log.debug("No class has been registered against schema {}",transformationTarget.getSchemaKey());
            return Optional.empty();
        }
        final var sourceContext = JsonPath.using(configuration).parse(messageRequest.getMessage());

        final var responseObject = MessageTransformerUtils.transformMessage(sourceContext, transformationTarget,
                getJsonPaths(transformationTarget.getSchemaKey()), getMapper());
        getMapper().convertValue(responseObject, registeredKlass); //Do this to do the schema validation of if the conversion is right or not.
        return Optional.of(
                LeiaMessage.builder()
                        .schemaKey(transformationTarget.getSchemaKey())
                        .tags(transformationTarget.getTags())
                        .message(responseObject)
                        .build()
        );
    }

    private Map<String, JsonPath> getJsonPaths(SchemaKey schemaKey) {
        return compiledPaths.containsKey(schemaKey) ? compiledPaths.get(schemaKey) : Map.of();
    }

    public Map<SchemaKey, LeiaMessage> getMessages(MessageRequest messageRequest,
                                                   TargetValidator tValidator) {
        final var messages = new HashMap<SchemaKey, LeiaMessage>();
        final var sourceSchemaDetails = SchemaUtils.getMatchingSchema(super.getSchemaDetails(), messageRequest.getSchemaKey())
                .orElse(null);
        if (null == sourceSchemaDetails) {
            log.error("No schema found for schemaKey {}", messageRequest.getSchemaKey());
            throw new UnsupportedOperationException("No valid schema found for schemaKey " + messageRequest.getSchemaKey().getReferenceId());
        }

        if (messageRequest.isIncludeSource()) {
            messages.put(messageRequest.getSchemaKey(), LeiaMessage.builder()
                    .schemaKey(sourceSchemaDetails.getSchemaKey())
                    .message(messageRequest.getMessage())
                    .tags(sourceSchemaDetails.getTags())
                    .build()
            );
        }
        final var transformationTargets = sourceSchemaDetails.getTransformationTargets();
        if (null == transformationTargets) {
            log.debug("No transformation target present for message: {}",messages);
            return messages;
        }
        transformationTargets.forEach(transformationTarget -> {
            final var message = createMessage(messageRequest, sourceSchemaDetails, transformationTarget, tValidator).orElse(null);
            if (null != message) {
                messages.put(message.getSchemaKey(), message);
                publishMetric(message.getSchemaKey(),Joiner.on(".").join(MetricUtils.TRANSFORMATION,MetricUtils.SUCCESS));
                publishMetric(transformationTarget.getSchemaKey(),"");
            }
            publishMetric(messageRequest.getSchemaKey(),Joiner.on(".").join(MetricUtils.TRANSFORMATION,MetricUtils.SKIPPED));

        });
        return messages;
    }

    public boolean validTarget(MessageRequest messageRequest,
                               SchemaDetails schemaDetails,
                               TransformationTarget transformationTarget,
                               TargetValidator tValidator) {
        final var initiatedValidator = null != targetValidator ? targetValidator.get() : null;
        final var validator = Objects.requireNonNullElseGet(tValidator, () -> Objects.requireNonNullElse(initiatedValidator, DEFAULT_VALIDATOR));
        return validator.validate(transformationTarget, messageRequest, schemaDetails);
    }

    private void publishMetric(SchemaKey schemaKey, String eventStatus){
        final var canonicalName = this.getClass().getCanonicalName();
        final var prefix = (null != canonicalName)?canonicalName:MetricUtils.PREFIX;
        metricRegistry.meter(Joiner.on(".")
                .skipNulls()
                .join(prefix,this.getClass().getSimpleName(),MetricUtils.MESSAGE,
                MetricUtils.getMetricKey(schemaKey),eventStatus));
    }

    public void processMessages(MessageRequest messageRequest,
                                MessageProcessor messageProcessor,
                                TargetValidator retriever) {
        final var processor = null != messageProcessor ? messageProcessor : processorSupplier.get();
        if (null == processor) {
            log.error("No message processor hub supplied to process messages, call getMessages instead");
            throw new UnsupportedOperationException("No message processor hub found");
        }
        final var messages = getMessages(messageRequest, retriever).values().stream().toList();
        processor.processMessages(messages);
    }

    @Override
    public void start() {
        compiledPaths.putAll(
                MessageTransformerUtils.getCompiledPaths(super.getSchemaDetails(), this::valid)
        );
    }
}
