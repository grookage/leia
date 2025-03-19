package com.grookage.leia.models.attributes;

import lombok.AllArgsConstructor;

import java.util.function.Function;

@AllArgsConstructor
public abstract class SchemaAttributeHandler<T> implements SchemaAttributeAcceptor<T> {
    private final Function<SchemaAttribute, T> defaultHandler;

    @Override
    public T accept(BooleanAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(ByteAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(DoubleAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(EnumAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(FloatAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(IntegerAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(LongAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(ShortAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(CharacterAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(final StringAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(final DateAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(final ArrayAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(final MapAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(final ObjectAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(final ParameterizedObjectAttribute attribute) {
        return defaultHandler.apply(attribute);
    }

    @Override
    public T accept(final TypeAttribute attribute) {
        return defaultHandler.apply(attribute);
    }
}
