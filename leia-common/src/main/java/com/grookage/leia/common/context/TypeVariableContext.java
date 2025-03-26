package com.grookage.leia.common.context;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
public class TypeVariableContext {
    private final Map<TypeVariable<?>, AnnotatedType> typeVariableMap = new HashMap<>();

    public void add(final TypeVariable<?> variable,
                    final AnnotatedType type) {
        typeVariableMap.put(variable, type);
    }

    public AnnotatedType resolveType(final AnnotatedType type) {
        if (type instanceof AnnotatedTypeVariable annotatedTypeVariable) {
            return typeVariableMap.get(annotatedTypeVariable.getType());
        }
        return type;
    }

    public static TypeVariableContext from(final Class<?> rawType,
                                           final AnnotatedParameterizedType annotatedParameterizedType,
                                           final TypeVariableContext parentContext) {
        final var newContext = new TypeVariableContext();
        TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
        AnnotatedType[] actualTypeArguments = annotatedParameterizedType.getAnnotatedActualTypeArguments();

        for (int i = 0; i < typeParameters.length; i++) {
            newContext.add(typeParameters[i], parentContext.resolveType(actualTypeArguments[i]));
        }
        return newContext;
    }


}
