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

import com.grookage.leia.common.context.TypeVariableContext;
import com.grookage.leia.common.utils.BuilderUtils;
import com.grookage.leia.common.utils.FieldUtils;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The SchemaBuilder class is responsible for generating schema definitions
 * from Java classes using annotations and reflection.
 * <p>
 * It supports complex types, including:
 * <ul>
 *     <li>Primitive types (int, double, boolean, etc.)</li>
 *     <li>Boxed types (Integer, Double, Boolean, etc.)</li>
 *     <li>Collections (List, Set, etc.)</li>
 *     <li>Generic classes and parameterized types</li>
 *     <li>Custom POJOs</li>
 * </ul>
 * <p>
 * The generated schema is used to create a {@link CreateSchemaRequest}.
 */
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
                .tags(Arrays.asList(schemaDefinition.tags()))
                .build()
        );
    }

    public Set<SchemaAttribute> getSchemaAttributes(final Class<?> klass) {
        return FieldUtils.getAllFields(klass)
                .stream()
                .map(field -> schemaAttribute(field, new TypeVariableContext()))
                .collect(Collectors.toSet());
    }

    private SchemaAttribute schemaAttribute(final Field field,
                                            final TypeVariableContext typeVariableContext) {
        return schemaAttribute(typeVariableContext.resolveType(field.getGenericType()), field.getName(), BuilderUtils.getQualifiers(field),
                BuilderUtils.isOptional(field), typeVariableContext);
    }

    private SchemaAttribute schemaAttribute(final Type type,
                                            final String name,
                                            final Set<QualifierInfo> qualifiers,
                                            final boolean optional,
                                            final TypeVariableContext typeVariableContext) {
        // Handle Class instances (eg. String, Enum classes, Complex POJO Objects etc.)
        if (type instanceof Class<?> klass) {
            return schemaAttribute(klass, name, qualifiers, optional);
        }

        // Handle ParameterizedType (e.g., List<String>, Map<String, Integer>)
        if (type instanceof ParameterizedType parameterizedType) {
            return handleParameterizedType(parameterizedType, name, qualifiers, optional, typeVariableContext);
        }

        // Handle GenericArrayType (e.g., T[], List<T[]>)
        if (type instanceof GenericArrayType genericArrayType) {
            return handleGenericArray(genericArrayType, name, qualifiers, optional, typeVariableContext);
        }

        throw new UnsupportedOperationException("Unsupported field type: " + type.getTypeName());
    }

    private SchemaAttribute handleParameterizedType(final ParameterizedType parameterizedType,
                                                    final String name,
                                                    final Set<QualifierInfo> qualifiers,
                                                    final boolean optional,
                                                    final TypeVariableContext typeVariableContext) {
        final var rawType = (Class<?>) parameterizedType.getRawType();

        // Handle List<T> or Set<T>
        if (org.apache.commons.lang3.ClassUtils.isAssignable(rawType, Collection.class)) {
            return handleCollection(parameterizedType, name, qualifiers, optional, typeVariableContext);
        }

        // Handle Map<T,R>
        if (org.apache.commons.lang3.ClassUtils.isAssignable(rawType, Map.class)) {
            return handleMap(parameterizedType, name, qualifiers, optional, typeVariableContext);
        }

        // handle Class<T1,T2> etc.
        // Extract and convert fields with resolved types
        // Capture generic type variables of the class from the parent context
        final var childContext = TypeVariableContext.from(rawType, parameterizedType, typeVariableContext);
        final var fieldAttributes = FieldUtils.getAllFields(rawType)
                .stream()
                .map(field -> schemaAttribute(field, childContext)) // Resolves TypeVariable<T>
                .collect(Collectors.toSet());

        return new ObjectAttribute(name, optional, qualifiers, fieldAttributes);
    }

    private SchemaAttribute handleMap(final ParameterizedType parameterizedType,
                                      final String name,
                                      final Set<QualifierInfo> qualifiers,
                                      final boolean optional,
                                      final TypeVariableContext typeVariableContext) {
        final var keyType = typeVariableContext.resolveType(parameterizedType.getActualTypeArguments()[0]);
        final var valueType = typeVariableContext.resolveType(parameterizedType.getActualTypeArguments()[1]);
        return new MapAttribute(
                name,
                optional,
                qualifiers,
                schemaAttribute(keyType, "key", BuilderUtils.getQualifiers(keyType), BuilderUtils.isOptional(keyType), typeVariableContext),
                schemaAttribute(valueType, "value", BuilderUtils.getQualifiers(valueType), BuilderUtils.isOptional(valueType), typeVariableContext)
        );
    }

    private SchemaAttribute handleCollection(final ParameterizedType parameterizedType,
                                             final String name,
                                             final Set<QualifierInfo> qualifiers,
                                             final boolean optional,
                                             final TypeVariableContext typeVariableContext) {
        final var elementType = typeVariableContext.resolveType(parameterizedType.getActualTypeArguments()[0]);
        return new ArrayAttribute(
                name,
                optional,
                qualifiers,
                schemaAttribute(elementType, ELEMENT, BuilderUtils.getQualifiers(elementType),
                        BuilderUtils.isOptional(elementType), typeVariableContext)
        );
    }

    private SchemaAttribute handleGenericArray(final GenericArrayType genericArrayType,
                                               final String name,
                                               final Set<QualifierInfo> qualifiers,
                                               final boolean optional,
                                               final TypeVariableContext typeVariableContext) {
        final var componentType = typeVariableContext.resolveType(genericArrayType.getGenericComponentType());
        return new ArrayAttribute(
                name,
                optional,
                qualifiers,
                schemaAttribute(componentType, ELEMENT, BuilderUtils.getQualifiers(componentType), BuilderUtils.isOptional(componentType),
                        typeVariableContext)
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
            return new EnumAttribute(name, optional, qualifiers, BuilderUtils.getEnumValues(klass));
        }

        // Handle int, Integer, long, Long, boolean  etc.
        if (klass.isPrimitive() || SchemaConstants.BOXED_PRIMITIVES.contains(klass)) {
            return BuilderUtils.buildPrimitiveAttribute(klass, name, qualifiers, optional);
        }

        // Handle String[], Object[] etc.
        if (klass.isArray()) {
            final var componentType = klass.getComponentType();
            return new ArrayAttribute(
                    name,
                    optional,
                    qualifiers,
                    schemaAttribute(componentType, ELEMENT, BuilderUtils.getQualifiers(componentType),
                            BuilderUtils.isOptional(componentType))
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
}
