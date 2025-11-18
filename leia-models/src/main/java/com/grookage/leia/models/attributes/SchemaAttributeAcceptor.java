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

public interface SchemaAttributeAcceptor<T> {

	T accept(BooleanAttribute attribute);

	T accept(ByteAttribute attribute);

	T accept(CharacterAttribute attribute);

	T accept(DoubleAttribute attribute);

	T accept(EnumAttribute attribute);

	T accept(FloatAttribute attribute);

	T accept(IntegerAttribute attribute);

	T accept(LongAttribute attribute);

	T accept(ShortAttribute attribute);

	T accept(StringAttribute attribute);

	T accept(DateAttribute attribute);

	T accept(ArrayAttribute attribute);

	T accept(MapAttribute attribute);

	T accept(ObjectAttribute attribute);
}
