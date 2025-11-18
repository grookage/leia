package com.grookage.leia.validator.stubs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TestData {
	int id;
	String name;
	Object object;
	TestEnum testEnum;
}
