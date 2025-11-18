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

package com.grookage.leia.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grookage.leia.client.refresher.LeiaClientRefresher;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.validator.LeiaSchemaValidator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Set;

@SuperBuilder
@AllArgsConstructor
@Data
public abstract class AbstractSchemaClient implements LeiaSchemaClient {

	private final ObjectMapper mapper;
	private final LeiaClientRefresher refresher;
	private final LeiaSchemaValidator schemaValidator;

	public List<SchemaDetails> getSchemaDetails() {
		return refresher.getData();
	}

	public List<SchemaDetails> getSchemaDetails(final Set<SchemaKey> schemas) {
		if (null == refresher.getData()) {
			throw new IllegalStateException("The configuration object has returned null data. Something gone wrong with refresher");
		}
		final var schemaKeys = schemas.stream().map(SchemaKey::getReferenceId).toList();
		return refresher.getData().stream()
				.filter(each -> schemaKeys.contains(each.getReferenceId())).toList();
	}

	public boolean valid(SchemaKey schemaKey) {
		return schemaValidator.valid(schemaKey);
	}

	public abstract void start();
}

