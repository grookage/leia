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

package com.grookage.leia.models.attributes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Preconditions;
import com.grookage.leia.models.qualifiers.QualifierInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class MapAttribute extends SchemaAttribute {

	private SchemaAttribute keyAttribute;
	private SchemaAttribute valueAttribute;

	public MapAttribute(final String name,
	                    final boolean optional,
	                    final Set<QualifierInfo> qualifiers,
	                    SchemaAttribute keyAttribute,
	                    SchemaAttribute valueAttribute) {
		super(DataType.MAP, name, optional, qualifiers);
		Preconditions.checkArgument(keyAttribute == null || valueAttribute != null);
		this.keyAttribute = keyAttribute;
		this.valueAttribute = valueAttribute;
	}

	@Override
	public <T> T accept(SchemaAttributeAcceptor<T> attributeAcceptor) {
		return attributeAcceptor.accept(this);
	}
}

