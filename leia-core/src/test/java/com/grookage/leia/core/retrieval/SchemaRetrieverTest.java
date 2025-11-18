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
import com.grookage.leia.models.request.LeiaRequestContext;
import com.grookage.leia.models.request.SearchRequest;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.engine.SchemaState;
import com.grookage.leia.models.utils.LeiaUtils;
import com.grookage.leia.repository.SchemaRepository;
import com.grookage.leia.repository.config.CacheConfig;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class SchemaRetrieverTest {

	private static SchemaRepository repository;

	@BeforeEach
	void setup() {
		repository = Mockito.mock(SchemaRepository.class);
	}

	CacheConfig getCacheConfig() {
		return CacheConfig.builder()
				.enabled(true)
				.refreshCacheSeconds(1)
				.build();
	}

	@Test
	@SneakyThrows
	void testGetSchemaDetailsCacheDisabled() {
		final var retriever = new SchemaRetriever(() -> repository, null);
		Assertions.assertNull(retriever.getRefresher());
		final var schemaDetails = ResourceHelper
				.getResource("schema/schemaDetails.json", SchemaDetails.class);
		final var schemaKey = ResourceHelper
				.getResource("schema/schemaKey.json", SchemaKey.class);
		Mockito.when(repository.get(schemaKey))
				.thenReturn(Optional.of(schemaDetails));
		final var schemas = retriever.getSchemaDetails(LeiaRequestContext.builder().build(), schemaKey).orElse(null);
		Assertions.assertNotNull(schemas);
	}

	@Test
	@SneakyThrows
	void testGetSchemaDetailsCacheEnabled() {
		final var cacheConfig = getCacheConfig();
		final var retriever = new SchemaRetriever(() -> repository, cacheConfig);
		final var schemaDetails = ResourceHelper
				.getResource("schema/schemaDetails.json", SchemaDetails.class);
		final var schemaKey = ResourceHelper
				.getResource("schema/schemaKey.json", SchemaKey.class);
		Mockito.when(repository.get(schemaKey))
				.thenReturn(Optional.of(schemaDetails));
		var schemas = retriever.getSchemaDetails(LeiaRequestContext.builder().ignoreCache(false).build(), schemaKey).orElse(null);
		Assertions.assertNull(schemas);
		Mockito.when(repository.getSchemas(SearchRequest.builder().build()))
				.thenReturn(List.of(schemaDetails));
		LeiaUtils.sleepUntil(4);
		schemas = retriever.getSchemaDetails(LeiaRequestContext.builder().ignoreCache(false).build(), schemaKey).orElse(null);
		Assertions.assertNotNull(schemas);
	}

	@Test
	@SneakyThrows
	void testGetCurrentSchemasNoCache() {
		final var retriever = new SchemaRetriever(() -> repository, null);
		Assertions.assertNull(retriever.getRefresher());
		final var schemaDetails = ResourceHelper
				.getResource("schema/schemaDetails.json", SchemaDetails.class);
		final var searchRequest = SearchRequest.builder()
				.schemaNames(Set.of("testNamespace"))
				.states(Set.of(SchemaState.APPROVED))
				.build();
		Mockito.when(repository.getSchemas(searchRequest))
				.thenReturn(List.of());
		var schemas = retriever.getSchemaDetails(LeiaRequestContext.builder().build(), searchRequest);
		Assertions.assertTrue(schemas.isEmpty());
		Mockito.when(repository.getSchemas(searchRequest))
				.thenReturn(List.of(schemaDetails));
		schemas = retriever.getSchemaDetails(LeiaRequestContext.builder().build(), searchRequest);
		Assertions.assertFalse(schemas.isEmpty());
	}

	@Test
	@SneakyThrows
	void testGetCurrentSchemasWithCache() {
		final var retriever = new SchemaRetriever(() -> repository, getCacheConfig());
		Assertions.assertNotNull(retriever.getRefresher());
		final var schemaDetails = ResourceHelper
				.getResource("schema/schemaDetails.json", SchemaDetails.class);
		Mockito.when(repository.getSchemas(SearchRequest.builder().build()))
				.thenReturn(List.of());
		final var searchRequest = SearchRequest.builder()
				.schemaNames(Set.of("testNamespace"))
				.build();
		final var requestContext = LeiaRequestContext.builder().ignoreCache(false).build();
		var schemas = retriever.getSchemaDetails(requestContext, searchRequest);
		Assertions.assertTrue(schemas.isEmpty());
		Mockito.when(repository.getSchemas(SearchRequest.builder().build()))
				.thenReturn(List.of(schemaDetails));
		LeiaUtils.sleepUntil(4);
		schemas = retriever.getSchemaDetails(requestContext, searchRequest);
		Assertions.assertTrue(schemas.isEmpty());
		schemaDetails.setSchemaState(SchemaState.APPROVED);
		Mockito.when(repository.getSchemas(SearchRequest.builder().build()))
				.thenReturn(List.of(schemaDetails));
		LeiaUtils.sleepUntil(4);
		schemas = retriever.getSchemaDetails(requestContext, searchRequest);
		Assertions.assertTrue(schemas.isEmpty());
	}

	@Test
	@SneakyThrows
	void testGetAllSchemasNoCache() {
		final var retriever = new SchemaRetriever(() -> repository, null);
		Assertions.assertNull(retriever.getRefresher());
		final var schemaDetails = ResourceHelper
				.getResource("schema/schemaDetails.json", SchemaDetails.class);
		var searchRequest = SearchRequest.builder()
				.schemaNames(Set.of("testNamespace"))
				.states(Set.of(SchemaState.APPROVED))
				.build();
		Mockito.when(repository.getSchemas(searchRequest))
				.thenReturn(List.of());

		final var requestContext = LeiaRequestContext.builder().build();
		var schemas = retriever.getSchemaDetails(requestContext, searchRequest);
		Assertions.assertTrue(schemas.isEmpty());
		searchRequest =
				SearchRequest.builder()
						.schemaNames(Set.of("testNamespace"))
						.states(Arrays.stream(SchemaState.values()).collect(Collectors.toSet()))
						.build();
		Mockito.when(repository.getSchemas(searchRequest)).thenReturn(List.of());
		searchRequest = SearchRequest.builder()
				.schemaNames(Set.of("testNamespace"))
				.build();
		schemas = retriever.getSchemaDetails(requestContext, searchRequest);
		Assertions.assertTrue(schemas.isEmpty());
	}

	@Test
	@SneakyThrows
	void testGetAllSchemasWithCache() {
		final var retriever = new SchemaRetriever(() -> repository, getCacheConfig());
		Assertions.assertNotNull(retriever.getRefresher());
		final var schemaDetails = ResourceHelper
				.getResource("schema/schemaDetails.json", SchemaDetails.class);
		var searchRequest = SearchRequest.builder()
				.build();
		Mockito.when(repository.getSchemas(searchRequest))
				.thenReturn(List.of());
		searchRequest = SearchRequest.builder()
				.schemaNames(Set.of("testNamespace"))
				.build();
		final var requestContext = LeiaRequestContext.builder().ignoreCache(false).build();
		var schemas = retriever.getSchemaDetails(requestContext, searchRequest);
		Assertions.assertTrue(schemas.isEmpty());
		searchRequest = SearchRequest.builder()
				.build();
		Mockito.when(repository.getSchemas(searchRequest))
				.thenReturn(List.of());
		LeiaUtils.sleepUntil(4);
		schemas = retriever.getSchemaDetails(requestContext, searchRequest);
		Assertions.assertTrue(schemas.isEmpty());
		Mockito.when(repository.getSchemas(Mockito.any(SearchRequest.class)))
				.thenReturn(List.of(schemaDetails));
		LeiaUtils.sleepUntil(4);
		schemas = retriever.getSchemaDetails(requestContext, searchRequest);
		Assertions.assertFalse(schemas.isEmpty());
	}
}
