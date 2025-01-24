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

package com.grookage.leia.dw.client;

import com.google.common.base.Preconditions;
import com.grookage.leia.client.LeiaMessageProduceClient;
import com.grookage.leia.client.datasource.NamespaceDataSource;
import com.grookage.leia.client.refresher.LeiaClientRefresher;
import com.grookage.leia.client.refresher.LeiaClientSupplier;
import com.grookage.leia.mux.processors.DefaultTargetValidator;
import com.grookage.leia.mux.processors.MessageProcessor;
import com.grookage.leia.mux.processors.TargetValidator;
import com.grookage.leia.provider.config.LeiaHttpConfiguration;
import com.grookage.leia.validator.LeiaSchemaValidator;
import com.grookage.leia.validator.StaticSchemaValidator;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import lombok.Getter;

import java.util.Set;
import java.util.function.Supplier;

@Getter
public abstract class LeiaClientBundle<T extends Configuration> implements ConfiguredBundle<T> {

    private LeiaMessageProduceClient producerClient;

    protected abstract NamespaceDataSource getNamespaceDataSource(T configuration);

    protected int getRefreshIntervalSeconds(T configuration) {
        return 30;
    }

    protected boolean refreshEnabled(T configuration) {
        return false;
    }

    protected abstract boolean withProducerClient(T configuration);

    protected abstract LeiaHttpConfiguration getHttpConfiguration(T configuration);

    protected abstract Set<String> getPackageRoots(T configuration);

    protected Supplier<String> getAuthHeaderSupplier(T configuration) {
        return () -> null;
    }

    protected LeiaSchemaValidator getSchemaValidator(T configuration,
                                                     LeiaClientRefresher clientRefresher) {
        return StaticSchemaValidator.builder()
                .supplier(clientRefresher::getData)
                .packageRoots(getPackageRoots(configuration))
                .build();
    }

    protected abstract Supplier<MessageProcessor> getMessageProcessor(T configuration);

    protected Supplier<TargetValidator> getTargetRetriever(T configuration) {
        return DefaultTargetValidator::new;
    }

    @Override
    public void run(T configuration, Environment environment) {
        final var namespaceDataSource = getNamespaceDataSource(configuration);
        Preconditions.checkNotNull(namespaceDataSource, "Namespace data source can't be null");
        final var httpConfiguration = getHttpConfiguration(configuration);
        Preconditions.checkNotNull(httpConfiguration, "Http Configuration can't be null");
        final var packageRoots = getPackageRoots(configuration);
        Preconditions.checkArgument(null != packageRoots && !packageRoots.isEmpty(), "Package Roots can't be null or empty");
        final var withProducerClient = withProducerClient(configuration);
        final var dataRefreshSeconds = getRefreshIntervalSeconds(configuration);

        final var clientRefresher = LeiaClientRefresher.builder()
                .supplier(
                        LeiaClientSupplier.builder()
                                .httpConfiguration(httpConfiguration)
                                .namespaceDataSource(namespaceDataSource)
                                .authHeaderSupplier(getAuthHeaderSupplier(configuration))
                                .build()
                )
                .refreshTimeInSeconds(dataRefreshSeconds)
                .periodicRefresh(refreshEnabled(configuration))
                .build();
        final var validator = getSchemaValidator(configuration, clientRefresher);
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() {
                clientRefresher.start();
                validator.start();
            }
        });
        if (withProducerClient) {
            producerClient = LeiaMessageProduceClient.builder()
                    .refresher(clientRefresher)
                    .schemaValidator(validator)
                    .mapper(environment.getObjectMapper())
                    .messageProcessor(getMessageProcessor(configuration))
                    .targetValidator(getTargetRetriever(configuration))
                    .build();
            environment.lifecycle().manage(new Managed() {
                @Override
                public void start() {
                    producerClient.start();
                }
            });
        }
    }
}
