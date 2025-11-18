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

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collection;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum DataType {
	INTEGER(Integer.class),

	STRING(String.class),

	FLOAT(Float.class),

	DOUBLE(Double.class),

	LONG(Long.class),

	SHORT(Short.class),

	BOOLEAN(Boolean.class),

	BYTES(Byte.class),

	CHARACTER(Character.class),

	DATE(Object.class),

	ARRAY(Collection.class),

	OBJECT(Object.class),

	MAP(Map.class),

	ENUM(Enum.class);

	private final Class<?> assignableClass;
}
