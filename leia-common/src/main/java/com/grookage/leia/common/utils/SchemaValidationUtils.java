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

package com.grookage.leia.common.utils;

import com.google.common.collect.Sets;
import com.grookage.leia.common.context.TypeVariableContext;
import com.grookage.leia.common.violation.LeiaSchemaViolation;
import com.grookage.leia.common.violation.ViolationContext;
import com.grookage.leia.models.attributes.ArrayAttribute;
import com.grookage.leia.models.attributes.MapAttribute;
import com.grookage.leia.models.attributes.ObjectAttribute;
import com.grookage.leia.models.attributes.SchemaAttribute;
import com.grookage.leia.models.attributes.SchemaAttributeHandler;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaValidationType;
import com.grookage.leia.models.schema.SchemaValidationVisitor;
import com.grookage.leia.models.utils.CollectionUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
@Slf4j
public class SchemaValidationUtils {
    private static final String TYPE_MISMATCH_MESSAGE = "Type mismatch, expected: %s, provided: %s";
    static Function<Class<?>, Function<SchemaAttribute, Boolean>> assignableCheckFunction =
            klass -> attribute -> ClassUtils.isAssignable(klass, attribute.getType().getAssignableClass());

    public List<LeiaSchemaViolation> valid(final SchemaDetails schemaDetails,
                                           final Class<?> klass) {
        return valid(schemaDetails.getValidationType(), schemaDetails.getAttributes(), klass,
                new ViolationContext(), new TypeVariableContext());
    }

    public List<LeiaSchemaViolation> valid(final SchemaValidationType validationType,
                                           final Set<SchemaAttribute> attributes,
                                           final Class<?> klass) {
        return valid(validationType, attributes, klass, new ViolationContext(), new TypeVariableContext());
    }

    public List<LeiaSchemaViolation> valid(final SchemaValidationType validationType,
                                           final Set<SchemaAttribute> attributes,
                                           final Class<?> klass,
                                           final ViolationContext violationContext,
                                           final TypeVariableContext typeVariableContext) {
        violationContext.pushClass(klass);
        final var fields = FieldUtils.getAllFields(klass);
        validateSchemaStructure(validationType, attributes, fields, violationContext);
        attributes.forEach(each -> {
            final var field = FieldUtils.filter(each.getName(), fields);
            if (field.isEmpty()) {
                violationContext.addViolation("Missing Field", each.getName());
                return;
            }
            validateField(each, field.get(), validationType, violationContext, typeVariableContext);
        });
        violationContext.popClass();
        return violationContext.getViolations();
    }

    private void validateField(final SchemaAttribute attribute,
                               final Field field,
                               final SchemaValidationType validationType,
                               final ViolationContext violationContext,
                               final TypeVariableContext typeVariableContext) {
        final var type = typeVariableContext.resolveType(field.getGenericType());
        validateType(validationType, attribute, type, violationContext,
                typeVariableContext);
    }

    private void validateType(final SchemaValidationType validationType,
                              final SchemaAttribute attribute,
                              final Type type,
                              final ViolationContext violationContext,
                              final TypeVariableContext typeVariableContext) {
        if (type instanceof Class<?> klass) {
            validateClass(validationType, attribute, klass, violationContext);
        } else if (type instanceof ParameterizedType parameterizedType) {
            validateParameterizedType(validationType, attribute, parameterizedType, violationContext, typeVariableContext);
        } else if (type instanceof GenericArrayType arrayType) {
            validateGenericArrayType(validationType, attribute, arrayType, violationContext, typeVariableContext);
        } else {
            violationContext.addViolation("Unsupported class type: " + type);
        }
    }

    private void validateClass(final SchemaValidationType validationType,
                               final SchemaAttribute schemaAttribute,
                               final Class<?> klass,
                               final ViolationContext violationContext) {
        if (!isMatchingType(klass, schemaAttribute)) {
            violationContext.addViolation(String.format(TYPE_MISMATCH_MESSAGE, schemaAttribute.getType(), klass.getSimpleName()),
                    schemaAttribute.getName());
            return;
        }

        schemaAttribute.accept(new SchemaAttributeHandler<Void>(a -> null) {
            @Override
            public Void accept(ArrayAttribute attribute) {
                if (Objects.nonNull(attribute.getElementAttribute())) {
                    if (klass.isArray()) {
                        validateClass(validationType, attribute.getElementAttribute(), klass.getComponentType(), violationContext);
                        return null;
                    }
                    // Provided List, Set expected List<?>, Set<?>
                    violationContext.addViolation(String.format("Missing Type arguments, expected ParameterizedType:%s",
                            attribute.getElementAttribute().getType()), attribute.getName());
                }
                return null;
            }

            @Override
            public Void accept(MapAttribute attribute) {
                if (Objects.nonNull(attribute.getKeyAttribute()) || Objects.nonNull(attribute.getValueAttribute())) {
                    // Provided Map, expected Map<?,?>
                    violationContext.addViolation(String.format("Missing Type Arguments, expected parameterized Types key:%s value:%s",
                            attribute.getKeyAttribute().getType(), attribute.getValueAttribute().getType()), attribute.getName());
                }
                return null;
            }

            @Override
            public Void accept(ObjectAttribute attribute) {
                if (Objects.nonNull(attribute.getNestedAttributes())) {
                    valid(validationType, attribute.getNestedAttributes(), klass, violationContext, new TypeVariableContext());
                }
                return null;
            }
        });
    }

