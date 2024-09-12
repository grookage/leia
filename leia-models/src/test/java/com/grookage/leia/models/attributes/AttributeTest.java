/*
 * Copyright (c) 2024. Koushik R <rkoushik.14@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.grookage.leia.models.attributes;

import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.qualifiers.QualifierType;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AttributeTest {

    @Test
    @SneakyThrows
    void testAttributeStructures() {
        var attribute = ResourceHelper.getResource("attributes/attribute.json", SchemaAttribute.class);
        Assertions.assertNotNull(attribute);
        Assertions.assertEquals("testAttribute", attribute.getName());
        Assertions.assertSame(DataType.ARRAY, attribute.getType());
        attribute = ResourceHelper.getResource("attributes/enumAttribute.json", SchemaAttribute.class);
        Assertions.assertNotNull(attribute);
        Assertions.assertEquals("testAttribute", attribute.getName());
        Assertions.assertSame(DataType.ENUM, attribute.getType());
        Assertions.assertTrue(((EnumAttribute) attribute).getValues().contains("TEST_ENUM"));
        attribute = ResourceHelper.getResource("attributes/attributeWithQualifier.json", SchemaAttribute.class);
        Assertions.assertNotNull(attribute);
        Assertions.assertEquals("testAttribute", attribute.getName());
        Assertions.assertSame(DataType.ARRAY, attribute.getType());
        Assertions.assertTrue(attribute.getQualifierInfo() != null && attribute.getQualifierInfo().getType() == QualifierType.PII);
    }
}