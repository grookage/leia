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

import com.grookage.leia.core.exception.LeiaSchemaErrorCode;
import com.grookage.leia.models.exception.LeiaException;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.engine.SchemaContext;
import com.grookage.leia.models.schema.engine.SchemaEvent;
import com.grookage.leia.models.schema.engine.SchemaState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.function.Supplier;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@Slf4j
public class RejectSchemaProcessor extends SchemaProcessor {

    private static final Set<SchemaState> ACCEPTABLE_STATES = Set.of(SchemaState.CREATED, SchemaState.APPROVED);

    @Override
    public SchemaEvent name() {
        return SchemaEvent.REJECT_SCHEMA;
    }

    @Override
    @SneakyThrows
    public void process(SchemaContext context) {
        final var schemaKey = context.getContext(SchemaKey.class)
                .orElseThrow((Supplier<Throwable>) () -> LeiaException.error(LeiaSchemaErrorCode.VALUE_NOT_FOUND));
        final var storedSchema = getRepositorySupplier().get().get(schemaKey).orElse(null);
        if (null == storedSchema || !ACCEPTABLE_STATES.contains(storedSchema.getSchemaState())) {
            log.error("There are no stored schemas present with namespace {}, version {} and schemaName {} or in acceptable states. " +
                            "Please try updating them instead",
                    schemaKey.getNamespace(),
                    schemaKey.getVersion(),
                    schemaKey.getSchemaName());
            throw LeiaException.error(LeiaSchemaErrorCode.NO_SCHEMA_FOUND);
        }
        addHistory(context, storedSchema);
        storedSchema.setSchemaState(SchemaState.REJECTED);
        getRepositorySupplier().get().update(storedSchema);
        context.addContext(SchemaDetails.class.getSimpleName(), storedSchema);
    }
}
