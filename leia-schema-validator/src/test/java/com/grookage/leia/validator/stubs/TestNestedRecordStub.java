package com.grookage.leia.validator.stubs;

import com.grookage.leia.models.annotations.SchemaDefinition;
import com.grookage.leia.models.annotations.attribute.Optional;
import com.grookage.leia.models.annotations.attribute.qualifiers.Encrypted;
import com.grookage.leia.models.annotations.attribute.qualifiers.PII;
import com.grookage.leia.models.schema.SchemaType;
import com.grookage.leia.models.schema.SchemaValidationType;

import java.util.List;
import java.util.Map;

@SchemaDefinition(
        name = TestNestedRecordStub.NAME,
        namespace = TestNestedRecordStub.NAMESPACE,
        version = TestNestedRecordStub.VERSION,
        description = TestNestedRecordStub.DESCRIPTION,
        type = SchemaType.JSON,
        validation = SchemaValidationType.STRICT
)
public class TestNestedRecordStub {
    static final String NAME = "TEST_NESTED_RECORD";
    static final String NAMESPACE = "test";
    static final String VERSION = "v1";
    static final String DESCRIPTION = "Test Nested Record";

    @PII
    @Encrypted
    String accountNumber;
    @Optional
    String accountId;
    TestData testData;
    Map<TestEnum, TestData> enumTestDataMap;
    List<String> strings;
}