package com.grookage.leia.validator;

import com.grookage.leia.common.builder.SchemaBuilder;
import com.grookage.leia.common.exception.SchemaValidationException;
import com.grookage.leia.common.exception.ValidationErrorCode;
import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.annotations.SchemaDefinition;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.engine.SchemaState;
import com.grookage.leia.validator.stubs.TestNestedRecordStub;
import com.grookage.leia.validator.stubs.TestRecordStub;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class StaticSchemaValidatorTest {
    private static final String PACKAGE = "com.grookage.leia.validator.stubs";

    @Test
    void testSchemaValidator() {
        final var recordStubDetails = toSchemaDetails(TestRecordStub.class);
        final var nestedStubDetails = toSchemaDetails(TestNestedRecordStub.class);
        final var schemaValidator = StaticSchemaValidator.builder()
                .packageRoots(Set.of(PACKAGE))
                .supplier(() -> List.of(recordStubDetails, nestedStubDetails))
                .build();
        Assertions.assertDoesNotThrow(schemaValidator::start);
        Assertions.assertTrue(schemaValidator.valid(schemaKey(TestRecordStub.class)));
        Assertions.assertTrue(schemaValidator.valid(schemaKey(TestNestedRecordStub.class)));
    }

    @Test
    void testSchemaValidator_withMissingSchemaDetails() {
        final var recordStubDetails = toSchemaDetails(TestRecordStub.class);
        final var schemaValidator = StaticSchemaValidator.builder()
                .packageRoots(Set.of(PACKAGE))
                .supplier(() -> List.of(recordStubDetails))
                .build();
        final var schemaValidationException = Assertions.assertThrows(SchemaValidationException.class, schemaValidator::start);
        Assertions.assertEquals(ValidationErrorCode.NO_SCHEMA_FOUND.name(), schemaValidationException.getCode());
    }

    @SneakyThrows
    @Test
    void testSchemaValidator_withInvalidSchema() {
        final var recordStubDetails = ResourceHelper.getResource("InvalidRecordStubSchema.json", SchemaDetails.class);
        final var nestedStubDetails = ResourceHelper.getResource("InvalidNestedRecordStubSchema.json", SchemaDetails.class);
        final var schemaValidator = StaticSchemaValidator.builder()
                .packageRoots(Set.of(PACKAGE))
                .supplier(() -> List.of(recordStubDetails, nestedStubDetails))
                .build();
        final var schemaValidationException = Assertions.assertThrows(SchemaValidationException.class, schemaValidator::start);
        Assertions.assertEquals(ValidationErrorCode.INVALID_SCHEMAS.name(), schemaValidationException.getCode());
        Assertions.assertFalse(schemaValidator.valid(schemaKey(TestRecordStub.class)));
        Assertions.assertFalse(schemaValidator.valid(schemaKey(TestNestedRecordStub.class)));
    }


    private SchemaDetails toSchemaDetails(final Class<?> schemaKlass) {
        final var schemaDefinition = schemaKlass.getAnnotation(SchemaDefinition.class);
        final var createSchemaRequest = SchemaBuilder.buildSchemaRequest(schemaKlass)
                .orElse(null);
        Assertions.assertNotNull(createSchemaRequest);
        return SchemaDetails.builder()
                .schemaKey(createSchemaRequest.getSchemaKey())
                .schemaState(SchemaState.CREATED)
                .schemaType(createSchemaRequest.getSchemaType())
                .description(createSchemaRequest.getDescription())
                .changeLog(createSchemaRequest.getChangeLog())
                .attributes(createSchemaRequest.getAttributes())
                .validationType(createSchemaRequest.getValidationType())
                .transformationTargets(createSchemaRequest.getTransformationTargets())
                .tags(createSchemaRequest.getTags())
                .build();
    }

    private SchemaKey schemaKey(final Class<?> schemaKlass) {
        final var schemaDefinition = schemaKlass.getAnnotation(SchemaDefinition.class);
        return SchemaKey.builder()
                .schemaName(schemaDefinition.name())
                .namespace(schemaDefinition.namespace())
                .version(schemaDefinition.version())
                .orgId(schemaDefinition.orgId())
                .tenantId(schemaDefinition.tenantId())
                .type(schemaDefinition.type())
                .build();
    }
}