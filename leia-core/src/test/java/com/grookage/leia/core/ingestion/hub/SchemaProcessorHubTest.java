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

package com.grookage.leia.core.ingestion.hub;

import com.grookage.leia.models.schema.engine.SchemaEvent;
import com.grookage.leia.repository.SchemaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SchemaProcessorHubTest {

	@Test
	void testSchemaProcessorHub() {
		final var schemaRepository = Mockito.mock(SchemaRepository.class);

		final var hub = SchemaProcessorHub.of()
				.withRepositoryResolver(() -> schemaRepository)
				.build();
		Assertions.assertNotNull(hub.getProcessor(SchemaEvent.CREATE_SCHEMA).orElse(null));
		Assertions.assertThrows(NullPointerException.class, () -> SchemaProcessorHub.of()
				.build());
	}
}
