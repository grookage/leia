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
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.ingestion.CreateSchemaRequest;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.*;
import java.util.*;
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
        final var schemaKey = SchemaKey.builder()
                .namespace(schemaDefinition.namespace())
                .schemaName(schemaDefinition.name())
                .orgId(schemaDefinition.orgId())
                .tenantId(schemaDefinition.tenantId())
                .version(schemaDefinition.version())
                .type(schemaDefinition.type())
                .build();
        return Optional.of(CreateSchemaRequest.builder()
                .schemaKey(schemaKey)
                .description(schemaDefinition.description())
                .schemaType(schemaDefinition.schemaType())
                .validationType(schemaDefinition.validation())
                .attributes(getSchemaAttributes(klass))
                .tags(Set.of(schemaDefinition.tags()))
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

        return schemaAttribute(typeVariableContext.resolveType(field.getAnnotatedType()), field.getName(), BuilderUtils.isOptional(field),
                BuilderUtils.getQualifiers(field), typeVariableContext);
    }

    private SchemaAttribute schemaAttribute(final AnnotatedType annotatedType,
                                            final String name,
                                            final boolean optional,
                                            final Set<QualifierInfo> qualifiers,
                                            final TypeVariableContext typeVariableContext) {
        final var type = annotatedType.getType();
        // Handle Class instances (eg. String, Enum classes, Complex POJO Objects etc.)
        if (type instanceof Class<?> klass) {
            return schemaAttribute(klass, name, qualifiers, optional);
        }

        // Handle ParameterizedType (e.g., List<String>, Map<String, Integer>)
        if (type instanceof ParameterizedType) {
            return handleParameterizedType((AnnotatedParameterizedType) annotatedType, name, qualifiers, optional,
                    typeVariableContext);
        }

        // Handle GenericArrayType (e.g., T[], List<T[]>)
        if (type instanceof GenericArrayType) {
            return handleGenericArray((AnnotatedArrayType) annotatedType, name, qualifiers, optional,
                    typeVariableContext);
        }

        throw new UnsupportedOperationException("Unsupported field type: " + type.getTypeName());
    }

    private SchemaAttribute handleParameterizedType(final AnnotatedParameterizedType annotatedParameterizedType,
                                                    final String name,
                                                    final Set<QualifierInfo> qualifiers,
                                                    final boolean optional,
                                                    final TypeVariableContext typeVariableContext) {
        final var parameterizedType = (ParameterizedType) annotatedParameterizedType.getType();
        final var rawType = (Class<?>) parameterizedType.getRawType();

        // Handle List<T> or Set<T>
        if (ClassUtils.isAssignable(rawType, Collection.class)) {
            return handleCollection(annotatedParameterizedType, name, qualifiers, optional, typeVariableContext);
        }

        // Handle Map<T,R>
        if (ClassUtils.isAssignable(rawType, Map.class)) {
            return handleMap(annotatedParameterizedType, name, qualifiers, optional, typeVariableContext);
        }

        // handle Class<T1,T2> etc.
        // Extract and convert fields with resolved types
        // Capture generic type variables of the class from the parent context
        final var childContext = TypeVariableContext.from(rawType, annotatedParameterizedType, typeVariableContext);
        final var fieldAttributes = FieldUtils.getAllFields(rawType)
                .stream()
                .map(field -> schemaAttribute(field, childContext)) // Resolves TypeVariable<T>
                .collect(Collectors.toSet());

        return new ObjectAttribute(name, optional, qualifiers, fieldAttributes);
    }

    private SchemaAttribute handleMap(final AnnotatedParameterizedType annotatedParameterizedType,
                                      final String name,
                                      final Set<QualifierInfo> qualifiers,
                                      final boolean optional,
                                      final TypeVariableContext typeVariableContext) {
        final var annotatedKeyType = typeVariableContext.resolveType(annotatedParameterizedType.getAnnotatedActualTypeArguments()[0]);
        final var annotatedValueType = typeVariableContext.resolveType(annotatedParameterizedType.getAnnotatedActualTypeArguments()[1]);
        return new MapAttribute(
                name,
                optional,
                qualifiers,
                schemaAttribute(annotatedKeyType, "key", BuilderUtils.isOptional(annotatedKeyType),
                        BuilderUtils.getQualifiers(annotatedKeyType), typeVariableContext),
                schemaAttribute(annotatedValueType, "value",  BuilderUtils.isOptional(annotatedKeyType),
                        BuilderUtils.getQualifiers(annotatedKeyType), typeVariableContext)
        );
    }

    private SchemaAttribute handleCollection(final AnnotatedParameterizedType annotatedParameterizedType,
                                             final String name,
                                             final Set<QualifierInfo> qualifiers,
                                             final boolean optional,
                                             final TypeVariableContext typeVariableContext) {
        final var annotatedElementType = typeVariableContext.resolveType(annotatedParameterizedType.getAnnotatedActualTypeArguments()[0]);
        return new ArrayAttribute(
                name,
                optional,
                qualifiers,
                schemaAttribute(annotatedElementType, ELEMENT,  BuilderUtils.isOptional(annotatedElementType),
                        BuilderUtils.getQualifiers(annotatedElementType), typeVariableContext)
        );
    }

    private SchemaAttribute handleGenericArray(final AnnotatedArrayType annotatedArrayType,
                                               final String name,
                                               final Set<QualifierInfo> qualifiers,
                                               final boolean optional,
                                               final TypeVariableContext typeVariableContext) {
        final var annotatedComponentType = typeVariableContext.resolveType(annotatedArrayType.getAnnotatedGenericComponentType());
        return new ArrayAttribute(
                name,
                optional,
                qualifiers,
                schemaAttribute(annotatedComponentType, ELEMENT,  BuilderUtils.isOptional(annotatedComponentType),
                        BuilderUtils.getQualifiers(annotatedComponentType),  typeVariableContext)
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
