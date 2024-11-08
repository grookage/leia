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

import com.grookage.leia.core.exception.LeiaErrorCode;
import com.grookage.leia.core.exception.LeiaException;
import com.grookage.leia.core.ingestion.utils.ContextUtils;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.engine.SchemaContext;
import com.grookage.leia.models.schema.engine.SchemaEvent;
import com.grookage.leia.models.schema.engine.SchemaState;
import com.grookage.leia.models.schema.ingestion.UpdateSchemaRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@Slf4j
public class UpdateSchemaProcessor extends SchemaProcessor {
    @Override
    public SchemaEvent name() {
        return SchemaEvent.UPDATE_SCHEMA;
    }

    @Override
    @SneakyThrows
    public void process(SchemaContext context) {
        final var updateSchemaRequest = context.getContext(UpdateSchemaRequest.class)
                .orElseThrow((Supplier<Throwable>) () -> LeiaException.error(LeiaErrorCode.VALUE_NOT_FOUND));
        final var storedSchema = getSchemaRepository()
                .get(SchemaKey.builder()
                        .version(updateSchemaRequest.getVersion())
                        .schemaName(updateSchemaRequest.getSchemaName())
                        .namespace(updateSchemaRequest.getNamespace())
                        .build()).orElse(null);
        if (null == storedSchema || storedSchema.getSchemaState() != SchemaState.CREATED) {
            log.error("There are no stored schemas present with namespace {}, version {} and schemaName {}. Please try updating them instead",
                    updateSchemaRequest.getNamespace(),
                    updateSchemaRequest.getVersion(),
                    updateSchemaRequest.getSchemaName());
            throw LeiaException.error(LeiaErrorCode.NO_SCHEMA_FOUND);
        }
        final var userName = ContextUtils.getUser(context);
        final var email = ContextUtils.getEmail(context);
        storedSchema.setDescription(updateSchemaRequest.getDescription());
        storedSchema.setAttributes(updateSchemaRequest.getAttributes());
        storedSchema.getSchemaMeta().setCreatedBy(userName);
        storedSchema.getSchemaMeta().setCreatedByEmail(email);
        storedSchema.getSchemaMeta().setCreatedAt(System.currentTimeMillis());
        storedSchema.setValidationType(updateSchemaRequest.getValidationType());
        storedSchema.setSchemaType(updateSchemaRequest.getSchemaType());
        if (null != updateSchemaRequest.getTransformationTargets()) {
            storedSchema.setTransformationTargets(updateSchemaRequest.getTransformationTargets());
        }
        getSchemaRepository().update(storedSchema);
        context.addContext(SchemaDetails.class.getSimpleName(), storedSchema);
    }
}
