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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Joiner;
import com.grookage.leia.models.SchemaConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Locale;

@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemaKey {
	@NotBlank
	private String orgId;
	@NotBlank
	private String namespace;
	@NotBlank
	private String tenantId;
	@NotBlank
	private String schemaName;
	@NotBlank
	private String version;
	@NotBlank
	private String type;

	@JsonIgnore
	public String getReferenceId() {
		return Joiner.on(SchemaConstants.KEY_DELIMITER).join(orgId,
				namespace,
				tenantId,
				schemaName,
				version
		).toUpperCase(Locale.ROOT);
	}

	@Override
	public int hashCode() {
		return this.getReferenceId().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		final var thatKey = (SchemaKey) obj;
		return (thatKey.getReferenceId().equals(this.getReferenceId()));
	}

}
