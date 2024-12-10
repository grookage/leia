package com.grookage.leia.common.violation;

public interface LeiaSchemaViolation {
    String message();

    String fieldPath();

    Class<?> rootKlass();
}
