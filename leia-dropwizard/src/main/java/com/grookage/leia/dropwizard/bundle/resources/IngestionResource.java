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

package com.grookage.leia.dropwizard.bundle.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.grookage.leia.core.ingestion.SchemaIngestor;
import com.grookage.leia.dropwizard.bundle.permissions.PermissionValidator;
import com.grookage.leia.dropwizard.bundle.resolvers.SchemaUpdaterResolver;
import com.grookage.leia.models.GenericResponse;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.ingestion.CreateSchemaRequest;
import com.grookage.leia.models.schema.ingestion.UpdateSchemaRequest;
import com.grookage.leia.models.user.SchemaUpdater;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.function.Supplier;

@Singleton
@Getter
@Setter
@Path("/v1/ingest")
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AllArgsConstructor
@PermitAll
public class IngestionResource<U extends SchemaUpdater> {

	private final SchemaIngestor<U> schemaIngestor;
	private final Supplier<SchemaUpdaterResolver<U>> updaterResolver;
	private final Supplier<PermissionValidator<U>> permissionValidatorSupplier;

	@PUT
	@Timed
	@ExceptionMetered
	@Path("/add")
	public GenericResponse<SchemaDetails> addSchema(@Context HttpHeaders headers,
	                                                @Valid final CreateSchemaRequest schemaRequest) {
		final var updater = updaterResolver.get().resolve(headers);
		permissionValidatorSupplier.get().validateSchemaCreation(headers, updater, schemaRequest);
		return GenericResponse.<SchemaDetails>builder()
				.success(true)
				.data(schemaIngestor.add(updater, schemaRequest))
				.build();
	}

	@POST
	@Timed
	@ExceptionMetered
	@Path("/update")
	public GenericResponse<SchemaDetails> updateSchema(@Context HttpHeaders headers,
	                                                   @Valid final UpdateSchemaRequest schemaRequest) {
		final var updater = updaterResolver.get().resolve(headers);
		permissionValidatorSupplier.get().validationSchemaModification(headers, updater, schemaRequest);
		return GenericResponse.<SchemaDetails>builder()
				.success(true)
				.data(schemaIngestor.update(updater, schemaRequest))
				.build();
	}

	@POST
	@Timed
	@ExceptionMetered
	@Path("/approve")
	public GenericResponse<SchemaDetails> approveSchema(@Context HttpHeaders headers,
	                                                    @Valid final SchemaKey schemaKey) {
		final var updater = updaterResolver.get().resolve(headers);
		permissionValidatorSupplier.get().validateSchemaApproval(headers, updater, schemaKey);
		return GenericResponse.<SchemaDetails>builder()
				.success(true)
				.data(schemaIngestor.approve(updater, schemaKey))
				.build();
	}

	@POST
	@Timed
	@ExceptionMetered
	@Path("/reject")
	public GenericResponse<SchemaDetails> rejectSchema(@Context HttpHeaders headers,
	                                                   @Valid final SchemaKey schemaKey) {
		final var updater = updaterResolver.get().resolve(headers);
		permissionValidatorSupplier.get().validateSchemaRejection(headers, updater, schemaKey);
		return GenericResponse.<SchemaDetails>builder()
				.success(true)
				.data(schemaIngestor.reject(updater, schemaKey))
				.build();
	}
}
