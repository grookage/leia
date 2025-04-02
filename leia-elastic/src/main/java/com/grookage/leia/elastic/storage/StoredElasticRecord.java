/*
 * Copyright (c) 2025. Koushik R <rkoushik.14@gmail.com>.
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

package com.grookage.leia.elastic.storage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.grookage.leia.models.attributes.SchemaAttribute;
import com.grookage.leia.models.schema.SchemaHistoryItem;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.SchemaType;
import com.grookage.leia.models.schema.SchemaValidationType;
import com.grookage.leia.models.schema.engine.SchemaState;
import com.grookage.leia.models.schema.transformer.TransformationTarget;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StoredElasticRecord {
    @NotBlank
    private String orgId;
    @NotBlank
    private String namespace;
    @NotBlank
    private String tenantId;
    @NotBlank
    private String schemaName;
    @NotBlank
    private String version;
    @NotBlank
    private String type;
    String description;
    @NotNull
    SchemaState schemaState;
    @NotNull
    SchemaType schemaType;
    SchemaValidationType validationType = SchemaValidationType.MATCHING;
    @NotEmpty
    Set<SchemaAttribute> attributes;
    @Builder.Default
    Set<TransformationTarget> transformationTargets = Set.of();
    @Builder.Default
    Set<SchemaHistoryItem> histories = new HashSet<>();
    @Builder.Default
    Set<String> tags = new HashSet<>();

    @JsonIgnore
    public SchemaKey getSchemaKey() {
        return SchemaKey.builder()
                .orgId(orgId)
                .namespace(namespace)
                .tenantId(tenantId)
                .schemaName(schemaName)
                .version(version)
                .type(type)
                .build();
    }
}
