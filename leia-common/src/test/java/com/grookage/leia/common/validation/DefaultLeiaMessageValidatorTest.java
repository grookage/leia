package com.grookage.leia.common.validation;

import com.grookage.leia.common.builder.SchemaBuilder;
import com.grookage.leia.common.stubs.NestedStub;
import com.grookage.leia.common.stubs.TestObjectStub;
import com.grookage.leia.common.stubs.TestParameterizedStub;
import com.grookage.leia.common.stubs.TestRawCollectionStub;
import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.attributes.*;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.SchemaValidationType;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefaultLeiaMessageValidatorTest {
    private DefaultLeiaMessageValidator validator;
    private static final SchemaKey SCHEMA_KEY = SchemaKey.builder()
            .namespace("testNamespace")
            .schemaName("testSchema")
            .version("v")
            .orgId("testOrg")
            .type("default")
            .tenantId("tenantId")
            .build();

    @BeforeEach
    public void setup() {
        validator = new DefaultLeiaMessageValidator();
    }

    @Test
    void testValidJsonAgainstSchema() throws Exception {
        final var jsonNode = ResourceHelper.getObjectMapper().readTree("""
                {
                    "name": "John Doe",
                    "age": 30,
                    "isActive": true
                }
                """);

        final var schemaAttributes = Set.of(
                new StringAttribute("name", false, null),
                new IntegerAttribute("age", false, null),
                new BooleanAttribute("isActive", false, null)
        );
        final var schemaDetails = SchemaDetails.builder()
                .schemaKey(SCHEMA_KEY)
                .attributes(schemaAttributes)
                .validationType(SchemaValidationType.STRICT)
                .build();

        final var errors = validator.validate(schemaDetails, jsonNode);

        assertTrue(errors.isEmpty());
    }

    @Test
    void testUnexpectedFieldInJson() throws Exception {
        final var jsonNode = ResourceHelper.getObjectMapper().readTree("""
                {
                    "name": "John Doe",
                    "age": 30,
                    "isActive": true,
                    "unexpectedField": "extra"
                }
                """);

        final var schemaAttributes = Set.of(
                new StringAttribute("name", false, null),
                new IntegerAttribute("age", false, null),
                new BooleanAttribute("isActive", false, null)
        );

        final var schemaDetails = SchemaDetails.builder()
                .schemaKey(SCHEMA_KEY)
                .attributes(schemaAttributes)
                .validationType(SchemaValidationType.STRICT)
                .build();

        final var errors = validator.validate(schemaDetails, jsonNode);

        assertFalse(errors.isEmpty());
        assertEquals(1, errors.size());
        assertEquals("Unexpected field: unexpectedField", errors.get(0).message());
    }

    @Test
    void testMissingRequiredField() throws Exception {
        final var jsonNode = ResourceHelper.getObjectMapper().readTree("""
                {
                    "name": "John Doe"
                }
                """);

        final var schemaAttributes = Set.of(
                new StringAttribute("name", false, null),
                new IntegerAttribute("age", false, null)
        );

        final var schemaDetails = SchemaDetails.builder()
                .schemaKey(SCHEMA_KEY)
                .attributes(schemaAttributes)
                .validationType(SchemaValidationType.STRICT)
                .build();

        final var errors = validator.validate(schemaDetails, jsonNode);

        assertFalse(errors.isEmpty());
        assertEquals(1, errors.size());
        assertEquals("Missing required field: age", errors.get(0).message());
    }

    @Test
    void testTypeMismatch() throws Exception {
        final var jsonNode = ResourceHelper.getObjectMapper().readTree("""
                {
                    "name": "John Doe",
                    "age": "thirty"
                }
                """);

        final var schemaAttributes = Set.of(
                new StringAttribute("name", false, null),
                new IntegerAttribute("age", false, null)
        );

        final var schemaDetails = SchemaDetails.builder()
                .schemaKey(SCHEMA_KEY)
                .attributes(schemaAttributes)
                .validationType(SchemaValidationType.STRICT)
                .build();

        final var errors = validator.validate(schemaDetails, jsonNode);

        assertFalse(errors.isEmpty());
        assertEquals(1, errors.size());
        assertEquals("Type mismatch for field: age. Expected: INTEGER, Found: STRING", errors.get(0).message());
    }

    @Test
    void testNestedObjectValidation() throws Exception {
        final var jsonNode = ResourceHelper.getObjectMapper().readTree("""
                {
                    "user": {
                        "id": 1,
                        "username": "johndoe"
                    }
                }
                """);

        final Set<SchemaAttribute> nestedAttributes = Set.of(
                new IntegerAttribute("id", false, null),
                new StringAttribute("username", false, null)
        );

        final Set<SchemaAttribute> schemaAttributes = Set.of(
                new ObjectAttribute("user", false, null, nestedAttributes)
        );

        final var schemaDetails = SchemaDetails.builder()
                .schemaKey(SCHEMA_KEY)
                .attributes(schemaAttributes)
                .validationType(SchemaValidationType.STRICT)
                .build();

        final var errors = validator.validate(schemaDetails, jsonNode);

        assertTrue(errors.isEmpty());
    }

    @Test
    void testArrayValidation() throws Exception {
        final var jsonNode = ResourceHelper.getObjectMapper().readTree("""
                {
                    "numbers": [1, 2, 3, 4]
                }
                """);

        final Set<SchemaAttribute> schemaAttributes = Set.of(
                new ArrayAttribute("numbers", false, null, new IntegerAttribute("element", false, null))
        );

        final var schemaDetails = SchemaDetails.builder()
                .schemaKey(SCHEMA_KEY)
                .attributes(schemaAttributes)
                .validationType(SchemaValidationType.STRICT)
                .build();

        final var errors = validator.validate(schemaDetails, jsonNode);

        assertTrue(errors.isEmpty());
    }

    @Test
    void testMapValidation() throws Exception {
        final var jsonNode = ResourceHelper.getObjectMapper().readTree("""
                {
                    "attributes": {
                        "key1": "value1",
                        "key2": "value2"
                    }
                }
                """);

        final Set<SchemaAttribute> schemaAttributes = Set.of(
                new MapAttribute(
                        "attributes",
                        false,
                        null,
                        new StringAttribute("key", false, null),
                        new StringAttribute("value", false, null)
                )
        );

        final var schemaDetails = SchemaDetails.builder()
                .schemaKey(SCHEMA_KEY)
                .attributes(schemaAttributes)
                .validationType(SchemaValidationType.STRICT)
                .build();

        final var errors = validator.validate(schemaDetails, jsonNode);

        assertTrue(errors.isEmpty());
    }

    @Test
    void testInvalidMapValueType() throws Exception {
        final var jsonNode = ResourceHelper.getObjectMapper().readTree("""
                {
                    "attributes": {
                        "key1": 100
                    }
                }
                """);

        final Set<SchemaAttribute> schemaAttributes = Set.of(
                new MapAttribute(
                        "attributes",
                        false,
                        null,
                        new StringAttribute("key", false, null),
                        new StringAttribute("value", false, null)
                )
        );

        final var schemaDetails = SchemaDetails.builder()
                .schemaKey(SCHEMA_KEY)
                .attributes(schemaAttributes)
                .validationType(SchemaValidationType.STRICT)
                .build();

        final var errors = validator.validate(schemaDetails, jsonNode);

        assertFalse(errors.isEmpty());
        assertEquals(1, errors.size());
    }

    @SneakyThrows
    @Test
    void testValidateNested() {
        final var jsonNode = ResourceHelper.getObjectMapper().valueToTree(ResourceHelper.getResource("stubs/validNestedStub.json",
                NestedStub.class));
        final var schemaAttributes = SchemaBuilder.getSchemaAttributes(NestedStub.class);
        final var schemaDetails = SchemaDetails.builder()
                .schemaKey(SCHEMA_KEY)
                .attributes(schemaAttributes)
                .validationType(SchemaValidationType.STRICT)
                .build();

        final var errors = validator.validate(schemaDetails, jsonNode);
        assertTrue(errors.isEmpty());
    }

    @SneakyThrows
    @Test
    void testValidateParameterizedStub() {
        final var jsonNode = ResourceHelper.getObjectMapper().valueToTree(ResourceHelper.getResource("stubs/validParameterizedStub.json",
                TestParameterizedStub.class));
        final var schemaAttributes = SchemaBuilder.getSchemaAttributes(TestParameterizedStub.class);
        final var schemaDetails = SchemaDetails.builder()
                .schemaKey(SCHEMA_KEY)
                .attributes(schemaAttributes)
                .validationType(SchemaValidationType.STRICT)
                .build();

        final var errors = validator.validate(schemaDetails, jsonNode);
        assertTrue(errors.isEmpty());
    }

    @SneakyThrows
    @Test
    void testObjectValidation() {
        final var jsonNode = ResourceHelper.getObjectMapper().valueToTree(ResourceHelper.getResource("stubs/validObjectStub.json",
                TestObjectStub.class));
        final var schemaAttributes = SchemaBuilder.getSchemaAttributes(TestObjectStub.class);
        final var schemaDetails = SchemaDetails.builder()
                .schemaKey(SCHEMA_KEY)
                .attributes(schemaAttributes)
                .validationType(SchemaValidationType.STRICT)
                .build();

        final var errors = validator.validate(schemaDetails, jsonNode);
        Assertions.assertTrue(errors.isEmpty());
    }

    @SneakyThrows
    @Test
    void testRawCollectionSchemaValidation() {
        final var jsonNode = ResourceHelper.getObjectMapper().valueToTree(ResourceHelper.getResource("stubs/validRawCollectionStub.json",
                TestRawCollectionStub.class));
        final var schemaAttributes = SchemaBuilder.getSchemaAttributes(TestRawCollectionStub.class);
        final var schemaDetails = SchemaDetails.builder()
                .schemaKey(SCHEMA_KEY)
                .attributes(schemaAttributes)
                .validationType(SchemaValidationType.STRICT)
                .build();

        final var errors = validator.validate(schemaDetails, jsonNode);
        Assertions.assertTrue(errors.isEmpty());
    }
}