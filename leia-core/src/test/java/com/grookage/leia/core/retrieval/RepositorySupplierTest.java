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

package com.grookage.leia.core.retrieval;

import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.request.SearchRequest;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.repository.SchemaRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class RepositorySupplierTest {

	@Test
	@SneakyThrows
	void testRepositorySupplier() {
		final var schemaDetails = ResourceHelper
				.getResource("schema/schemaDetails.json", SchemaDetails.class);
		final var repository = Mockito.mock(SchemaRepository.class);
		final var supplier = new RepositorySupplier(() -> repository);
		Mockito.when(repository.getSchemas(SearchRequest.builder().build()))
				.thenReturn(List.of(schemaDetails));
		final var registry = supplier.get();
		Assertions.assertFalse(registry.getSchemas().isEmpty());
		final var schema = registry.getSchemaDetails(SchemaKey.builder()
				.namespace("testNamespace")
				.schemaName("testSchema")
				.version("V1234")
				.orgId("testOrg")
				.type("default")
				.tenantId("tenantId")
				.build()).orElse(null);
		Assertions.assertNotNull(schema);
	}
}
