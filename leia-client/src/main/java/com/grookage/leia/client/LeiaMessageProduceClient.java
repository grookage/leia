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

import com.grookage.leia.common.validation.LeiaMessageValidator;
import com.grookage.leia.common.validation.NoOpLeiaMessageValidator;
import com.grookage.leia.models.mux.LeiaMessage;
import com.grookage.leia.models.mux.MessageRequest;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.transformer.TransformationTarget;
import com.grookage.leia.models.utils.SchemaUtils;
import com.grookage.leia.mux.MessageProcessor;
import com.grookage.leia.mux.filter.BackendFilter;
import com.grookage.leia.mux.filter.NoOpBackendFilter;
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
import lombok.val;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
	private static final LeiaMessageValidator DEFAULT_MESSAGE_VALIDATOR = new NoOpLeiaMessageValidator();
	private final Map<SchemaKey, Map<String, JsonPath>> compiledPaths = new HashMap<>();
	private final Supplier<MessageProcessor> processorSupplier;
	private final Supplier<TargetValidator> targetValidator;
	private final LeiaMessageValidator leiaMessageValidator;

	/*
		Multiplexes from source and generates the list of messages as applicable
		a) Fetches the schemaDetails from schemaKey of source
		b) Checks if there is a transformationTarget, if none, returns source as is.
	 */
	@SneakyThrows
	private Optional<LeiaMessage> createMessage(MessageRequest messageRequest,
	                                            SchemaDetails sourceSchema,
	                                            TransformationTarget transformationTarget,
	                                            TargetValidator tValidator) {
		if (!validTarget(messageRequest, sourceSchema, transformationTarget, tValidator)) {
			log.error("Transformation target {} is not valid for source schemaKey {}",
					transformationTarget.getSchemaKey().getReferenceId(), messageRequest.getSchemaKey().getReferenceId());
			return Optional.empty();
		}
		final var targetSchema = SchemaUtils.getMatchingSchema(super.getSchemaDetails(), transformationTarget.getSchemaKey())
				.orElse(null);
		if (null == targetSchema) {
			log.error("No schema found for target schemaKey {}", transformationTarget.getSchemaKey());
			throw new UnsupportedOperationException("No valid schema found for target schemaKey " + transformationTarget.getSchemaKey().getReferenceId());
		}
		final var sourceContext = JsonPath.using(configuration).parse(messageRequest.getMessage());
		final var responseObject = MessageTransformerUtils.transformMessage(sourceContext, transformationTarget,
				getJsonPaths(transformationTarget.getSchemaKey()), getMapper());
		final var messageValidator = Objects.nonNull(this.leiaMessageValidator) ? this.leiaMessageValidator :
				DEFAULT_MESSAGE_VALIDATOR;
		final var validationErrors = messageValidator.validate(targetSchema, responseObject);
		if (!validationErrors.isEmpty()) {
			log.error("Transformed message validation failed for target schemaKey {} with errors {}",
					transformationTarget.getSchemaKey(), validationErrors);
			throw new IllegalStateException("Transformed message validation failed for target schemaKey "
					+ transformationTarget.getSchemaKey().getReferenceId() + " with errors " + validationErrors);
		}
		val tags = Stream.of(targetSchema.getTags(), transformationTarget.getTags())
				.flatMap(Collection::stream).collect(Collectors.toSet());
		return Optional.of(
				LeiaMessage.builder()
						.schemaKey(transformationTarget.getSchemaKey())
						.tags(tags)
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
			return messages;
		}
		transformationTargets.forEach(transformationTarget ->
				createMessage(messageRequest, sourceSchemaDetails, transformationTarget, tValidator)
						.ifPresent(message -> messages.put(message.getSchemaKey(), message)));
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

	@Deprecated(forRemoval = true, since = "1.1.0")
	public void processMessages(MessageRequest messageRequest,
	                            MessageProcessor messageProcessor,
	                            TargetValidator targetValidator) {
		processMessages(messageRequest, messageProcessor, targetValidator, new NoOpBackendFilter());
	}

	public void processMessages(MessageRequest messageRequest,
	                            MessageProcessor messageProcessor,
	                            TargetValidator targetValidator,
	                            BackendFilter backendFilter) {
		final var processor = null != messageProcessor ? messageProcessor : processorSupplier.get();
		if (null == processor) {
			log.error("No message processor hub supplied to process messages, call getMessages instead");
			throw new UnsupportedOperationException("No message processor hub found");
		}
		final var messages = getMessages(messageRequest, targetValidator).values().stream().toList();
		processor.processMessages(messages, null != backendFilter ? backendFilter : new NoOpBackendFilter());
	}

	@Override
	public void start() {
		compiledPaths.putAll(
				MessageTransformerUtils.getCompiledPaths(super.getSchemaDetails(), this::valid)
		);
	}
}
