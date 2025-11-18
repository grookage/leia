/*
 * Copyright (c) 2025. Koushik R <rkoushik.14@gmail.com>.
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.transformer.AttributeTransformer;
import com.grookage.leia.models.schema.transformer.TransformationTarget;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@UtilityClass
@Slf4j
public class MessageTransformerUtils {

	private static final String LITERAL = "~";

	public static Map<SchemaKey, Map<String, JsonPath>> getCompiledPaths(List<SchemaDetails> schemas,
	                                                                     Predicate<SchemaKey> schemaPredicate) {
		final var compiledPaths = new HashMap<SchemaKey, Map<String, JsonPath>>();
		schemas.forEach(schemaDetails -> {
			if (!schemaPredicate.test(schemaDetails.getSchemaKey())) {
				return;
			}
			final var transformationTargets = schemaDetails.getTransformationTargets();
			transformationTargets.forEach(transformationTarget -> {
				final var valid = schemaPredicate.test(transformationTarget.getSchemaKey());
				if (!valid) {
					log.error("The transformationSchema schema doesn't seem to be valid for schemaKey {}. Please check the schema bindings provided",
							transformationTarget.getSchemaKey());
					throw new IllegalStateException("Invalid transformation schema");
				}
				final var paths = new HashMap<String, JsonPath>();
				transformationTarget.getTransformers()
						.forEach(transformer -> {
							if (!transformer.getTransformationPath().startsWith(LITERAL)) {
								paths.put(transformer.getAttributeName(),
										JsonPath.compile(transformer.getTransformationPath()));
							}
						});
				compiledPaths.put(transformationTarget.getSchemaKey(), paths);
			});
		});
		return compiledPaths;
	}

	public static boolean text(String transformationPath) {
		return transformationPath.startsWith(LITERAL);
	}

	public static JsonNode toTextNode(String attributeValue) {
		return new TextNode(attributeValue.substring(attributeValue.lastIndexOf(LITERAL) + 1));
	}

	public static JsonNode transformMessage(DocumentContext sourceContext,
	                                        TransformationTarget transformationTarget,
	                                        Map<String, JsonPath> compiledPaths,
	                                        ObjectMapper mapper) {
		final var responseObject = JsonNodeFactory.instance.objectNode();
		transformationTarget.getTransformers().forEach(transformer -> {
			if (text(transformer.getTransformationPath())) {
				responseObject.set(transformer.getAttributeName(), toTextNode(transformer.getTransformationPath()));
			} else {
				final var jsonPath = compiledPaths.get(transformer.getAttributeName());
				if (null != jsonPath) {
					responseObject.set(transformer.getAttributeName(), jsonPathValue(sourceContext, transformer, jsonPath, mapper));
				}
			}
		});
		return responseObject;
	}

	@SneakyThrows
	private static JsonNode jsonPathValue(DocumentContext sourceContext,
	                                      AttributeTransformer transformer,
	                                      JsonPath jsonPath,
	                                      ObjectMapper mapper) {
		final JsonNode value = sourceContext.read(jsonPath);
		return transformer.isSerialize() ? new TextNode(mapper.writeValueAsString(value)) : value;
	}
}
