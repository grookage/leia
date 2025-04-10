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

import com.google.common.base.Preconditions;
import com.grookage.leia.models.request.LeiaRequestContext;
import com.grookage.leia.models.request.SearchRequest;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.utils.CollectionUtils;
import com.grookage.leia.repository.SchemaRepository;
import com.grookage.leia.repository.config.CacheConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Getter
public class SchemaRetriever {

    private final Supplier<SchemaRepository> repositorySupplier;
    private final CacheConfig cacheConfig;
    private RepositoryRefresher refresher;

    @Builder
    public SchemaRetriever(final Supplier<SchemaRepository> repositorySupplier,
                           final CacheConfig cacheConfig) {
        Preconditions.checkNotNull(repositorySupplier, "Schema Repository can't be null");
        this.repositorySupplier = repositorySupplier;
        this.cacheConfig = cacheConfig;

        if (null != cacheConfig && cacheConfig.isEnabled()) {
            final var supplier = new RepositorySupplier(repositorySupplier);
            supplier.start();
            this.refresher = RepositoryRefresher.builder()
                    .supplier(supplier)
                    .refreshTimeInSeconds(cacheConfig.getRefreshCacheSeconds())
                    .periodicRefresh(true)
                    .build();
            refresher.start();
        }
    }

    private boolean useRepositoryCache(final LeiaRequestContext requestContext) {
        return null != requestContext && !requestContext.isIgnoreCache()
                && null != cacheConfig && cacheConfig.isEnabled();
    }

    public Optional<SchemaDetails> getSchemaDetails(final LeiaRequestContext requestContext,
                                                    final SchemaKey schemaKey) {
        if (useRepositoryCache(requestContext)) {
            return this.refresher.getData().getSchemaDetails(schemaKey);
        } else {
            return repositorySupplier.get().get(schemaKey);
        }
    }

    private boolean match(SchemaDetails schemaDetails, SearchRequest searchRequest) {
        final var schemaKey = schemaDetails.getSchemaKey();
        final var orgIdMatch = CollectionUtils.isNullOrEmpty(searchRequest.getOrgIds()) ||
                               searchRequest.getOrgIds().contains(schemaKey.getOrgId());
        final var namespaceMatch = CollectionUtils.isNullOrEmpty(searchRequest.getNamespaces()) ||
                searchRequest.getNamespaces().contains(schemaKey.getNamespace());
        final var tenantIdMatch = CollectionUtils.isNullOrEmpty(searchRequest.getTenants()) ||
                                  searchRequest.getTenants().contains(schemaKey.getTenantId());
        final var schemaNameMatch = CollectionUtils.isNullOrEmpty(searchRequest.getSchemaNames()) ||
                searchRequest.getSchemaNames().contains(schemaKey.getSchemaName());
        final var schemaStateMatch = CollectionUtils.isNullOrEmpty(searchRequest.getStates()) ||
                searchRequest.getStates().contains(schemaDetails.getSchemaState());
        return orgIdMatch && namespaceMatch && tenantIdMatch && schemaNameMatch && schemaStateMatch;
    }

    public List<SchemaDetails> getSchemaDetails(final LeiaRequestContext requestContext,
                                                final SearchRequest searchRequest) {
        if (useRepositoryCache(requestContext)) {
            return refresher.getData().getSchemaDetails().stream()
                    .filter(each -> match(each, searchRequest))
                    .toList();
        } else {
            return repositorySupplier.get().getSchemas(searchRequest);
        }
    }
}
