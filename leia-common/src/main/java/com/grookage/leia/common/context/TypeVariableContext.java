package com.grookage.leia.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
public class TypeVariableContext {
    private final Map<TypeVariable<?>, Type> typeVariableMap = new HashMap<>();

    public void addMapping(final TypeVariable<?> variable,
                           final Type type) {
        typeVariableMap.put(variable, type);
    }

    public Type resolveType(Type type) {
        if (type instanceof TypeVariable<?> typeVariable) {
            return typeVariableMap.getOrDefault(typeVariable, Object.class);
        }
        return type;
    }

    public static TypeVariableContext from(final Class<?> rawType,
                                           final ParameterizedType parameterizedType,
                                           final TypeVariableContext parentContext) {
        final var newContext = new TypeVariableContext();
        TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

        for (int i = 0; i < typeParameters.length; i++) {
            newContext.addMapping(typeParameters[i], parentContext.resolveType(actualTypeArguments[i]));
        }
        return newContext;
    }


}
