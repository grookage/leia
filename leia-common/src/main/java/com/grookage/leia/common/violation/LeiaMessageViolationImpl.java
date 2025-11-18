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

package com.grookage.leia.common.violation;

import com.google.common.base.Strings;
import com.grookage.leia.models.schema.SchemaKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeiaMessageViolationImpl implements LeiaMessageViolation {
	private String message;
	private String fieldPath;
	private SchemaKey schemaKey;

	@Override
	public String message() {
		return message;
	}

	@Override
	public String fieldPath() {
		return fieldPath;
	}

	@Override
	public SchemaKey schemaKey() {
		return schemaKey;
	}

	@Override
	public String toString() {
		if (Strings.isNullOrEmpty(fieldPath)) {
			return String.format("[LeiaMessageViolation] schema = %s, message = %s", schemaKey, message);
		} else if (schemaKey == null) {
			return String.format("[LeiaMessageViolation] fieldPath = %s, message = %s", fieldPath, message);
		}
		return String.format("[LeiaMessageViolation] schema = %s, fieldPath = %s, message = %s",
				schemaKey, fieldPath, message);
	}
}

