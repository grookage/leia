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

package com.grookage.leia.core.ingestion;

import com.grookage.leia.core.ingestion.hub.SchemaProcessorHub;
import com.grookage.leia.core.ingestion.processors.SchemaProcessor;
import com.grookage.leia.core.stubs.StubbedSchemaUpdater;
import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.exception.LeiaException;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.engine.SchemaEvent;
import com.grookage.leia.models.schema.ingestion.CreateSchemaRequest;
import com.grookage.leia.models.schema.ingestion.UpdateSchemaRequest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

class SchemaIngestorTest {

    private static final StubbedSchemaUpdater schemaUpdater = new StubbedSchemaUpdater();
    private static SchemaProcessorHub schemaProcessorHub;
    private static SchemaIngestor<StubbedSchemaUpdater> schemaIngestor;

    @BeforeEach
    void setup() {
        schemaProcessorHub = Mockito.mock(SchemaProcessorHub.class);
        schemaIngestor = new SchemaIngestor<StubbedSchemaUpdater>()
                .withProcessorHub(schemaProcessorHub).build();

    }

    @Test
    @SneakyThrows
    void testCreateSchemaNoProcessor() {
        Mockito.when(schemaProcessorHub.getProcessor(Mockito.any(SchemaEvent.class)))
                .thenReturn(Optional.empty());
        final var createSchemaRequest = ResourceHelper.getResource(
                "schema/createSchemaRequest.json",
                CreateSchemaRequest.class
        );
        Assertions.assertNotNull(createSchemaRequest);
        Assertions.assertThrows(LeiaException.class, () -> schemaIngestor.add(schemaUpdater, createSchemaRequest));
    }

    @Test
    @SneakyThrows
    void testCreateSchema() {
        final var schemaProcessor = Mockito.mock(SchemaProcessor.class);
        Mockito.when(schemaProcessorHub.getProcessor(Mockito.any(SchemaEvent.class)))
                .thenReturn(Optional.of(schemaProcessor));
        final var createSchemaRequest = ResourceHelper.getResource(
                "schema/createSchemaRequest.json",
                CreateSchemaRequest.class
        );
        Assertions.assertNotNull(createSchemaRequest);
        schemaIngestor.add(schemaUpdater, createSchemaRequest);
        Mockito.verify(schemaProcessor, Mockito.times(1)).process(Mockito.any());
    }

    @Test
    @SneakyThrows
    void testUpdateSchema() {
        final var schemaProcessor = Mockito.mock(SchemaProcessor.class);
        Mockito.when(schemaProcessorHub.getProcessor(Mockito.any(SchemaEvent.class)))
                .thenReturn(Optional.of(schemaProcessor));
        final var updateSchemaRequest = ResourceHelper.getResource(
                "schema/updateSchemaRequest.json",
                UpdateSchemaRequest.class
        );
        Assertions.assertNotNull(updateSchemaRequest);
        schemaIngestor.update(schemaUpdater, updateSchemaRequest);
        Mockito.verify(schemaProcessor, Mockito.times(1)).process(Mockito.any());
    }

    @Test
    @SneakyThrows
    void testApproveSchema() {
        final var schemaProcessor = Mockito.mock(SchemaProcessor.class);
        Mockito.when(schemaProcessorHub.getProcessor(Mockito.any(SchemaEvent.class)))
                .thenReturn(Optional.of(schemaProcessor));
        final var schemaKey = ResourceHelper.getResource(
                "schema/schemaKey.json",
                SchemaKey.class
        );
        Assertions.assertNotNull(schemaKey);
        schemaIngestor.approve(schemaUpdater, schemaKey);
        Mockito.verify(schemaProcessor, Mockito.times(1)).process(Mockito.any());
    }

    @Test
    @SneakyThrows
    void testRejectSchema() {
        final var schemaProcessor = Mockito.mock(SchemaProcessor.class);
        Mockito.when(schemaProcessorHub.getProcessor(Mockito.any(SchemaEvent.class)))
                .thenReturn(Optional.of(schemaProcessor));
        final var schemaKey = ResourceHelper.getResource(
                "schema/schemaKey.json",
                SchemaKey.class
        );
        Assertions.assertNotNull(schemaKey);
        schemaIngestor.reject(schemaUpdater, schemaKey);
        Mockito.verify(schemaProcessor, Mockito.times(1)).process(Mockito.any());
    }
}
