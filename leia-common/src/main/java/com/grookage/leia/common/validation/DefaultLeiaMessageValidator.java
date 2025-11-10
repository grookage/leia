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
import com.grookage.leia.common.violation.LeiaMessageViolation;
import com.grookage.leia.common.violation.LeiaMessageViolationImpl;
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
import com.grookage.leia.models.attributes.SchemaAttribute;
import com.grookage.leia.models.attributes.SchemaAttributeAcceptor;
import com.grookage.leia.models.attributes.ShortAttribute;
import com.grookage.leia.models.attributes.StringAttribute;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaValidationType;
import com.grookage.leia.models.utils.MapperUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultLeiaMessageValidator implements LeiaMessageValidator {

    @Override
    public List<LeiaMessageViolation> validate(final SchemaDetails schemaDetails, final JsonNode message) {
        return validate(message, schemaDetails.getValidationType(),
                schemaDetails.getAttributes());
    }

    public List<LeiaMessageViolation> validate(final JsonNode jsonNode,
                                               final SchemaValidationType validationType,
                                               final Set<SchemaAttribute> schemaAttributes) {
        final var validationErrors = validateInternal(jsonNode, validationType, schemaAttributes, "");
        return validationErrors.stream()
                .map(error -> LeiaMessageViolationImpl.builder()
                        .message(error.getMessage())
                        .fieldPath(error.getFieldPath())
                        .build())
                .collect(Collectors.toList());
    }

    public List<ValidationError> validateInternal(final JsonNode jsonNode,
                                                  final SchemaValidationType validationType,
                                                  final Set<SchemaAttribute> schemaAttributes,
                                                  final String currentPath) {
        final List<ValidationError> validationErrors = new ArrayList<>();
        final var schemaMap = schemaAttributes.stream()
                .collect(Collectors.toMap(SchemaAttribute::getName, attribute -> attribute, (a, b) -> b));

        // Validate extra fields in case of Strict Validation
        if (validationType == SchemaValidationType.STRICT) {
            jsonNode.fieldNames().forEachRemaining(fieldName -> {
                if (!schemaMap.containsKey(fieldName)) {
                    validationErrors.add(new ValidationError(
                            "Unexpected field: " + fieldName,
                            buildPath(currentPath, fieldName)));
                }
            });
        }

        // Validate missing and type mismatched fields
        schemaAttributes.forEach(attribute -> {
            final var fieldName = attribute.getName();
            final var fieldPath = buildPath(currentPath, fieldName);

            if (jsonNode.isObject() && !jsonNode.has(fieldName)) {
                if (!attribute.isOptional()) {
                    validationErrors.add(new ValidationError(
                            "Missing required field: " + fieldName,
                            fieldPath));
                }
                return;
            }

            if (jsonNode.isValueNode()) {
                validationErrors.addAll(validateField(jsonNode, attribute, validationType, currentPath));
                return;
            }

            final var fieldNode = jsonNode.get(fieldName);
            validationErrors.addAll(validateField(fieldNode, attribute, validationType, fieldPath));
        });

        return validationErrors;
    }

    private List<ValidationError> validateField(final JsonNode fieldNode,
                                                final SchemaAttribute attribute,
                                                final SchemaValidationType validationType,
                                                final String fieldPath) {
        final List<ValidationError> validationErrors = new ArrayList<>();
        final var fieldName = attribute.getName();

        if (!isMatchingType(fieldNode, attribute)) {
            validationErrors.add(new ValidationError(
                    "Type mismatch for field: " + fieldName +
                            ". Expected: " + attribute.getType() +
                            ", Found: " + fieldNode.getNodeType(),
                    fieldPath));
            return validationErrors;
        }

        // Recursively validate nested objects
        if (attribute instanceof ObjectAttribute objectAttribute) {
            if (objectAttribute.getNestedAttributes() != null) {
                validationErrors.addAll(validateInternal(fieldNode, validationType,
                        objectAttribute.getNestedAttributes(), fieldPath));
            }
        } else if (attribute instanceof ArrayAttribute arrayAttribute) {
            validationErrors.addAll(validateCollectionAttribute(fieldNode, arrayAttribute,
                    validationType, fieldPath));
        } else if (attribute instanceof MapAttribute mapAttribute) {
            validationErrors.addAll(validateMapAttribute(fieldNode, mapAttribute,
                    validationType, fieldPath));
        }

        return validationErrors;
    }

    private List<ValidationError> validateCollectionAttribute(final JsonNode fieldNode,
                                                              final ArrayAttribute arrayAttribute,
                                                              final SchemaValidationType schemaValidationType,
                                                              final String fieldPath) {
        final List<ValidationError> validationErrors = new ArrayList<>();

        // Handling Non-Parameterized Collections
        if (arrayAttribute.getElementAttribute() == null) {
            return validationErrors;
        }

        int index = 0;
        for (JsonNode arrayElement : fieldNode) {
            final var elementPath = fieldPath + "[" + index + "]";
            validationErrors.addAll(validateField(arrayElement, arrayAttribute.getElementAttribute(),
                    schemaValidationType, elementPath));
            index++;
        }

        return validationErrors;
    }

    private List<ValidationError> validateMapAttribute(final JsonNode fieldNode,
                                                       final MapAttribute mapAttribute,
                                                       final SchemaValidationType schemaValidationType,
                                                       final String fieldPath) {
        final List<ValidationError> validationErrors = new ArrayList<>();

        // Handling Raw Map.class
        if (Objects.isNull(mapAttribute.getKeyAttribute()) && Objects.isNull(mapAttribute.getValueAttribute())) {
            return validationErrors;
        }

        fieldNode.fields().forEachRemaining(entry -> {
            final var keyNode = entry.getKey() != null
                    ? MapperUtils.mapper().convertValue(entry.getKey(), JsonNode.class)
                    : null;
            if (Objects.nonNull(keyNode)) {
                final var entryPath = fieldPath + "['" + entry.getKey() + "']";
                // validate Key
                validationErrors.addAll(validateField(keyNode, mapAttribute.getKeyAttribute(),
                        schemaValidationType, entryPath + ".key"));
                // Validate value
                validationErrors.addAll(validateField(entry.getValue(), mapAttribute.getValueAttribute(),
                        schemaValidationType, entryPath + ".value"));
            } else {
                validationErrors.add(new ValidationError(
                        "Key not present for map attribute field: " + mapAttribute.getName(),
                        fieldPath));
            }
        });

        return validationErrors;
    }

    private String buildPath(String currentPath, String fieldName) {
        if (currentPath == null || currentPath.isEmpty()) {
            return fieldName;
        }
        return currentPath + "." + fieldName;
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
        });
    }

    /**
     * Internal class to track validation errors with field paths
     */
    private static class ValidationError {
        private final String message;
        private final String fieldPath;

        public ValidationError(String message, String fieldPath) {
            this.message = message;
            this.fieldPath = fieldPath;
        }

        public String getMessage() {
            return message;
        }

        public String getFieldPath() {
            return fieldPath;
        }
    }
}
