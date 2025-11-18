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

package com.grookage.leia.core.ingestion.processors;

import com.grookage.leia.core.exception.LeiaSchemaErrorCode;
import com.grookage.leia.core.ingestion.utils.SchemaUtils;
import com.grookage.leia.models.exception.LeiaException;
import com.grookage.leia.models.request.SearchRequest;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.engine.SchemaContext;
import com.grookage.leia.models.schema.engine.SchemaEvent;
import com.grookage.leia.models.schema.engine.SchemaState;
import com.grookage.leia.models.schema.ingestion.CreateSchemaRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.function.Supplier;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@Slf4j
public class CreateSchemaProcessor extends SchemaProcessor {

	@Override
	public SchemaEvent name() {
		return SchemaEvent.CREATE_SCHEMA;
	}

	@Override
	@SneakyThrows
	public void process(SchemaContext context) {
		final var createSchemaRequest = context.getContext(CreateSchemaRequest.class)
				.orElseThrow((Supplier<Throwable>) () -> LeiaException.error(LeiaSchemaErrorCode.VALUE_NOT_FOUND));
		validateSchemaCreation(createSchemaRequest.getSchemaKey());
		final var schemaDetails = SchemaUtils.toSchemaDetails(createSchemaRequest);
		addHistory(context, schemaDetails);
		getRepositorySupplier().get().create(schemaDetails);
		context.addContext(SchemaDetails.class.getSimpleName(), schemaDetails);
	}

	private void validateSchemaCreation(SchemaKey schemaKey) {
		final var schemas = getRepositorySupplier().get()
				.getSchemas(SearchRequest.builder()
						.orgIds(Set.of(schemaKey.getOrgId()))
						.namespaces(Set.of(schemaKey.getNamespace()))
						.tenants(Set.of(schemaKey.getTenantId()))
						.schemaNames(Set.of(schemaKey.getSchemaName()))
						.build());

		if (schemas == null || schemas.isEmpty()) {
			return;
		}
		// Check if there are is any existing record with the same version
		final var recordExists = schemas.stream()
				.anyMatch(schemaDetails -> schemaDetails.getSchemaKey().equals(schemaKey));
		if (recordExists) {
			log.error("Stored Schema already present against schemaKey:{}", schemaKey.getReferenceId());
			throw LeiaException.error(LeiaSchemaErrorCode.SCHEMA_ALREADY_EXISTS);
		}
		// Check if there are any created records
		final var createdRecordExists = schemas.stream()
				.anyMatch(schemaDetails -> schemaDetails.getSchemaState() == SchemaState.CREATED);
		if (createdRecordExists) {
			log.error("There are already stored schemas in created state present with schemaName {}. Please try updating them instead",
					schemaKey.getSchemaName());
			throw LeiaException.error(LeiaSchemaErrorCode.SCHEMA_ALREADY_EXISTS);
		}
	}
}
