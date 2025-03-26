package com.grookage.leia.common.builder;

import com.grookage.leia.common.LeiaTestUtils;
import com.grookage.leia.common.stubs.*;
import com.grookage.leia.models.annotations.SchemaDefinition;
import com.grookage.leia.models.annotations.attribute.Optional;
import com.grookage.leia.models.annotations.attribute.qualifiers.Encrypted;
import com.grookage.leia.models.annotations.attribute.qualifiers.PII;
import com.grookage.leia.models.attributes.*;
import com.grookage.leia.models.qualifiers.EncryptedQualifier;
import com.grookage.leia.models.qualifiers.PIIQualifier;
import com.grookage.leia.models.schema.SchemaType;
import com.grookage.leia.models.schema.SchemaValidationType;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

class SchemaBuilderTest {
    @SneakyThrows
    @Test
    void testSchemaRequest() {
        final var schemaCreateRequest = SchemaBuilder.buildSchemaRequest(TestRecord.class)
                .orElse(null);
        Assertions.assertNotNull(schemaCreateRequest);
        Assertions.assertEquals(7, schemaCreateRequest.getAttributes().size());
        final var schemaAttributes = SchemaBuilder.getSchemaAttributes(TestRecord.class);
        Assertions.assertEquals(TestRecord.NAME, schemaCreateRequest.getSchemaName());
        Assertions.assertEquals(TestRecord.NAMESPACE, schemaCreateRequest.getNamespace());
        Assertions.assertEquals(TestRecord.DESCRIPTION, schemaCreateRequest.getDescription());
        Assertions.assertEquals(SchemaType.JSON, schemaCreateRequest.getSchemaType());
        Assertions.assertEquals(SchemaValidationType.MATCHING, schemaCreateRequest.getValidationType());
        Assertions.assertEquals(schemaAttributes.size(), schemaCreateRequest.getAttributes().size());
        Assertions.assertEquals(2, schemaCreateRequest.getTags().size());
    }

    @Test
    void testSchemaRequest_WithInvalidClass() {
        Assertions.assertTrue(SchemaBuilder.buildSchemaRequest(null).isEmpty());
        Assertions.assertTrue(SchemaBuilder.buildSchemaRequest(TestObject.class).isEmpty());
    }

    @Test
    void testSchemaAttributes_WithPrimitiveClass() {
        final var schemaAttributeSet = SchemaBuilder.getSchemaAttributes(PrimitiveTestClass.class);
        Assertions.assertNotNull(schemaAttributeSet);
        Assertions.assertEquals(9, schemaAttributeSet.size());
        final var nameAttribute = new StringAttribute("name", true, new HashSet<>());
        LeiaTestUtils.assertEquals(nameAttribute, LeiaTestUtils.filter(schemaAttributeSet, "name").orElse(null));
        final var idAttribute = new IntegerAttribute("id", false, new HashSet<>());
        LeiaTestUtils.assertEquals(idAttribute, LeiaTestUtils.filter(schemaAttributeSet, "id").orElse(null));
    }

    @Test
    void testSchemaAttributes_WithRecordClass() {
        final var schemaAttributeSet = SchemaBuilder.getSchemaAttributes(RecordStub.class);
        Assertions.assertNotNull(schemaAttributeSet);
        Assertions.assertEquals(2, schemaAttributeSet.size());
        final var nameAttribute = new StringAttribute("name", false, Set.of(new PIIQualifier()));
        LeiaTestUtils.assertEquals(nameAttribute, LeiaTestUtils.filter(schemaAttributeSet, "name").orElse(null));
        final var idAttribute = new IntegerAttribute("id", true, new HashSet<>());
        LeiaTestUtils.assertEquals(idAttribute, LeiaTestUtils.filter(schemaAttributeSet, "id").orElse(null));
    }

