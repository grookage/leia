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

package com.grookage.leia.core.ingestion.utils;

import com.grookage.leia.core.exception.LeiaSchemaErrorCode;
import com.grookage.leia.models.exception.LeiaException;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.engine.SchemaContext;
import com.grookage.leia.models.schema.engine.SchemaEvent;
import com.grookage.leia.models.schema.engine.SchemaState;
import com.grookage.leia.models.schema.ingestion.CreateSchemaRequest;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class SchemaUtils {
	private static final boolean LOCAL_ENV = Boolean.parseBoolean(System.getProperty("localEnv", "false"));

	public SchemaDetails toSchemaDetails(final CreateSchemaRequest createSchemaRequest) {
		return SchemaDetails.builder()
				.schemaKey(createSchemaRequest.getSchemaKey())
				.schemaState(SchemaState.CREATED)
				.schemaType(createSchemaRequest.getSchemaType())
				.description(createSchemaRequest.getDescription())
				.attributes(createSchemaRequest.getAttributes())
				.validationType(createSchemaRequest.getValidationType())
				.transformationTargets(createSchemaRequest.getTransformationTargets())
				.tags(createSchemaRequest.getTags())
				.build();
	}

	public void validateSchemaApproval(SchemaContext context, SchemaDetails storedSchema) {
		if (LOCAL_ENV) {
			// Skip approver validation in local environment
			return;
		}
		final var schemaKey = storedSchema.getSchemaKey();
		final String currentUserId = ContextUtils.getUserId(context);

		// Check if current user has previously created or updated this schema
		final var isCreatorOrUpdater = storedSchema.getHistories().stream()
				.filter(history -> history.getSchemaEvent() == SchemaEvent.CREATE_SCHEMA ||
						history.getSchemaEvent() == SchemaEvent.UPDATE_SCHEMA)
				.anyMatch(history -> history.getConfigUpdaterId().equalsIgnoreCase(currentUserId));

		if (isCreatorOrUpdater) {
			log.error("User '{}' cannot approve schema '{}' because they previously created or updated it",
					ContextUtils.getUser(context), schemaKey.getReferenceId());
			throw LeiaException.error(LeiaSchemaErrorCode.SCHEMA_APPROVAL_UNAUTHORIZED);
		}
	}
}
