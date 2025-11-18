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

package com.grookage.leia.models.schema.ingestion;

import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.attributes.DataType;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.SchemaType;
import com.grookage.leia.models.schema.SchemaValidationType;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IngestionRequestTest {

	@Test
	@SneakyThrows
	void testIngestionRequests() {
		final var createSchemaRequest = ResourceHelper.getResource(
				"schema/createSchemaRequest.json",
				CreateSchemaRequest.class
		);
		Assertions.assertNotNull(createSchemaRequest);
		var schemaAttributes = createSchemaRequest.getAttributes();
		Assertions.assertNotNull(schemaAttributes);
		Assertions.assertTrue(schemaAttributes.stream().anyMatch(each -> each.getType() == DataType.ARRAY));
		Assertions.assertTrue(schemaAttributes.stream().noneMatch(each -> each.getType() == DataType.ENUM));
		Assertions.assertTrue(schemaAttributes.stream().noneMatch(each -> each.getType() == DataType.INTEGER));
		Assertions.assertEquals("testNamespace", createSchemaRequest.getSchemaKey().getNamespace());
		Assertions.assertEquals("testSchema", createSchemaRequest.getSchemaKey().getSchemaName());
		Assertions.assertSame(SchemaValidationType.MATCHING, createSchemaRequest.getValidationType());
		Assertions.assertSame(SchemaType.JSON, createSchemaRequest.getSchemaType());

		final var updateSchemaRequest = ResourceHelper.getResource(
				"schema/updateSchemaRequest.json",
				UpdateSchemaRequest.class
		);
		Assertions.assertNotNull(updateSchemaRequest);
		schemaAttributes = updateSchemaRequest.getAttributes();
		Assertions.assertNotNull(schemaAttributes);
		Assertions.assertTrue(schemaAttributes.stream().anyMatch(each -> each.getType() == DataType.ARRAY));
		Assertions.assertTrue(schemaAttributes.stream().anyMatch(each -> each.getType() == DataType.ENUM));
		Assertions.assertTrue(schemaAttributes.stream().noneMatch(each -> each.getType() == DataType.INTEGER));
		Assertions.assertEquals("testNamespace", updateSchemaRequest.getSchemaKey().getNamespace());
		Assertions.assertEquals("testSchema", updateSchemaRequest.getSchemaKey().getSchemaName());
		Assertions.assertSame(SchemaValidationType.STRICT, updateSchemaRequest.getValidationType());
		Assertions.assertSame(SchemaType.JSON, updateSchemaRequest.getSchemaType());

		final var schemaKey = ResourceHelper.getResource(
				"schema/schemaKey.json",
				SchemaKey.class
		);
		Assertions.assertNotNull(schemaKey);
		Assertions.assertEquals("testNamespace", schemaKey.getNamespace());
		Assertions.assertEquals("testSchema", schemaKey.getSchemaName());
		Assertions.assertEquals("V1234", schemaKey.getVersion());
	}
}