    @Test
    void testSchemaAttributes_WithNestedObject() {
        final var schemaAttributes = SchemaBuilder.getSchemaAttributes(NestedStub.class);
        Assertions.assertFalse(schemaAttributes.isEmpty());
        Assertions.assertEquals(6, schemaAttributes.size());
        final var nameAttribute = new StringAttribute("name", false, new HashSet<>());
        LeiaTestUtils.assertEquals(nameAttribute, LeiaTestUtils.filter(schemaAttributes, "name").orElse(null));

        final var idAttribute = new IntegerAttribute("id", false, new HashSet<>());
        LeiaTestUtils.assertEquals(idAttribute, LeiaTestUtils.filter(schemaAttributes, "id").orElse(null));

        final var testPIIDataAttributes = new HashSet<SchemaAttribute>();
        final var piiNameAttribute = new StringAttribute("name", false, new HashSet<>());
        final var accountNumberAttribute = new StringAttribute("accountNumber", false, Set.of(new EncryptedQualifier()));
        testPIIDataAttributes.add(piiNameAttribute);
        testPIIDataAttributes.add(accountNumberAttribute);
        final var piiDataAttribute = new ObjectAttribute("piiData", false, Set.of(new PIIQualifier(), new EncryptedQualifier()), testPIIDataAttributes);
        LeiaTestUtils.assertEquals(piiDataAttribute, LeiaTestUtils.filter(schemaAttributes, "piiData").orElse(null));

        final var testRecordAttributes = new HashSet<SchemaAttribute>();
        final var recordNameAttribute = new StringAttribute("name", false, Set.of(new PIIQualifier()));
        final var recordIdAttribute = new IntegerAttribute("id", true, new HashSet<>());
        testRecordAttributes.add(recordNameAttribute);
        testRecordAttributes.add(recordIdAttribute);
        final var testRecordAttribute = new ObjectAttribute("recordStub", false, Set.of(new EncryptedQualifier()),
                testRecordAttributes);
        LeiaTestUtils.assertEquals(testRecordAttribute, LeiaTestUtils.filter(schemaAttributes, "recordStub").orElse(null));

        final var enumClassAttribute = new EnumAttribute("enumClass", false, new HashSet<>(), Set.of(TestEnum.ONE.name(),
                TestEnum.TWO.name()));
        LeiaTestUtils.assertEquals(enumClassAttribute, LeiaTestUtils.filter(schemaAttributes, "enumClass").orElse(null));

        final var phoneNoAttribute = new StringAttribute("phoneNumber", false, Set.of(new PIIQualifier()));
        LeiaTestUtils.assertEquals(phoneNoAttribute, LeiaTestUtils.filter(schemaAttributes, "phoneNumber").orElse(null));
    }

    @Test
    void testSchemaAttributes_WithParameterizedType() {
        final var schemaAttributes = SchemaBuilder.getSchemaAttributes(TestParameterizedStub.class);
        Assertions.assertNotNull(schemaAttributes);
        Assertions.assertEquals(3, schemaAttributes.size());

        final var valuesAttributes = new ArrayAttribute("values", false, new HashSet<>(),
                new StringAttribute("element", false, new HashSet<>()));
        LeiaTestUtils.assertEquals(valuesAttributes, LeiaTestUtils.filter(schemaAttributes, "values").orElse(null));

        final var testPIIDataAttributes = new HashSet<SchemaAttribute>();
        final var piiNameAttribute = new StringAttribute("name", false, new HashSet<>());
        final var accountNumberAttribute = new StringAttribute("accountNumber", false, Set.of(new EncryptedQualifier()));
        testPIIDataAttributes.add(piiNameAttribute);
        testPIIDataAttributes.add(accountNumberAttribute);
        final var piiDataListAttribute = new ArrayAttribute("piiDataList", false, Set.of(),
                new ObjectAttribute("element", false, Set.of(new PIIQualifier()), testPIIDataAttributes));
        LeiaTestUtils.assertEquals(piiDataListAttribute, LeiaTestUtils.filter(schemaAttributes, "piiDataList").orElse(null));

        final var mapAttribute = new MapAttribute("map", false, Set.of(new EncryptedQualifier()),
                new EnumAttribute("key", false, new HashSet<>(), Set.of(TestEnum.ONE.name(), TestEnum.TWO.name())),
                new StringAttribute("value", false, new HashSet<>()));
        LeiaTestUtils.assertEquals(mapAttribute, LeiaTestUtils.filter(schemaAttributes, "map").orElse(null));
    }

