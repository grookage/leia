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

package com.grookage.leia.mux;

import com.fasterxml.jackson.core.type.TypeReference;
import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.exception.LeiaException;
import com.grookage.leia.models.mux.LeiaMessage;
import com.grookage.leia.mux.executor.MessageExecutor;
import com.grookage.leia.mux.executor.MessageExecutorFactory;
import com.grookage.leia.mux.resolver.TagBasedNameResolver;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class DefaultMessageProcessorTest {

    @Test
    @SneakyThrows
    void testHttpMessageProcessor() {
        final var resolver = new TagBasedNameResolver(() -> Set.of("BACKEND1"));
        final var httpExecutor = Mockito.mock(MessageExecutor.class);
        final var executorFactory = new MessageExecutorFactory() {
            @Override
            public Optional<MessageExecutor> getExecutor(String backendName) {
                return backendName.equalsIgnoreCase("BACKEND1") ? Optional.of(httpExecutor) : Optional.empty();
            }
        };
        final var leiaMessages = ResourceHelper.getResource("mux/leiaMessages.json", new TypeReference<List<LeiaMessage>>() {
        });
        final var messageProcessor = new DefaultMessageProcessor("test", 10_000L, resolver, executorFactory) {
            @Override
            protected boolean validBackends(Set<String> backends) {
                return false;
            }

            @Override
            protected boolean validExecutor(MessageExecutor executor) {
                return false;
            }
        };
        Assertions.assertThrows(LeiaException.class, () -> messageProcessor.processMessages(leiaMessages));
        leiaMessages.forEach(leiaMessage -> leiaMessage.setTags(Set.of("backend-backend1::backend2",
                "importance-mild::extreme")));
        final var messageProcessor1 = new DefaultMessageProcessor("test", 10_000L, resolver, executorFactory) {
            @Override
            protected boolean validBackends(Set<String> backends) {
                return true;
            }

            @Override
            protected boolean validExecutor(MessageExecutor executor) {
                return true;
            }
        };
        messageProcessor1.processMessages(leiaMessages);
        Mockito.verify(httpExecutor, Mockito.times(1)).send(leiaMessages);
    }
}
