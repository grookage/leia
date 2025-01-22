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

package com.grookage.leia.client.processor;

import com.grookage.leia.models.mux.MessageRequest;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.transformer.TransformationTarget;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
public class DefaultTargetRetriever implements TargetRetriever {

    @Override
    public Set<TransformationTarget> getTargets(MessageRequest messageRequest, List<SchemaDetails> schemaDetails) {
        if (null == schemaDetails || null == messageRequest) return Set.of();
        return schemaDetails
                .stream()
                .filter(each -> each.getSchemaKey().equals(messageRequest.getSchemaKey()))
                .findFirst()
                .map(SchemaDetails::getTransformationTargets)
                .orElse(Set.of());
    }
}
