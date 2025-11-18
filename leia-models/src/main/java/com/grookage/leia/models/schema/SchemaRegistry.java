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

package com.grookage.leia.models.schema;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
@NoArgsConstructor
public class SchemaRegistry {

	private final ConcurrentHashMap<SchemaKey, SchemaDetails> schemas = new ConcurrentHashMap<>();

	public void add(final SchemaDetails schemaDetails) {
		schemas.putIfAbsent(schemaDetails.getSchemaKey(), schemaDetails);
	}

	public Optional<SchemaDetails> getSchemaDetails(final SchemaKey schemaKey) {
		return Optional.ofNullable(schemas.get(schemaKey));
	}

	public Collection<SchemaDetails> getSchemaDetails() {
		return schemas.values();
	}
}
