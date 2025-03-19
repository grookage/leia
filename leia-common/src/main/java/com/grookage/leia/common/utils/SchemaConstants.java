package com.grookage.leia.common.utils;

import lombok.experimental.UtilityClass;

import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

@UtilityClass
public class SchemaConstants {
    public static final Set<Class<?>> SUPPORTED_DATE_CLASSES = Set.of(
            LocalDate.class, LocalTime.class, LocalDateTime.class,
            ZonedDateTime.class, OffsetDateTime.class, OffsetTime.class,
            Instant.class,
            Date.class, Calendar.class
    );

    public static final Set<Class<?>> BOXED_PRIMITIVES = Set.of(
            Integer.class, Boolean.class, Double.class, Long.class,
            Float.class, Short.class, Character.class, Byte.class
    );
}
