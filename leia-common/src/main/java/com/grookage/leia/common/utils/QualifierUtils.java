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

import com.grookage.leia.models.annotations.attribute.qualifiers.Encrypted;
import com.grookage.leia.models.annotations.attribute.qualifiers.PII;
import com.grookage.leia.models.annotations.attribute.qualifiers.ShortLived;
import com.grookage.leia.models.qualifiers.*;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@UtilityClass
public class QualifierUtils {
    public Optional<QualifierInfo> filter(final Set<QualifierInfo> qualifiers,
                                          final QualifierType type) {
        if (Objects.isNull(qualifiers)) {
            return Optional.empty();
        }
        return qualifiers.stream()
                .filter(qualifierInfo -> qualifierInfo.getType().equals(type))
                .findFirst();
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