    @Test
    void testSchemaAttributes_WithRawCollections() {
        final var schemaAttributes = SchemaBuilder.getSchemaAttributes(TestRawCollectionStub.class);
        Assertions.assertNotNull(schemaAttributes);
        Assertions.assertEquals(6, schemaAttributes.size());

        final var rawListAttribute = new ArrayAttribute("rawList", false, Set.of(), null);
        LeiaTestUtils.assertEquals(rawListAttribute, LeiaTestUtils.filter(schemaAttributes, "rawList").orElse(null));

        final var rawLinkedListAttribute = new ArrayAttribute("rawLinkedList", false, Set.of(), null);
        LeiaTestUtils.assertEquals(rawLinkedListAttribute, LeiaTestUtils.filter(schemaAttributes, "rawLinkedList").orElse(null));

        final var rawSetAttribute = new ArrayAttribute("rawSet", false, Set.of(), null);
        LeiaTestUtils.assertEquals(rawSetAttribute, LeiaTestUtils.filter(schemaAttributes, "rawSet").orElse(null));

        final var rawHashSetAttribute = new ArrayAttribute("rawHashSet", false, Set.of(), null);
        LeiaTestUtils.assertEquals(rawHashSetAttribute, LeiaTestUtils.filter(schemaAttributes, "rawHashSet").orElse(null));

        final var rawMapAttribute = new MapAttribute("rawMap", false, Set.of(), null, null);
        LeiaTestUtils.assertEquals(rawMapAttribute, LeiaTestUtils.filter(schemaAttributes, "rawMap").orElse(null));

        final var rawSortedMapAttribute = new MapAttribute("rawSortedMap", false, Set.of(), null, null);
        LeiaTestUtils.assertEquals(rawSortedMapAttribute, LeiaTestUtils.filter(schemaAttributes, "rawSortedMap").orElse(null));
    }

    @Test
    void testSchemaAttributes_WithObjects() {
        final var schemaAttributes = SchemaBuilder.getSchemaAttributes(TestObjectStub.class);
        Assertions.assertNotNull(schemaAttributes);
        Assertions.assertEquals(5, schemaAttributes.size());

        final var objectAttribute = new ObjectAttribute("object", false, Set.of(), null);
        LeiaTestUtils.assertEquals(objectAttribute, LeiaTestUtils.filter(schemaAttributes, "object").orElse(null));

        final var objectsAttribute = new ArrayAttribute("objects", false, Set.of(),
                new ObjectAttribute("element", false, Set.of(), null));
        LeiaTestUtils.assertEquals(objectsAttribute, LeiaTestUtils.filter(schemaAttributes, "objects").orElse(null));

        final var objectListAttribute = new ArrayAttribute("objectList", false, Set.of(),
                new ObjectAttribute("element", false, Set.of(), null));
        LeiaTestUtils.assertEquals(objectListAttribute, LeiaTestUtils.filter(schemaAttributes, "objectList").orElse(null));

        final var objectSetAttribute = new ArrayAttribute("objectSet", false, Set.of(),
                new ObjectAttribute("element", false, Set.of(), null));
        LeiaTestUtils.assertEquals(objectSetAttribute, LeiaTestUtils.filter(schemaAttributes, "objectSet").orElse(null));

        final var objectMapAttribute = new MapAttribute("objectMap", false, Set.of(),
                new StringAttribute("key", false, Set.of()),
                new ObjectAttribute("value", false, Set.of(), null));
        LeiaTestUtils.assertEquals(objectMapAttribute, LeiaTestUtils.filter(schemaAttributes, "objectMap").orElse(null));

    }

