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

package com.grookage.leia.models.schema.ingestion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.grookage.leia.models.attributes.SchemaAttribute;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.SchemaType;
import com.grookage.leia.models.schema.SchemaValidationType;
import com.grookage.leia.models.schema.transformer.TransformationTarget;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateSchemaRequest {
    @NotNull
    @Valid
    private SchemaKey schemaKey;
    @NotNull
    private SchemaType schemaType;
    private SchemaValidationType validationType = SchemaValidationType.MATCHING;
    private String description;
    private String changeLog;
    @NotEmpty
    private Set<SchemaAttribute> attributes;
    @Builder.Default
    private Set<TransformationTarget> transformationTargets = Set.of();
    private JsonNode data;
    @Builder.Default
    Set<String> tags = new HashSet<>();
}
