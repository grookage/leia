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

package com.grookage.leia.common.builder;

import com.grookage.leia.common.utils.FieldUtils;
import com.grookage.leia.common.utils.QualifierUtils;
import com.grookage.leia.common.utils.SchemaConstants;
import com.grookage.leia.models.annotations.SchemaDefinition;
import com.grookage.leia.models.attributes.*;
import com.grookage.leia.models.qualifiers.QualifierInfo;
import com.grookage.leia.models.schema.ingestion.CreateSchemaRequest;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class SchemaBuilder {

    private static final String ELEMENT = "element";

    public Optional<CreateSchemaRequest> buildSchemaRequest(final Class<?> klass) {
        if (Objects.isNull(klass) || !klass.isAnnotationPresent(SchemaDefinition.class)) {
            return Optional.empty();
        }
        final var schemaDefinition = klass.getAnnotation(SchemaDefinition.class);
        return Optional.of(CreateSchemaRequest.builder()
                .schemaName(schemaDefinition.name())
                .namespace(schemaDefinition.namespace())
                .description(schemaDefinition.description())
                .schemaType(schemaDefinition.type())
                .validationType(schemaDefinition.validation())
                .attributes(getSchemaAttributes(klass))
                .build()
        );
    }

    public Set<SchemaAttribute> getSchemaAttributes(final Class<?> klass) {
        return FieldUtils.getAllFields(klass)
                .stream()
                .map(SchemaBuilder::schemaAttribute)
                .collect(Collectors.toSet());
    }

    private SchemaAttribute schemaAttribute(final Field field) {
        return schemaAttribute(
                field.getGenericType(),
                field.getName(),
                QualifierUtils.getQualifiers(field),
                isOptional(field)
        );
    }

    private SchemaAttribute schemaAttribute(final Type type,
                                            final String name,
                                            final Set<QualifierInfo> qualifiers,
                                            final boolean optional) {
        // Handle TypeVariable (Generic type parameters like T, U, etc.)
        if (type instanceof TypeVariable<?>) {
            return new TypeAttribute(name, optional, qualifiers);
        }

        // Handle Class instances (eg. String, Enum classes, Complex POJO Objects etc.)
        if (type instanceof Class<?> klass) {
            return schemaAttribute(klass, name, qualifiers, optional);
        }

        // Handle ParameterizedType (e.g., List<String>, Map<String, Integer>)
        if (type instanceof ParameterizedType parameterizedType) {
            return schemaAttribute(parameterizedType, name, qualifiers, optional);
        }

        // Handle GenericArrayType (e.g., T[], List<T[]>)
        if (type instanceof GenericArrayType genericArrayType) {
            return schemaAttribute(genericArrayType, name, qualifiers, optional);
        }

        throw new UnsupportedOperationException("Unsupported field type: " + type.getTypeName());
    }

    private SchemaAttribute schemaAttribute(final ParameterizedType parameterizedType,
                                            final String name,
                                            final Set<QualifierInfo> qualifiers,
                                            final boolean optional) {
        final var rawType = (Class<?>) parameterizedType.getRawType();
        // Handle List<T> or Set<T>
        if (ClassUtils.isAssignable(rawType, Collection.class)) {
            return handleCollection(parameterizedType, name, qualifiers, optional);
        }

        // Handle Map<T,R>
        if (ClassUtils.isAssignable(rawType, Map.class)) {
            return handleMap(parameterizedType, name, qualifiers, optional);
        }

        // Handle Class<T1,T2,T3> etc.
        final var rawTypeAttribute = schemaAttribute(rawType, name, qualifiers, optional);
        // Convert type parameters
        final var typedAttributes = Arrays.stream(parameterizedType.getActualTypeArguments())
                .map(type -> schemaAttribute(type, "type", QualifierUtils.getQualifiers(type), isOptional(type)))
                .toList();

        return new ParameterizedObjectAttribute(name, optional, qualifiers, rawTypeAttribute, typedAttributes);
    }

    private SchemaAttribute handleMap(final ParameterizedType parameterizedType,
                                      final String name,
                                      final Set<QualifierInfo> qualifiers,
                                      final boolean optional) {
        final var keyType = parameterizedType.getActualTypeArguments()[0];
        final var valueType = parameterizedType.getActualTypeArguments()[1];
        return new MapAttribute(
                name,
                optional,
                qualifiers,
                schemaAttribute(keyType, "key", QualifierUtils.getQualifiers(keyType), isOptional(keyType)),
                schemaAttribute(valueType, "value", QualifierUtils.getQualifiers(valueType), isOptional(valueType))
        );
    }

    private SchemaAttribute handleCollection(final ParameterizedType parameterizedType,
                                             final String name,
                                             final Set<QualifierInfo> qualifiers, boolean optional) {
        final var elementType = parameterizedType.getActualTypeArguments()[0];
        return new ArrayAttribute(
                name,
                optional,
                qualifiers,
                schemaAttribute(elementType, ELEMENT, QualifierUtils.getQualifiers(elementType),
                        isOptional(elementType))
        );
    }

    private SchemaAttribute schemaAttribute(final GenericArrayType genericArrayType,
                                            final String name,
                                            final Set<QualifierInfo> qualifiers,
                                            final boolean optional) {
        final var componentType = genericArrayType.getGenericComponentType();
        return new ArrayAttribute(
                name,
                optional,
                qualifiers,
                schemaAttribute(componentType, ELEMENT, QualifierUtils.getQualifiers(componentType),
                        isOptional(componentType))
        );
    }


    private SchemaAttribute schemaAttribute(final Class<?> klass,
                                            final String name,
                                            final Set<QualifierInfo> qualifiers,
                                            final boolean optional) {
        if (klass == String.class) {
            return new StringAttribute(name, optional, qualifiers);
        }

        if (klass.isEnum()) {
            return new EnumAttribute(name, optional, qualifiers, getEnumValues(klass));
        }

        // Handle int, Integer, long, Long, boolean  etc.
        if (klass.isPrimitive() || SchemaConstants.BOXED_PRIMITIVES.contains(klass)) {
            return handlePrimitive(klass, name, qualifiers, optional);
        }

        // Handle String[], Object[] etc.
        if (klass.isArray()) {
            final var componentType = klass.getComponentType();
            return new ArrayAttribute(
                    name,
                    optional,
                    qualifiers,
                    schemaAttribute(componentType, ELEMENT, QualifierUtils.getQualifiers(componentType),
                            isOptional(componentType))
            );
        }

        // Handle Raw List, Set
        if (ClassUtils.isAssignable(klass, Collection.class)) {
            return new ArrayAttribute(name, optional, qualifiers, null);
        }

        // Handle Raw Map
        if (ClassUtils.isAssignable(klass, Map.class)) {
            return new MapAttribute(name, optional, qualifiers, null, null);
        }

        if (klass.equals(Object.class)) {
            return new ObjectAttribute(name, optional, qualifiers, null);
        }

        if (SchemaConstants.SUPPORTED_DATE_CLASSES.contains(klass)) {
            return new DateAttribute(name, optional, qualifiers);
        }

        // Handling custom defined POJO's
        final var schemaAttributes = getSchemaAttributes(klass);
        return new ObjectAttribute(name, optional, qualifiers, schemaAttributes);
    }

    private SchemaAttribute handlePrimitive(final Class<?> klass,
                                            final String name,
                                            final Set<QualifierInfo> qualifiers,
                                            final boolean optional) {
        if (klass == Integer.class || klass == int.class) {
            return new IntegerAttribute(name, optional, qualifiers);
        }
        if (klass == Boolean.class || klass == boolean.class) {
            return new BooleanAttribute(name, optional, qualifiers);
        }
        if (klass == Double.class || klass == double.class) {
            return new DoubleAttribute(name, optional, qualifiers);
        }
        if (klass == Long.class || klass == long.class) {
            return new LongAttribute(name, optional, qualifiers);
        }
        if (klass == Float.class || klass == float.class) {
            return new FloatAttribute(name, optional, qualifiers);
        }
        if (klass == Short.class || klass == short.class) {
            return new ShortAttribute(name, optional, qualifiers);
        }
        if (klass == Character.class || klass == char.class) {
            return new CharacterAttribute(name, optional, qualifiers);
        }
        if (klass == Byte.class || klass == byte.class) {
            return new ByteAttribute(name, optional, qualifiers);
        }

        throw new UnsupportedOperationException("Unsupported primitive class type: " + klass.getName());

    }

    private Set<String> getEnumValues(final Class<?> klass) {
        return Arrays.stream(klass.getEnumConstants())
                .map(enumConstant -> ((Enum<?>) enumConstant).name())
                .collect(Collectors.toSet());
    }

    private boolean isOptional(final Type type) {
        if (type instanceof Class<?> klass) {
            return isOptional(klass);
        }
        return false;
    }

    private boolean isOptional(final Class<?> klass) {
        return klass.isAnnotationPresent(com.grookage.leia.models.annotations.attribute.Optional.class);
    }

    private boolean isOptional(final Field field) {
        return field.isAnnotationPresent(com.grookage.leia.models.annotations.attribute.Optional.class);
    }
}
