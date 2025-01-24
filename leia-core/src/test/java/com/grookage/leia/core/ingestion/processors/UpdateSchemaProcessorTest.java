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

package com.grookage.leia.core.ingestion.processors;

import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.exception.LeiaException;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.engine.SchemaContext;
import com.grookage.leia.models.schema.ingestion.UpdateSchemaRequest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;


class UpdateSchemaProcessorTest extends SchemaProcessorTest {

    @Test
    @SneakyThrows
    void testUpdateSchemas() {
        final var schemaContext = new SchemaContext();
        final var schemaProcessor = getSchemaProcessor();
        final var schemaRequest = ResourceHelper.getResource(
                "schema/updateSchemaRequest.json",
                UpdateSchemaRequest.class
        );
        schemaContext.addContext("USER", "testUser");
        schemaContext.addContext("EMAIL", "testEmail");
        schemaContext.addContext("USER_ID", "testUserId");
        schemaContext.addContext(UpdateSchemaRequest.class.getSimpleName(), schemaRequest);
        final var schemaDetails = ResourceHelper
                .getResource("schema/schemaDetails.json", SchemaDetails.class);
        Mockito.when(getRepositorySupplier().get().get(Mockito.any(SchemaKey.class))).thenReturn(Optional.of(schemaDetails));
        schemaProcessor.process(schemaContext);
        Mockito.verify(getRepositorySupplier().get(), Mockito.times(1)).update(Mockito.any(SchemaDetails.class));
    }

    @Test
    @SneakyThrows
    void testUpdateSchemasNoDetails() {
        final var schemaContext = new SchemaContext();
        final var schemaProcessor = getSchemaProcessor();
        final var schemaRequest = ResourceHelper.getResource(
                "schema/updateSchemaRequest.json",
                UpdateSchemaRequest.class
        );
        schemaContext.addContext("USER", "testUser");
        schemaContext.addContext("EMAIL", "testEmail");
        schemaContext.addContext("USER_ID", "testUserId");
        schemaContext.addContext(UpdateSchemaRequest.class.getSimpleName(), schemaRequest);
        Mockito.when(getRepositorySupplier().get().get(Mockito.any(SchemaKey.class))).thenReturn(Optional.empty());
        Assertions.assertThrows(LeiaException.class, () -> schemaProcessor.process(schemaContext));
    }

    @Override
    SchemaProcessor getSchemaProcessor() {
        return UpdateSchemaProcessor.builder()
                .versionSupplier(getGenerator())
                .repositorySupplier(getRepositorySupplier())
                .build();
    }
}
