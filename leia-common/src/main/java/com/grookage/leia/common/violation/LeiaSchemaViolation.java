package com.grookage.leia.common.violation;

/**
 * Violation for schema validation that includes class context
 */
public interface LeiaSchemaViolation extends LeiaViolation {

    /**
     * @return Class of the field being validated
     */
    Class<?> rootKlass();
}
