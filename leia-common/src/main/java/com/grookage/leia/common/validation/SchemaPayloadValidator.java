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

package com.grookage.leia.common.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.grookage.leia.models.attributes.ArrayAttribute;
import com.grookage.leia.models.attributes.BooleanAttribute;
import com.grookage.leia.models.attributes.ByteAttribute;
import com.grookage.leia.models.attributes.CharacterAttribute;
import com.grookage.leia.models.attributes.DateAttribute;
import com.grookage.leia.models.attributes.DoubleAttribute;
import com.grookage.leia.models.attributes.EnumAttribute;
import com.grookage.leia.models.attributes.FloatAttribute;
import com.grookage.leia.models.attributes.IntegerAttribute;
import com.grookage.leia.models.attributes.LongAttribute;
import com.grookage.leia.models.attributes.MapAttribute;
import com.grookage.leia.models.attributes.ObjectAttribute;
import com.grookage.leia.models.attributes.ParameterizedObjectAttribute;
import com.grookage.leia.models.attributes.SchemaAttribute;
import com.grookage.leia.models.attributes.SchemaAttributeAcceptor;
import com.grookage.leia.models.attributes.ShortAttribute;
import com.grookage.leia.models.attributes.StringAttribute;
import com.grookage.leia.models.attributes.TypeAttribute;
import com.grookage.leia.models.schema.SchemaValidationType;
import com.grookage.leia.models.utils.MapperUtils;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class SchemaPayloadValidator {
    public static List<String> validate(final JsonNode jsonNode,
                                        final SchemaValidationType validationType,
                                        final Set<SchemaAttribute> schemaAttributes) {
        final List<String> validationErrors = new ArrayList<>();
        final var schemaMap = schemaAttributes.stream()
                .collect(Collectors.toMap(SchemaAttribute::getName, attribute -> attribute, (a, b) -> b));

        // Validate extra fields in case of Strict Validation
        if (validationType == SchemaValidationType.STRICT) {
            jsonNode.fieldNames().forEachRemaining(fieldName -> {
                if (!schemaMap.containsKey(fieldName)) {
                    validationErrors.add("Unexpected field: " + fieldName);
                }
            });
        }

        // Validate missing and type mismatched fields
        // Check the attribute only if the jsonNode is an object
        schemaAttributes.forEach(attribute -> {
            final var fieldName = attribute.getName();
            if (jsonNode.isObject() && !jsonNode.has(fieldName)) {
                if (!attribute.isOptional()) {
                    validationErrors.add("Missing required field: " + fieldName);
                }
                return;
            }
            if (jsonNode.isValueNode()) {
                validateField(jsonNode, attribute, validationType, validationErrors);
                return;
            }
            final var fieldNode = jsonNode.get(fieldName);
            validateField(fieldNode, attribute, validationType, validationErrors);
        });

        return validationErrors;
    }

    private void validateField(final JsonNode fieldNode,
                               final SchemaAttribute attribute,
                               final SchemaValidationType validationType,
                               final List<String> validationErrors) {
        final var fieldName = attribute.getName();
        if (!isMatchingType(fieldNode, attribute)) {
            validationErrors.add("Type mismatch for field: " + fieldName +
                                 ". Expected: " + attribute.getType() +
                                 ", Found: " + fieldNode.getNodeType());
            return;
        }

        // Recursively validate nested objects
        if (attribute instanceof ObjectAttribute objectAttribute) {
            if (objectAttribute.getNestedAttributes() != null) {
                validationErrors.addAll(validate(fieldNode, validationType, objectAttribute.getNestedAttributes()));
            }
        } else if (attribute instanceof ArrayAttribute arrayAttribute) {
            validateCollectionAttribute(fieldNode, arrayAttribute, validationType, validationErrors);
        } else if (attribute instanceof MapAttribute mapAttribute) {
            validateMapAttribute(fieldNode, mapAttribute, validationType, validationErrors);
        }
    }

    private void validateCollectionAttribute(final JsonNode fieldNode,
                                             final ArrayAttribute arrayAttribute,
                                             final SchemaValidationType schemaValidationType,
                                             final List<String> validationErrors) {
        // Handling Non-Parameterized Collections (eg: List.class, Set.class, Map.class etc.)
        if (arrayAttribute.getElementAttribute() == null) {
            return;
        }

        for (JsonNode arrayElement : fieldNode) {
            validateField(arrayElement, arrayAttribute.getElementAttribute(), schemaValidationType, validationErrors);
        }
    }

    private void validateMapAttribute(final JsonNode fieldNode,
                                      final MapAttribute mapAttribute,
                                      final SchemaValidationType schemaValidationType,
                                      final List<String> validationErrors) {
        // Handling Raw Map.class
        if (Objects.isNull(mapAttribute.getKeyAttribute()) && Objects.isNull(mapAttribute.getValueAttribute())) {
            return;
        }
        fieldNode.fields().forEachRemaining(entry -> {
            final var keyNode = entry.getKey() != null
                    ? MapperUtils.mapper().convertValue(entry.getKey(), JsonNode.class)
                    : null;
            if (Objects.nonNull(keyNode)) {
                // validate Key
                validateField(keyNode, mapAttribute.getKeyAttribute(), schemaValidationType, validationErrors);
                // Validate value
                validateField(entry.getValue(), mapAttribute.getValueAttribute(), schemaValidationType, validationErrors);
            } else {
                validationErrors.add("Key Not present for map attribute field:" +
                                     mapAttribute.getName());
            }
        });

    }

    private boolean isMatchingType(final JsonNode fieldNode,
                                   final SchemaAttribute attribute) {
        return attribute.accept(new SchemaAttributeAcceptor<>() {
            @Override
            public Boolean accept(BooleanAttribute attribute) {
                return fieldNode.isBoolean();
            }

            @Override
            public Boolean accept(ByteAttribute attribute) {
                return fieldNode.isArray();
            }

            @Override
            public Boolean accept(CharacterAttribute attribute) {
                // A CharacterAttribute must be a single character, not a full string
                return fieldNode.isTextual() && fieldNode.asText().length() == 1;
            }

            @Override
            public Boolean accept(DoubleAttribute attribute) {
                return fieldNode.isDouble() || fieldNode.isFloat() || fieldNode.isInt();
            }

            @Override
            public Boolean accept(EnumAttribute attribute) {
                return fieldNode.isTextual() && attribute.getValues().contains(fieldNode.asText());
            }

            @Override
            public Boolean accept(FloatAttribute attribute) {
                return fieldNode.isFloat();
            }

            @Override
            public Boolean accept(IntegerAttribute attribute) {
                return fieldNode.isInt();
            }

            @Override
            public Boolean accept(LongAttribute attribute) {
                return fieldNode.isLong() || fieldNode.isInt();
            }

            @Override
            public Boolean accept(ShortAttribute attribute) {
                return fieldNode.isShort() || fieldNode.isInt();
            }

            @Override
            public Boolean accept(StringAttribute attribute) {
                return fieldNode.isTextual();
            }

            @Override
            public Boolean accept(DateAttribute attribute) {
                return fieldNode.isTextual() || fieldNode.isLong() || fieldNode.isInt();
            }

            @Override
            public Boolean accept(ArrayAttribute attribute) {
                return fieldNode.isArray();
            }

            @Override
            public Boolean accept(MapAttribute attribute) {
                return fieldNode.isObject();
            }

            @Override
            public Boolean accept(ObjectAttribute attribute) {
                // Handling Object.class
                if (attribute.getNestedAttributes() == null) {
                    return true;
                }
                return fieldNode.isObject();
            }

            @Override
            public Boolean accept(ParameterizedObjectAttribute attribute) {
                return fieldNode.isObject();
            }

            @Override
            public Boolean accept(TypeAttribute attribute) {
                return true;
            }
        });
    }
}
