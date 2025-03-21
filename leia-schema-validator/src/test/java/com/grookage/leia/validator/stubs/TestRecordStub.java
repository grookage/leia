package com.grookage.leia.validator.stubs;

import com.grookage.leia.models.GenericResponse;
import com.grookage.leia.models.annotations.SchemaDefinition;
import com.grookage.leia.models.annotations.attribute.Optional;
import com.grookage.leia.models.annotations.attribute.qualifiers.Encrypted;
import com.grookage.leia.models.annotations.attribute.qualifiers.PII;
import com.grookage.leia.models.schema.SchemaType;
import com.grookage.leia.models.schema.SchemaValidationType;

@SchemaDefinition(
        name = TestRecordStub.NAME,
        namespace = TestRecordStub.NAMESPACE,
        version = TestRecordStub.VERSION,
        description = TestRecordStub.DESCRIPTION,
        type = SchemaType.JSON,
        validation = SchemaValidationType.STRICT
)
public class TestRecordStub {
    static final String NAME = "TEST_RECORD";
    static final String NAMESPACE = "test";
    static final String VERSION = "v1";
    static final String DESCRIPTION = "Test Record";

    int id;
    String name;
    @PII
    @Encrypted
    String accountNumber;
    long ttl;
    @Optional
    String accountId;
}
