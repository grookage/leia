package com.grookage.leia.common.utils;

import com.grookage.leia.models.annotations.attribute.Optional;
import com.grookage.leia.models.annotations.attribute.qualifiers.Encrypted;
import com.grookage.leia.models.annotations.attribute.qualifiers.PII;
import com.grookage.leia.models.annotations.attribute.qualifiers.ShortLived;
import com.grookage.leia.models.attributes.BooleanAttribute;
import com.grookage.leia.models.attributes.ByteAttribute;
import com.grookage.leia.models.attributes.CharacterAttribute;
import com.grookage.leia.models.attributes.DoubleAttribute;
import com.grookage.leia.models.attributes.FloatAttribute;
import com.grookage.leia.models.attributes.IntegerAttribute;
import com.grookage.leia.models.attributes.LongAttribute;
import com.grookage.leia.models.attributes.SchemaAttribute;
import com.grookage.leia.models.attributes.ShortAttribute;
import com.grookage.leia.models.qualifiers.EncryptedQualifier;
import com.grookage.leia.models.qualifiers.PIIQualifier;
import com.grookage.leia.models.qualifiers.QualifierInfo;
import com.grookage.leia.models.qualifiers.ShortLivedQualifier;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class BuilderUtils {
    public SchemaAttribute buildPrimitiveAttribute(final Class<?> klass,
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

    public Set<String> getEnumValues(final Class<?> klass) {
        return Arrays.stream(klass.getEnumConstants())
                .map(enumConstant -> ((Enum<?>) enumConstant).name())
                .collect(Collectors.toSet());
    }

    public boolean isOptional(final Type type) {
        if (type instanceof Class<?> klass) {
            return isOptional(klass);
        }
        return false;
    }

    public boolean isOptional(final Class<?> klass) {
        return klass.isAnnotationPresent(Optional.class);
    }

    public boolean isOptional(final Field field) {
        return field.isAnnotationPresent(Optional.class);
    }

    public Set<QualifierInfo> getQualifiers(final Type type) {
        if (type instanceof Class<?> klass) {
            return getQualifiers(klass);
        }
        return new HashSet<>();
    }

    public Set<QualifierInfo> getQualifiers(final Field field) {
        Set<QualifierInfo> qualifiers = new HashSet<>();
        if (field.isAnnotationPresent(Encrypted.class)) {
            qualifiers.add(new EncryptedQualifier());
        }
        if (field.isAnnotationPresent(PII.class)) {
            qualifiers.add(new PIIQualifier());
        }
        if (field.isAnnotationPresent(ShortLived.class)) {
            final var shortLived = field.getAnnotation(ShortLived.class);
            qualifiers.add(new ShortLivedQualifier(shortLived.ttlSeconds()));
        }
        return qualifiers;
    }

    public Set<QualifierInfo> getQualifiers(final Class<?> klass) {
        Set<QualifierInfo> qualifiers = new HashSet<>();
        if (klass.isAnnotationPresent(Encrypted.class)) {
            qualifiers.add(new EncryptedQualifier());
        }
        if (klass.isAnnotationPresent(PII.class)) {
            qualifiers.add(new PIIQualifier());
        }
        if (klass.isAnnotationPresent(ShortLived.class)) {
            final var shortLived = klass.getAnnotation(ShortLived.class);
            qualifiers.add(new ShortLivedQualifier(shortLived.ttlSeconds()));
        }
        return qualifiers;
    }
}
