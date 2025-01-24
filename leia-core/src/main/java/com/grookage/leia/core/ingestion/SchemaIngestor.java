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

import com.google.common.base.Preconditions;
import com.grookage.leia.core.exception.LeiaSchemaErrorCode;
import com.grookage.leia.core.ingestion.hub.SchemaProcessorHub;
import com.grookage.leia.core.ingestion.utils.ContextUtils;
import com.grookage.leia.models.exception.LeiaException;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.engine.SchemaContext;
import com.grookage.leia.models.schema.engine.SchemaEvent;
import com.grookage.leia.models.schema.ingestion.CreateSchemaRequest;
import com.grookage.leia.models.schema.ingestion.UpdateSchemaRequest;
import com.grookage.leia.models.user.SchemaUpdater;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
@NoArgsConstructor
public class SchemaIngestor<U extends SchemaUpdater> {

    private SchemaProcessorHub processorHub;

    public SchemaIngestor<U> withProcessorHub(final SchemaProcessorHub schemaProcessorHub) {
        Preconditions.checkNotNull(schemaProcessorHub, "Processor hub can't be null");
        this.processorHub = schemaProcessorHub;
        return this;
    }

    public SchemaIngestor<U> build() {
        log.info("Building Ingestion Service");
        return this;
    }

    @SneakyThrows
    public SchemaDetails add(U schemaUpdater, CreateSchemaRequest createSchemaRequest) {
        final var schemaContext = new SchemaContext();
        schemaContext.addContext(CreateSchemaRequest.class.getSimpleName(), createSchemaRequest);
        ContextUtils.addSchemaUpdaterContext(schemaContext, schemaUpdater);
        final var processor = processorHub.getProcessor(SchemaEvent.CREATE_SCHEMA)
                .orElseThrow((Supplier<Throwable>) () -> LeiaException.error(LeiaSchemaErrorCode.PROCESSOR_NOT_FOUND));
        processor.process(schemaContext);
        return schemaContext.getContext(SchemaDetails.class).orElse(null);
    }

    @SneakyThrows
    public SchemaDetails update(U schemaUpdater, UpdateSchemaRequest updateRequest) {
        final var schemaContext = new SchemaContext();
        schemaContext.addContext(UpdateSchemaRequest.class.getSimpleName(), updateRequest);
        ContextUtils.addSchemaUpdaterContext(schemaContext, schemaUpdater);
        final var processor = processorHub.getProcessor(SchemaEvent.UPDATE_SCHEMA)
                .orElseThrow((Supplier<Throwable>) () -> LeiaException.error(LeiaSchemaErrorCode.PROCESSOR_NOT_FOUND));
        processor.process(schemaContext);
        return schemaContext.getContext(SchemaDetails.class).orElse(null);
    }

    @SneakyThrows
    public SchemaDetails approve(U schemaUpdater, SchemaKey schemaKey) {
        final var schemaContext = new SchemaContext();
        schemaContext.addContext(SchemaKey.class.getSimpleName(), schemaKey);
        ContextUtils.addSchemaUpdaterContext(schemaContext, schemaUpdater);
        final var processor = processorHub.getProcessor(SchemaEvent.APPROVE_SCHEMA)
                .orElseThrow((Supplier<Throwable>) () -> LeiaException.error(LeiaSchemaErrorCode.PROCESSOR_NOT_FOUND));
        processor.process(schemaContext);
        return schemaContext.getContext(SchemaDetails.class).orElse(null);
    }

    @SneakyThrows
    public SchemaDetails reject(U schemaUpdater, SchemaKey schemaKey) {
        final var schemaContext = new SchemaContext();
        schemaContext.addContext(SchemaKey.class.getSimpleName(), schemaKey);
        ContextUtils.addSchemaUpdaterContext(schemaContext, schemaUpdater);
        final var processor = processorHub.getProcessor(SchemaEvent.REJECT_SCHEMA)
                .orElseThrow((Supplier<Throwable>) () -> LeiaException.error(LeiaSchemaErrorCode.PROCESSOR_NOT_FOUND));
        processor.process(schemaContext);
        return schemaContext.getContext(SchemaDetails.class).orElse(null);
    }
}
