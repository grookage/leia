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

import com.grookage.korg.exceptions.KorgErrorCode;
import com.grookage.korg.exceptions.KorgException;
import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.SchemaRegistry;
import com.grookage.leia.models.utils.LeiaUtils;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


class RepositoryRefresherTest {

	@Test
	@SneakyThrows
	void testRepositoryRefresher() {
		final var schemaDetails = ResourceHelper
				.getResource("schema/schemaDetails.json", SchemaDetails.class);
		final var supplier = Mockito.mock(RepositorySupplier.class);
		final var registry = SchemaRegistry.builder()
				.build();
		Mockito.when(supplier.get()).thenReturn(registry);
		final var refresher = new RepositoryRefresher(supplier, 5, true);
		refresher.start();
		Assertions.assertTrue(refresher.getData().getSchemas().isEmpty());
		registry.add(schemaDetails);
		LeiaUtils.sleepFor(6);
		final var schemas = refresher.getData();
		final var schema = schemas.getSchemaDetails(SchemaKey.builder()
				.namespace("testNamespace")
				.schemaName("testSchema")
				.version("V1234")
				.orgId("testOrg")
				.type("default")
				.tenantId("tenantId")
				.build()).orElse(null);
		Assertions.assertNotNull(schema);
	}

	@Test
	void testRepositoryRefresher_whenSupplierReturnNullAtStart() {
		final var supplier = Mockito.mock(RepositorySupplier.class);
		Mockito.doReturn(null).when(supplier).get();
		val repositoryRefresher = new RepositoryRefresher(supplier, 5, true);
		final var exception = Assertions.assertThrows(KorgException.class,
				repositoryRefresher::start);
		Assertions.assertNotNull(exception);
		Assertions.assertEquals(KorgErrorCode.REFRESH_FAILED.getStatus(), exception.getStatus());
	}

	@Test
	void testRepositoryRefresher_whenSupplierThrowExceptionAtStart() {
		final var supplier = Mockito.mock(RepositorySupplier.class);
		Mockito.doThrow(KorgException.error(KorgErrorCode.REFRESH_FAILED)).when(supplier).get();
		val repositoryRefresher = new RepositoryRefresher(supplier, 5, true);
		final var exception = Assertions.assertThrows(KorgException.class,
				repositoryRefresher::start);
		Assertions.assertNotNull(exception);
		Assertions.assertEquals(KorgErrorCode.REFRESH_FAILED.getStatus(), exception.getStatus());
	}
}
