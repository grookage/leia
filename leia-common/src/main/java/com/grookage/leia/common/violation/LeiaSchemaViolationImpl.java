package com.grookage.leia.common.violation;

import com.google.common.base.Joiner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeiaSchemaViolationImpl implements LeiaSchemaViolation {
    private String message;
    private String fieldPath;
    private Class<?> rootKlass;

    @Override
    public String message() {
        return message;
    }

    @Override
    public String fieldPath() {
        return fieldPath;
    }

    @Override
    public Class<?> rootKlass() {
        return rootKlass;
    }

    public String toString() {
        return Joiner.on(":")
                .skipNulls()
                .join(rootKlass, fieldPath, message);
    }
}
