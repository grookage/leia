/*
 * Copyright (c) 2024-2025. Koushik R <rkoushik.14@gmail.com>.
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

package com.grookage.leia.mux;


import com.google.common.base.Preconditions;
import com.grookage.leia.models.exception.LeiaException;
import com.grookage.leia.models.mux.LeiaMessage;
import com.grookage.leia.mux.exception.LeiaProcessorErrorCode;
import com.grookage.leia.mux.executor.MessageExecutor;
import com.grookage.leia.mux.executor.MessageExecutorFactory;
import com.grookage.leia.mux.resolver.BackendNameResolver;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Data
@Slf4j
public class DefaultMessageProcessor implements MessageProcessor {

    private final String name;
    private final long processingThresholdMs;
    private final BackendNameResolver backendNameResolver;
    private final MessageExecutorFactory executorFactory;

    @Builder
    protected DefaultMessageProcessor(String name,
                                      long processingThresholdMs,
                                      BackendNameResolver backendNameResolver,
                                      MessageExecutorFactory executorFactory) {
        Preconditions.checkNotNull(backendNameResolver, "Backend Resolver can't be null");
        Preconditions.checkNotNull(executorFactory, "Executor Factory can't be null");
        this.name = name;
        this.processingThresholdMs = processingThresholdMs;
        this.backendNameResolver = backendNameResolver;
        this.executorFactory = executorFactory;
    }

    protected boolean validBackends(Set<String> backends) {
        return null != backends && !backends.isEmpty();
    }

    protected boolean validExecutor(MessageExecutor executor) {
        return null != executor;
    }

    private Map<MessageExecutor, List<LeiaMessage>> getExecutorMapping(List<LeiaMessage> messages) {
        final var executorMapping = new HashMap<MessageExecutor, List<LeiaMessage>>();
        messages.forEach(message -> {
            final var backends = backendNameResolver.getEligibleBackends(message);
            if (!validBackends(backends)) {
                log.error("No backends found for message with schemaKey {} and tags {}", message.getSchemaKey(), message.getTags());
                throw LeiaException.error(LeiaProcessorErrorCode.BACKENDS_NOT_FOUND);
            }
            backends.forEach(backend -> {
                final var executor = executorFactory.getExecutor(backend).orElse(null);
                if (!validExecutor(executor)) {
                    log.error("No executor found for backend name {}", backend);
                    throw LeiaException.error(LeiaProcessorErrorCode.EXECUTOR_NOT_FOUND);
                }
                executorMapping.computeIfAbsent(executor, k -> new ArrayList<>()).add(message);
            });
        });
        return executorMapping;
    }

    public void processMessages(List<LeiaMessage> messages) {
        final var executorMapping = getExecutorMapping(messages);
        if (executorMapping.isEmpty()) {
            log.debug("Haven't found any eligible executors with the set of messages {}", messages);
            return;
        }
        final var futures = CompletableFuture.allOf(
                executorMapping.entrySet().stream()
                        .map(each -> CompletableFuture.runAsync(
                                () -> each.getKey().send(each.getValue())))
                        .toArray(CompletableFuture[]::new));
        try {
            futures.get(getProcessingThresholdMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Couldn't perform the message processor execution. It exceeded the process duration specified : {}",
                    getProcessingThresholdMs(), e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Couldn't perform the message processor execution. It exceeded the process duration set at " + getProcessingThresholdMs());
        } catch (Exception e) {
            log.error("There is an exception while trying to process messages", e);
            throw new IllegalStateException("There is an exception while trying to process messages", e);
        }
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        final var thatKey = (DefaultMessageProcessor) obj;
        return (thatKey.getName().equalsIgnoreCase(this.getName()));
    }
}


