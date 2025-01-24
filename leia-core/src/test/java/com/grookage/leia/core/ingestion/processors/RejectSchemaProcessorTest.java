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
import com.grookage.leia.models.schema.engine.SchemaState;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

class RejectSchemaProcessorTest extends SchemaProcessorTest {
    @Override
    SchemaProcessor getSchemaProcessor() {
        return RejectSchemaProcessor.builder()
                .repositorySupplier(getRepositorySupplier())
                .versionSupplier(getGenerator())
                .build();
    }

    @Test
    @SneakyThrows
    void testSchemaRejectionsInvalid() {
        final var schemaContext = new SchemaContext();
        final var schemaProcessor = getSchemaProcessor();
        Assertions.assertThrows(LeiaException.class, () -> schemaProcessor.process(schemaContext));
        Mockito.when(getRepositorySupplier().get().get(Mockito.any(SchemaKey.class))).thenReturn(Optional.empty());
        Assertions.assertThrows(LeiaException.class, () -> schemaProcessor.process(schemaContext));
    }

    @Test
    @SneakyThrows
    void testSchemaRejectionsApprovedState() {
        final var schemaKey = ResourceHelper.getResource(
                "schema/schemaKey.json",
                SchemaKey.class
        );
        final var schemaDetails = ResourceHelper
                .getResource("schema/schemaDetails.json", SchemaDetails.class);
        schemaDetails.setSchemaState(SchemaState.APPROVED);
        final var schemaContext = new SchemaContext();
        schemaContext.addContext("USER", "testUser");
        schemaContext.addContext("EMAIL", "testEmail");
        schemaContext.addContext("USER_ID", "testUserId");
        schemaContext.addContext(SchemaKey.class.getSimpleName(), schemaKey);
        final var schemaProcessor = getSchemaProcessor();
        Mockito.when(getRepositorySupplier().get().get(Mockito.any(SchemaKey.class))).thenReturn(Optional.of(schemaDetails));
        schemaProcessor.process(schemaContext);
        Assertions.assertEquals(SchemaState.REJECTED, schemaDetails.getSchemaState());
        Mockito.verify(getRepositorySupplier().get(), Mockito.times(1)).update(Mockito.any(SchemaDetails.class));
    }

    @Test
    @SneakyThrows
    void testSchemaRejections() {
        final var schemaKey = ResourceHelper.getResource(
                "schema/schemaKey.json",
                SchemaKey.class
        );
        final var schemaDetails = ResourceHelper
                .getResource("schema/schemaDetails.json", SchemaDetails.class);
        schemaDetails.setSchemaState(SchemaState.CREATED);
        final var schemaContext = new SchemaContext();
        schemaContext.addContext(SchemaKey.class.getSimpleName(), schemaKey);
        schemaContext.addContext("USER", "testUser");
        schemaContext.addContext("EMAIL", "testEmail");
        schemaContext.addContext("USER_ID", "testUserId");
        final var schemaProcessor = getSchemaProcessor();
        Mockito.when(getRepositorySupplier().get().get(Mockito.any(SchemaKey.class))).thenReturn(Optional.of(schemaDetails));
        schemaProcessor.process(schemaContext);
        Assertions.assertEquals(SchemaState.REJECTED, schemaDetails.getSchemaState());
        Mockito.verify(getRepositorySupplier().get(), Mockito.times(1)).update(Mockito.any(SchemaDetails.class));
    }

}
