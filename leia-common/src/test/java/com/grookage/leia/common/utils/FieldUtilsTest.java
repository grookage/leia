package com.grookage.leia.common.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FieldUtilsTest {
	@Test
	void testFieldUtils() {
		final var fields = FieldUtils.getAllFields(TestData.class);
		Assertions.assertFalse(fields.isEmpty());
		Assertions.assertEquals(2, fields.size());
		Assertions.assertTrue(fields.stream().noneMatch(field -> field.getName().equals("phoneNumber")));
		Assertions.assertTrue(fields.stream().noneMatch(field -> field.getName().equals("CONSTANT")));
		Assertions.assertTrue(fields.stream().noneMatch(field -> field.getName().equals("exclusion")));
	}

	static class BaseData {
		static final String CONSTANT = "CONSTANT";
		String name;
		@JsonIgnore
		private String exclusion;
	}

	static class TestData extends BaseData {
		String email;
		transient String phoneNumber;
	}
}
