package com.grookage.leia.common.stubs;

import com.grookage.leia.models.annotations.attribute.qualifiers.Encrypted;
import com.grookage.leia.models.annotations.attribute.qualifiers.PII;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class NestedStub {
	String name;
	int id;
	@PII
	@Encrypted
	PIIData piiData;
	@Encrypted
	RecordStub recordStub;
	TestEnum enumClass;
	@PII
	String phoneNumber;
}
