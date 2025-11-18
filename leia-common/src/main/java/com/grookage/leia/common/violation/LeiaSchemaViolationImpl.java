package com.grookage.leia.common.violation;

import com.google.common.base.Strings;
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
		if (Strings.isNullOrEmpty(fieldPath)) {
			return String.format("[LeiaSchemaViolation] %s, message = %s", rootKlass, message);
		}
		return String.format("[LeiaSchemaViolation] %s, fieldPath = %s, message = %s", rootKlass,
				fieldPath, message);
	}
}
