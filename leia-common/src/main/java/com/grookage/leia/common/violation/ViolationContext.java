package com.grookage.leia.common.violation;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
public class ViolationContext {
    @Getter
    private final List<LeiaSchemaViolation> violations = new ArrayList<>();
    private final LinkedList<Class<?>> klassPath = new LinkedList<>();

    public void addViolation(final String message) {
        violations.add(new LeiaSchemaViolationImpl(message, null, klassPath.peekLast()));
    }

    public void addViolation(final String message,
                             final String path) {
        violations.add(new LeiaSchemaViolationImpl(message, path, klassPath.peekLast()));
    }

    public void pushClass(final Class<?> klass) {
        klassPath.addLast(klass);
    }

    public void popClass() {
        if (!klassPath.isEmpty()) {
            klassPath.removeLast();
        }
    }
}
