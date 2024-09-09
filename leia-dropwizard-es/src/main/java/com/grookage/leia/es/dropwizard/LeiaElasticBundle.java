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

package com.grookage.leia.es.dropwizard;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.grookage.leia.dropwizard.bundle.LeiaBundle;
import com.grookage.leia.dropwizard.bundle.health.LeiaHealthCheck;
import com.grookage.leia.elastic.config.ElasticConfig;
import com.grookage.leia.elastic.repository.ElasticRepository;
import com.grookage.leia.models.user.SchemaUpdater;
import com.grookage.leia.repository.SchemaRepository;
import com.grookage.leia.repository.config.CacheConfig;
import io.dropwizard.Configuration;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Getter
public abstract class LeiaElasticBundle<T extends Configuration, U extends SchemaUpdater> extends LeiaBundle<T, U> {

    private ElasticConfig elasticConfig;
    private ElasticsearchClient elasticsearchClient;
    private ElasticRepository elasticSchemaRepository;

    protected abstract ElasticConfig getElasticConfig(T configuration);

    protected abstract CacheConfig getCacheConfig(T configuration);

    @Override
    protected void runPreconditions(T configuration) {
        this.elasticConfig = getElasticConfig(configuration);
        final var cacheConfig = getCacheConfig(configuration);
        this.elasticSchemaRepository = new ElasticRepository(cacheConfig, elasticConfig);
        this.elasticsearchClient = elasticSchemaRepository.getClient();
    }

    @Override
    protected SchemaRepository getSchemaRepository(T configuration) {
        return elasticSchemaRepository;
    }

    protected List<LeiaHealthCheck> withHealthChecks(T configuration) {
        return List.of(new ElasticHealthCheck(elasticConfig, elasticsearchClient));
    }
}