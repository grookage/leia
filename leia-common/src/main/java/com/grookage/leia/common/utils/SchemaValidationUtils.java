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
    static Function<Class<?>, Function<SchemaAttribute, Boolean>> assignableCheckFunction =
            klass -> attribute -> ClassUtils.isAssignable(klass, attribute.getType().getAssignableClass());

    private static final String TYPE_VIOLATION = "Incompatible Type, expected: %s, provided: %s";

    public List<LeiaSchemaViolation> valid(final SchemaDetails schemaDetails,
                                           final Class<?> klass) {
        return valid(schemaDetails.getValidationType(), schemaDetails.getAttributes(), klass, new ViolationContext());
    }

    public List<LeiaSchemaViolation> valid(final SchemaValidationType validationType,
                                           final Set<SchemaAttribute> attributes,
                                           final Class<?> klass,
                                           final ViolationContext context) {
        context.pushClass(klass);
        final var fields = FieldUtils.getAllFields(klass);
        validSchema(validationType, attributes, fields, klass, context);
        attributes.forEach(each -> validAttribute(each, fields, validationType, context));
        context.popClass();
        return context.getViolations();
    }

    private void validSchema(final SchemaValidationType validationType,
                             final Set<SchemaAttribute> attributes,
                             final List<Field> fields,
                             final Class<?> klass,
                             final ViolationContext context) {
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
                    context.addViolation(String.format("[STRICT] Validation: attributes not found or extra attributes :%s", mismatchedAttributes));
                }
                return null;
            }

            @Override
            public Void matching() {
                final var attributesMissing = Sets.difference(attributesListed, fieldNames);
                if (!attributesMissing.isEmpty()) {
                    context.addViolation(String.format("[MATCHING] Validation: Missing attributes found :%s", attributesMissing));
                }
                return null;
            }
        });
    }

    private void validAttribute(final SchemaAttribute attribute,
                                final List<Field> fields,
                                final SchemaValidationType validationType,
                                final ViolationContext context) {
        final var field = fields.stream()
                .filter(each -> each.getName().equals(attribute.getName()))
                .findFirst().orElse(null);
        if (field == null) {
            context.addViolation("Missing Field", attribute.getName());
            return;
        }
        valid(validationType, attribute, field.getGenericType(), context);
    }

    private void valid(final SchemaValidationType validationType,
                       final SchemaAttribute attribute,
                       final Type type,
                       final ViolationContext context) {
        if (type instanceof Class<?> klass) {
            valid(validationType, attribute, klass, context);
        } else if (type instanceof ParameterizedType parameterizedType) {
            valid(validationType, attribute, parameterizedType, context);
        } else if (type instanceof GenericArrayType arrayType) {
            valid(validationType, attribute, arrayType, context);
        } else {
            context.addViolation("Unsupported class type: " + type);
        }
    }

    private void valid(final SchemaValidationType validationType,
                       final SchemaAttribute schemaAttribute,
                       final Class<?> klass,
                       final ViolationContext context) {
        if (!isMatchingType(klass, schemaAttribute)) {
            context.addViolation(String.format(TYPE_VIOLATION, schemaAttribute.getType(), klass.getSimpleName()),
                    schemaAttribute.getName());
            return;
        }

        schemaAttribute.accept(new SchemaAttributeHandler<Void>(a -> null) {
            @Override
            public Void accept(ArrayAttribute attribute) {
                if (klass.isArray()) {
                    valid(validationType, attribute.getElementAttribute(), klass.getComponentType(), context);
                    return null;
                } else if (!Objects.isNull(attribute.getElementAttribute())) {
                    context.addViolation(String.format("Missing Type arguments, expected ParameterizedType:%s",
                            attribute.getElementAttribute().getType()), attribute.getName());
                }
                return null;
            }

            @Override
            public Void accept(MapAttribute attribute) {
                if (!Objects.isNull(attribute.getKeyAttribute()) || !Objects.isNull(attribute.getValueAttribute())) {
                    context.addViolation(String.format("Missing Type Arguments, expected parameterized Types key:%s value:%s",
                            attribute.getKeyAttribute().getType(), attribute.getValueAttribute().getType()), attribute.getName());
                }
                return null;
            }

            @Override
            public Void accept(ObjectAttribute attribute) {
                valid(validationType, attribute.getNestedAttributes(), klass, context);
                return null;
            }
        });
    }

    private void valid(final SchemaValidationType validationType,
                       final SchemaAttribute attribute,
                       final ParameterizedType parameterizedType,
                       final ViolationContext context) {
        if (attribute instanceof ArrayAttribute arrayAttribute) {
            if (arrayAttribute.getElementAttribute() == null) {
                return;
            }
            final var typeArguments = getTypeArguments(parameterizedType);
            valid(validationType, arrayAttribute.getElementAttribute(), typeArguments[0], context);
        } else if (attribute instanceof MapAttribute mapAttribute) {
            if (Objects.isNull(mapAttribute.getKeyAttribute()) || Objects.isNull(mapAttribute.getValueAttribute())) {
                return;
            }
            final var typeArguments = getTypeArguments(parameterizedType);
            valid(validationType, mapAttribute.getKeyAttribute(), typeArguments[0], context);
            valid(validationType, mapAttribute.getValueAttribute(), typeArguments[1], context);
        } else {
            context.addViolation(String.format(TYPE_VIOLATION, attribute.getType(), parameterizedType), attribute.getName());
        }
    }

    private void valid(final SchemaValidationType validationType,
                       final SchemaAttribute attribute,
                       final GenericArrayType arrayType,
                       final ViolationContext context) {
        if (attribute instanceof ArrayAttribute arrayAttribute) {
            valid(validationType, arrayAttribute.getElementAttribute(), arrayType.getGenericComponentType(), context);
            return;
        }
        context.addViolation(String.format(TYPE_VIOLATION, attribute.getType(), arrayType), attribute.getName());
    }

    private boolean isMatchingType(final Class<?> klass,
                                   final SchemaAttribute attribute) {
        return attribute.accept(new SchemaAttributeHandler<>(assignableCheckFunction.apply(klass)) {
            @Override
            public Boolean accept(ArrayAttribute attribute) {
                return klass.isArray() || ClassUtils.isAssignable(klass, Collection.class);
            }

            @Override
            public Boolean accept(ObjectAttribute attribute) {
                if (klass.equals(Object.class) && !Objects.isNull(attribute.getNestedAttributes())) {
                    return false;
                }
                return true;
            }
        });
    }

    private Type[] getTypeArguments(final ParameterizedType parameterizedType) {
        final var typeArguments = parameterizedType.getActualTypeArguments();
        if (typeArguments.length == 0) {
            throw new IllegalArgumentException("No type arguments found for " + parameterizedType);
        }
        return typeArguments;
    }

    public boolean valid(final Class<?> klass,
                         final SchemaAttribute schemaAttribute) {
        return schemaAttribute.accept(new SchemaAttributeHandler<>(assignableCheckFunction.apply(klass)) {
        });
    }
}
