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

package com.grookage.leia.dropwizard.bundle;

import com.google.common.base.Preconditions;
import com.grookage.leia.core.ingestion.SchemaIngestor;
import com.grookage.leia.core.ingestion.VersionIDGenerator;
import com.grookage.leia.core.ingestion.hub.SchemaProcessorHub;
import com.grookage.leia.core.retrieval.SchemaRetriever;
import com.grookage.leia.dropwizard.bundle.health.LeiaHealthCheck;
import com.grookage.leia.dropwizard.bundle.lifecycle.Lifecycle;
import com.grookage.leia.dropwizard.bundle.mapper.LeiaExceptionMapper;
import com.grookage.leia.dropwizard.bundle.mapper.LeiaRefresherMapper;
import com.grookage.leia.dropwizard.bundle.resolvers.SchemaUpdaterResolver;
import com.grookage.leia.dropwizard.bundle.resources.IngestionResource;
import com.grookage.leia.dropwizard.bundle.resources.SchemaResource;
import com.grookage.leia.models.user.SchemaUpdater;
import com.grookage.leia.repository.SchemaRepository;
import com.grookage.leia.repository.config.CacheConfig;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Supplier;

@NoArgsConstructor
@Getter
public abstract class LeiaBundle<T extends Configuration, U extends SchemaUpdater> implements ConfiguredBundle<T> {

    private SchemaIngestor<U> schemaIngestor;
    private SchemaRepository schemaRepository;
    private SchemaRetriever schemaRetriever;

    protected abstract Supplier<SchemaUpdaterResolver<U>> userResolver(T configuration);

    protected abstract CacheConfig getCacheConfig(T configuration);

    protected abstract Supplier<SchemaRepository> getSchemaRepository(T configuration);

    protected abstract Supplier<VersionIDGenerator> getVersionIDGenerator();

    protected List<LeiaHealthCheck> withHealthChecks(T configuration) {
        return List.of();
    }

    protected List<Lifecycle> withLifecycleManagers(T configuration) {
        return List.of();
    }

    @Override
    public void run(T configuration, Environment environment) {
        final var userResolver = userResolver(configuration).get();
        Preconditions.checkNotNull(userResolver, "User Resolver can't be null");
        this.schemaRepository = getSchemaRepository(configuration).get();
        final var schemaProcessorHub = SchemaProcessorHub.of()
                .withSchemaRepository(schemaRepository)
                .withVersionIDGenerator(getVersionIDGenerator().get())
                .build();
        this.schemaIngestor = new SchemaIngestor<U>()
                .withProcessorHub(schemaProcessorHub)
                .build();
        final var cacheConfig = getCacheConfig(configuration);
        this.schemaRetriever = new SchemaRetriever(schemaRepository, cacheConfig);
        withLifecycleManagers(configuration)
                .forEach(lifecycle -> environment.lifecycle().manage(new Managed() {
                    @Override
                    public void start() {
                        lifecycle.start();
                    }

                    @Override
                    public void stop() {
                        lifecycle.stop();
                    }
                }));
        withHealthChecks(configuration)
                .forEach(leiaHealthCheck -> environment.healthChecks().register(leiaHealthCheck.getName(), leiaHealthCheck));
        environment.jersey().register(new IngestionResource<>(schemaIngestor, userResolver));
        environment.jersey().register(new SchemaResource(schemaRetriever));
        environment.jersey().register(new LeiaExceptionMapper());
        environment.jersey().register(new LeiaRefresherMapper());
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        //NOOP. Nothing to do here.
    }
}