    @Test
    void testSchemaAttributes_WithGenerics() {
        final var schemaAttributes = SchemaBuilder.getSchemaAttributes(TestGenericStub.class);
        Assertions.assertNotNull(schemaAttributes);
        Assertions.assertEquals(1, schemaAttributes.size());
        final var objectAttribute = (ObjectAttribute) schemaAttributes.stream().findFirst().orElse(null);
        Assertions.assertNotNull(objectAttribute);
        final var nestedAttributes = objectAttribute.getNestedAttributes();
        Assertions.assertEquals(7, nestedAttributes.size());

        final var genericResponseAttributes = new HashSet<SchemaAttribute>();
        genericResponseAttributes.add(new BooleanAttribute("success", false, Set.of()));
        genericResponseAttributes.add(new StringAttribute("code", false, Set.of()));
        genericResponseAttributes.add(new StringAttribute("message", false, Set.of()));
        genericResponseAttributes.add(new IntegerAttribute("data", false, Set.of()));
        final var genericResponseAttribute = new ObjectAttribute("rGenericResponse", false, Set.of(), genericResponseAttributes);
        LeiaTestUtils.assertEquals(genericResponseAttribute, LeiaTestUtils.filter(nestedAttributes, "rGenericResponse").orElse(null));


        final var dataAttribute = new StringAttribute("data", false, Set.of());
        LeiaTestUtils.assertEquals(dataAttribute, LeiaTestUtils.filter(nestedAttributes, "data").orElse(null));

        final var keyAttribute = new IntegerAttribute("key", false, Set.of());
        LeiaTestUtils.assertEquals(keyAttribute, LeiaTestUtils.filter(nestedAttributes, "key").orElse(null));

        final var rangeAttributes = new HashSet<SchemaAttribute>();
        rangeAttributes.add(new ObjectAttribute("comparator", false, Set.of(), Set.of()));
        rangeAttributes.add(new IntegerAttribute("maximum", false, Set.of()));
        rangeAttributes.add(new IntegerAttribute("minimum", false, Set.of()));
        final var rangeAttribute = new ObjectAttribute("tRange", false, Set.of(), rangeAttributes);
        LeiaTestUtils.assertEquals(rangeAttribute, LeiaTestUtils.filter(nestedAttributes, "tRange").orElse(null));

        final var listAttribute = new ArrayAttribute("rList", false, Set.of(),
                new IntegerAttribute("element", false, Set.of()));
        LeiaTestUtils.assertEquals(listAttribute, LeiaTestUtils.filter(nestedAttributes, "rList").orElse(null));

        final var mapAttribute = new MapAttribute("urMap", false, Set.of(),
                new StringAttribute("key", false, Set.of()),
                new IntegerAttribute("value", false, Set.of()));
        LeiaTestUtils.assertEquals(mapAttribute, LeiaTestUtils.filter(nestedAttributes, "urMap").orElse(null));

        final var arrayAttribute = new ArrayAttribute("rArray", false, Set.of(),
                new IntegerAttribute("element", false, Set.of()));
        LeiaTestUtils.assertEquals(arrayAttribute, LeiaTestUtils.filter(nestedAttributes, "rArray").orElse(null));
    }

    static class PrimitiveTestClass {
        @Optional
        String name;
        int id;
        char c;
        short s;
        long l;
        byte b;
        double d;
        boolean bl;
        float f;
    }

    @SchemaDefinition(
            name = TestRecord.NAME,
            namespace = TestRecord.NAMESPACE,
            version = TestRecord.VERSION,
            description = TestRecord.DESCRIPTION,
            type = SchemaType.JSON,
            validation = SchemaValidationType.MATCHING,
            tags = {"foxtrot", "audit"}
    )
    static class TestRecord {
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
        Date date;
        LocalDateTime localDateTime;
    }

    static class TestObject {
        String name;
    }
}