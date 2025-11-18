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

package com.grookage.leia.dropwizard.bundle.permissions;

import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.ingestion.CreateSchemaRequest;
import com.grookage.leia.models.schema.ingestion.UpdateSchemaRequest;
import com.grookage.leia.models.user.SchemaUpdater;

import javax.ws.rs.core.HttpHeaders;

public interface PermissionValidator<U extends SchemaUpdater> {

	void validateSchemaCreation(final HttpHeaders headers,
	                            final U schemaUpdater,
	                            final CreateSchemaRequest schemaRequest);

	void validationSchemaModification(final HttpHeaders headers,
	                                  final U schemaUpdater,
	                                  final UpdateSchemaRequest schemaRequest);

	void validateSchemaApproval(final HttpHeaders headers,
	                            final U schemaUpdater,
	                            final SchemaKey schemaKey);

	void validateSchemaRejection(final HttpHeaders headers,
	                             final U schemaUpdater,
	                             final SchemaKey schemaKey);

}
