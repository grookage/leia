/*
 * Copyright (c) 2025. Koushik R <rkoushik.14@gmail.com>.
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

package com.grookage.leia.models.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class SchemaUtilsTest {

	@Test
	@SneakyThrows
	void testSchemaUtils() {
		final var allSchemas = ResourceHelper.getResource("schema/allSchemas.json",
				new TypeReference<List<SchemaDetails>>() {
				});
		Assertions.assertFalse(allSchemas.isEmpty());
		Assertions.assertEquals(3, allSchemas.size());
		var schemaKey = SchemaKey.builder()
				.namespace("testNamespace")
				.schemaName("testSchema")
				.version("v1234")
				.orgId("testOrg")
				.tenantId("tenantId")
				.type("default")
				.build();
		var matchingSchema = SchemaUtils.getMatchingSchema(allSchemas, schemaKey).orElse(null);
		Assertions.assertNotNull(matchingSchema);
		Assertions.assertEquals("V1234", matchingSchema.getSchemaKey().getVersion());
	}
}
