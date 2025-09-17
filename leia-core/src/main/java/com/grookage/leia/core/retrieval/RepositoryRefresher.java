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

package com.grookage.leia.core.retrieval;

import com.grookage.korg.refresher.AbstractKorgRefresher;
import com.grookage.leia.models.schema.SchemaRegistry;
import lombok.Builder;

public class RepositoryRefresher extends AbstractKorgRefresher<SchemaRegistry> {

    @Builder
    protected RepositoryRefresher(final RepositorySupplier supplier,
                                  final int refreshTimeInSeconds,
                                  final boolean periodicRefresh) {
        super(supplier, refreshTimeInSeconds, periodicRefresh, null);
    }

}
