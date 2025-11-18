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

package com.grookage.leia.models.schema;

import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.attributes.DataType;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SchemaDetailsTest {
	@Test
	@SneakyThrows
	void testSchemaDetails() {
		final var schemaDetails = ResourceHelper
				.getResource("schema/schemaDetails.json", SchemaDetails.class);
		Assertions.assertNotNull(schemaDetails);
		final var schemaKey = schemaDetails.getSchemaKey();
		Assertions.assertNotNull(schemaKey);
		Assertions.assertEquals("testNamespace", schemaKey.getNamespace());
		Assertions.assertEquals("testSchema", schemaKey.getSchemaName());
		Assertions.assertEquals("V1234", schemaKey.getVersion());
		final var schemaAttributes = schemaDetails.getAttributes();
		Assertions.assertNotNull(schemaAttributes);
		Assertions.assertTrue(schemaAttributes.stream().anyMatch(each -> each.getType() == DataType.ARRAY));
		Assertions.assertTrue(schemaAttributes.stream().anyMatch(each -> each.getType() == DataType.ENUM));
		Assertions.assertTrue(schemaAttributes.stream().noneMatch(each -> each.getType() == DataType.INTEGER));
	}
}