    private void validateParameterizedType(final SchemaValidationType validationType,
                                           final SchemaAttribute attribute,
                                           final ParameterizedType parameterizedType,
                                           final ViolationContext violationContext,
                                           final TypeVariableContext typeVariableContext) {
        final var rawType = (Class<?>) parameterizedType.getRawType();
        if (attribute instanceof ArrayAttribute arrayAttribute) {
            if (arrayAttribute.getElementAttribute() == null) {
                return;
            }
            final var elementType = typeVariableContext.resolveType(parameterizedType.getActualTypeArguments()[0]);
            validateType(validationType, arrayAttribute.getElementAttribute(), elementType, violationContext, typeVariableContext);
        } else if (attribute instanceof MapAttribute mapAttribute) {
            if (Objects.isNull(mapAttribute.getKeyAttribute()) || Objects.isNull(mapAttribute.getValueAttribute())) {
                return;
            }
            final var keyType = typeVariableContext.resolveType(parameterizedType.getActualTypeArguments()[0]);
            final var valueType = typeVariableContext.resolveType(parameterizedType.getActualTypeArguments()[1]);
            validateType(validationType, mapAttribute.getKeyAttribute(), keyType, violationContext, typeVariableContext);
            validateType(validationType, mapAttribute.getValueAttribute(), valueType, violationContext, typeVariableContext);
        } else if (attribute instanceof ObjectAttribute objectAttribute) {
            if (CollectionUtils.isNullOrEmpty(objectAttribute.getNestedAttributes())) {
                return;
            }
            final var newContext = TypeVariableContext.from(rawType, parameterizedType, typeVariableContext);
            valid(validationType, objectAttribute.getNestedAttributes(), rawType, violationContext, newContext);
        } else {
            violationContext.addViolation(String.format(TYPE_MISMATCH_MESSAGE, attribute.getType(), parameterizedType), attribute.getName());
        }
    }

    private void validateGenericArrayType(final SchemaValidationType validationType,
                                          final SchemaAttribute attribute,
                                          final GenericArrayType arrayType,
                                          final ViolationContext violationContext,
                                          final TypeVariableContext typeVariableContext) {
        if (attribute instanceof ArrayAttribute arrayAttribute) {
            final var elementType = typeVariableContext.resolveType(arrayType.getGenericComponentType());
            validateType(validationType, arrayAttribute.getElementAttribute(), elementType, violationContext, typeVariableContext);
            return;
        }
        violationContext.addViolation(String.format(TYPE_MISMATCH_MESSAGE, attribute.getType(), arrayType), attribute.getName());
    }

    private boolean isMatchingType(final Class<?> klass,
                                   final SchemaAttribute attribute) {
        return attribute.accept(new SchemaAttributeHandler<>(assignableCheckFunction.apply(klass)) {
            @Override
            public Boolean accept(ArrayAttribute attribute) {
                return klass.isArray() || org.apache.commons.lang3.ClassUtils.isAssignable(klass, Collection.class);
            }

            @Override
            public Boolean accept(ObjectAttribute attribute) {
                return !klass.equals(Object.class) || !Objects.nonNull(attribute.getNestedAttributes());
            }
        });
    }

    private void validateSchemaStructure(final SchemaValidationType validationType,
                                         final Set<SchemaAttribute> attributes,
                                         final List<Field> fields,
                                         final ViolationContext violationContext) {
        final var fieldNames = fields.stream()
                .map(Field::getName)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        final var attributesListed = attributes.stream()
                .map(SchemaAttribute::getName)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        validationType.accept(new SchemaValidationVisitor<>() {
            @Override
            public Void strict() {
                final var mismatchedAttributes = Sets.symmetricDifference(fieldNames, attributesListed);
                if (!mismatchedAttributes.isEmpty()) {
                    violationContext.addViolation(String.format("[STRICT] Validation: attributes not found or extra attributes :%s", mismatchedAttributes));
                }
                return null;
            }

            @Override
            public Void matching() {
                final var attributesMissing = Sets.difference(attributesListed, fieldNames);
                if (!attributesMissing.isEmpty()) {
                    violationContext.addViolation(String.format("[MATCHING] Validation: Missing attributes found :%s", attributesMissing));
                }
                return null;
            }
        });
    }

    public boolean valid(final Class<?> klass,
                         final SchemaAttribute schemaAttribute) {
        return schemaAttribute.accept(new SchemaAttributeHandler<>(assignableCheckFunction.apply(klass)) {
        });
    }

}
