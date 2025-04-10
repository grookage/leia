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

package com.grookage.leia.repository;

import com.grookage.leia.models.request.SearchRequest;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;

import java.util.List;
import java.util.Optional;

public interface SchemaRepository {

    boolean createdRecordExists(final SchemaKey schemaKey);

    boolean recordExists(final SchemaKey schemaKey);

    void create(final SchemaDetails schema);

    void update(final SchemaDetails schema);

    Optional<SchemaDetails> get(final SchemaKey schemaKey);

    List<SchemaDetails> getSchemas(final SearchRequest searchRequest);
}
