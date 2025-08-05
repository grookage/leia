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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.grookage.leia.core.ingestion.processors.*;
import com.grookage.leia.core.managers.SchemaProcessorFactory;
import com.grookage.leia.models.schema.engine.SchemaEvent;
import com.grookage.leia.models.schema.engine.SchemaEventVisitor;
import com.grookage.leia.repository.SchemaRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class SchemaProcessorHub {

    private final Map<SchemaEvent, SchemaProcessor> processors = Maps.newHashMap();
    private Supplier<SchemaRepository> repositorySupplier;
    private Supplier<SchemaProcessorFactory> schemaProcessorFactorySupplier;

    private SchemaProcessorHub() {

    }

    public static SchemaProcessorHub of() {
        return new SchemaProcessorHub();
    }

    public SchemaProcessorHub withRepositoryResolver(Supplier<SchemaRepository> repositorySupplier) {
        this.repositorySupplier = repositorySupplier;
        return this;
    }

    public SchemaProcessorHub withProcessorFactory(Supplier<SchemaProcessorFactory> schemaProcessorFactorySupplier) {
        this.schemaProcessorFactorySupplier = schemaProcessorFactorySupplier;
        return this;
    }

    public SchemaProcessorHub build() {
        Preconditions.checkNotNull(repositorySupplier, "Schema Repository can't be null");
        Arrays.stream(SchemaEvent.values()).forEach(this::buildProcessor);
        return this;
    }


    public void buildProcessor(final SchemaEvent event) {
        processors.putIfAbsent(event, event.accept(new SchemaEventVisitor<>() {
            @Override
            public SchemaProcessor schemaCreate() {
                return CreateSchemaProcessor.builder()
                        .repositorySupplier(repositorySupplier)
                        .processorFactorySupplier(schemaProcessorFactorySupplier)
                        .build();
            }

            @Override
            public SchemaProcessor schemaUpdate() {
                return UpdateSchemaProcessor.builder()
                        .repositorySupplier(repositorySupplier)
                        .processorFactorySupplier(schemaProcessorFactorySupplier)
                        .build();
            }

            @Override
            public SchemaProcessor schemaApprove() {
                return ApproveSchemaProcessor.builder()
                        .repositorySupplier(repositorySupplier)
                        .processorFactorySupplier(schemaProcessorFactorySupplier)
                        .build();
            }

            @Override
            public SchemaProcessor schemaReject() {
                return RejectSchemaProcessor.builder()
                        .repositorySupplier(repositorySupplier)
                        .processorFactorySupplier(schemaProcessorFactorySupplier)
                        .build();
            }
        }));
    }


    public Optional<SchemaProcessor> getProcessor(final SchemaEvent event) {
        return Optional.ofNullable(processors.get(event));
    }

}
