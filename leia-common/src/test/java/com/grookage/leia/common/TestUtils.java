package com.grookage.leia.common;

import com.grookage.leia.common.utils.QualifierUtils;
import com.grookage.leia.models.attributes.ArrayAttribute;
import com.grookage.leia.models.attributes.BooleanAttribute;
import com.grookage.leia.models.attributes.ByteAttribute;
import com.grookage.leia.models.attributes.DoubleAttribute;
import com.grookage.leia.models.attributes.EnumAttribute;
import com.grookage.leia.models.attributes.FloatAttribute;
import com.grookage.leia.models.attributes.IntegerAttribute;
import com.grookage.leia.models.attributes.LongAttribute;
import com.grookage.leia.models.attributes.MapAttribute;
import com.grookage.leia.models.attributes.ObjectAttribute;
import com.grookage.leia.models.attributes.SchemaAttribute;
import com.grookage.leia.models.attributes.SchemaAttributeAcceptor;
import com.grookage.leia.models.attributes.StringAttribute;
import com.grookage.leia.models.qualifiers.QualifierInfo;
import com.grookage.leia.models.qualifiers.QualifierType;
import com.grookage.leia.models.qualifiers.ShortLivedQualifier;
import lombok.experimental.UtilityClass;
import org.junit.jupiter.api.Assertions;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@UtilityClass
public class TestUtils {
    public Optional<SchemaAttribute> filter(Set<SchemaAttribute> schemaAttributes,
                                            String name) {
        return schemaAttributes.stream()
                .filter(schemaAttribute -> schemaAttribute.getName().equals(name))
                .findFirst();
    }

    public void assertEquals(SchemaAttribute expected,
                             SchemaAttribute original) {
        if (Objects.isNull(expected) && Objects.isNull(original)) {
            return;
        }
        Assertions.assertEquals(expected.getType(), original.getType(), "Type mismatch");
        Assertions.assertEquals(expected.getName(), original.getName(), "Name mismatch");
        Assertions.assertEquals(expected.isOptional(), original.isOptional(), "Optionality mismatch");

        // Compare QualifierInfo
        assertEquals(expected.getQualifiers(), original.getQualifiers());

        // Accept the expected attribute type and perform specific validations
        expected.accept(new SchemaAttributeAcceptor<Void>() {
            @Override
            public Void accept(BooleanAttribute attribute) {
                Assertions.assertInstanceOf(BooleanAttribute.class, original, "Original is not BooleanAttribute");
                return null;
            }

            @Override
            public Void accept(ByteAttribute attribute) {
                Assertions.assertInstanceOf(ByteAttribute.class, original, "Original is not ByteAttribute");
                return null;
            }

            @Override
            public Void accept(DoubleAttribute attribute) {
                Assertions.assertInstanceOf(DoubleAttribute.class, original, "Original is not DoubleAttribute");
                return null;
            }

            @Override
            public Void accept(EnumAttribute attribute) {
                Assertions.assertInstanceOf(EnumAttribute.class, original, "Original is not EnumAttribute");
                EnumAttribute originalEnum = (EnumAttribute) original;
                Assertions.assertEquals(attribute.getValues(), originalEnum.getValues(), "Enum values mismatch");
                return null;
            }

            @Override
            public Void accept(FloatAttribute attribute) {
                Assertions.assertInstanceOf(FloatAttribute.class, original, "Original is not FloatAttribute");
                return null;
            }

            @Override
            public Void accept(IntegerAttribute attribute) {
                Assertions.assertInstanceOf(IntegerAttribute.class, original, "Original is not IntegerAttribute");
                return null;
            }

            @Override
            public Void accept(LongAttribute attribute) {
                Assertions.assertInstanceOf(LongAttribute.class, original, "Original is not LongAttribute");
                return null;
            }

            @Override
            public Void accept(StringAttribute attribute) {
                Assertions.assertInstanceOf(StringAttribute.class, original, "Original is not StringAttribute");
                return null;
            }

            @Override
            public Void accept(ArrayAttribute attribute) {
                Assertions.assertInstanceOf(ArrayAttribute.class, original, "Original is not ArrayAttribute");
                ArrayAttribute originalArray = (ArrayAttribute) original;
                assertEquals(attribute.getElementAttribute(), originalArray.getElementAttribute()); // Recursive comparison for elementAttribute
                return null;
            }

            @Override
            public Void accept(MapAttribute attribute) {
                Assertions.assertInstanceOf(MapAttribute.class, original, "Original is not MapAttribute");
                MapAttribute originalMap = (MapAttribute) original;
                assertEquals(attribute.getKeyAttribute(), originalMap.getKeyAttribute()); // Recursive comparison for key
                assertEquals(attribute.getValueAttribute(), originalMap.getValueAttribute()); // Recursive comparison for value
                return null;
            }

            @Override
            public Void accept(ObjectAttribute attribute) {
                Assertions.assertInstanceOf(ObjectAttribute.class, original, "Original is not ObjectAttribute");
                ObjectAttribute originalObject = (ObjectAttribute) original;

                if (Objects.isNull(attribute.getNestedAttributes()) && Objects.isNull(originalObject.getNestedAttributes())) {
                    return null;
                }

                // Recursive comparison of nested attributes
                Assertions.assertEquals(attribute.getNestedAttributes().size(), originalObject.getNestedAttributes().size(),
                        "Nested attributes size mismatch");
                Iterator<SchemaAttribute> expectedIterator = attribute.getNestedAttributes().iterator();
                Iterator<SchemaAttribute> originalIterator = originalObject.getNestedAttributes().iterator();

                while (expectedIterator.hasNext() && originalIterator.hasNext()) {
                    assertEquals(expectedIterator.next(), originalIterator.next());
                }
                return null;
            }
        });
    }

    public void assertEquals(Set<QualifierInfo> expected,
                             Set<QualifierInfo> original) {
        Assertions.assertNotNull(expected, "Expected qualifiers should not be null");
        Assertions.assertNotNull(original, "Actual qualifiers should not be null");
        Assertions.assertEquals(expected.size(), original.size(), "Qualifier sets size mismatch");

        expected.forEach(expectedQualifier -> {
            Optional<QualifierInfo> matchingQualifier = QualifierUtils.filter(original, expectedQualifier.getType());

            Assertions.assertTrue(matchingQualifier.isPresent(),
                    "Missing qualifier of type: " + expectedQualifier.getType());

            if (expectedQualifier.getType() == QualifierType.SHORT_LIVED) {
                Assertions.assertInstanceOf(ShortLivedQualifier.class, matchingQualifier.get(), "Actual SHORT_LIVED qualifier must be of type ShortLivedQualifier");

                ShortLivedQualifier expectedShortLived = (ShortLivedQualifier) expectedQualifier;
                ShortLivedQualifier actualShortLived = (ShortLivedQualifier) matchingQualifier.get();

                Assertions.assertEquals(expectedShortLived.getTtlSeconds(), actualShortLived.getTtlSeconds(),
                        "Mismatch in TTL seconds for SHORT_LIVED qualifier");
            }
        });
    }
}