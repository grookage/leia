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

package com.grookage.leia.http.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.grookage.leia.http.processor.config.HttpBackendConfig;
import com.grookage.leia.http.processor.config.HttpClientConfig;
import com.grookage.leia.http.processor.config.LeiaHttpEndPoint;
import com.grookage.leia.http.processor.request.LeiaHttpEntity;
import com.grookage.leia.http.processor.utils.HttpClientUtils;
import com.grookage.leia.http.processor.utils.HttpRequestUtils;
import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.mux.LeiaMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@WireMockTest
@Slf4j
class HttpMessageExecutorTest {

    @Test
    @SneakyThrows
    void testSyncMessageSending(WireMockRuntimeInfo wireMockRuntimeInfo) {
        final var clientConfig = ResourceHelper.getResource("httpClientConfig.json", HttpClientConfig.class);
        HttpClientUtils.initialize(clientConfig);
        Assertions.assertNotNull(clientConfig);
        final var backend = clientConfig.getBackendConfigs().stream().findFirst().orElse(null);
        Assertions.assertNotNull(backend);
        final var port = wireMockRuntimeInfo.getHttpPort();
        backend.setPort(port);
        backend.setUri("/ingest");
        final var messages = ResourceHelper.getResource("mux/leiaMessages.json", new TypeReference<List<LeiaMessage>>() {
        });
        Assertions.assertNotNull(messages);
        Assertions.assertFalse(messages.isEmpty());
        final var entityMessages = HttpRequestUtils.toHttpEntity(messages, backend);
        stubFor(post(urlEqualTo("/ingest"))
                .withRequestBody(binaryEqualTo(ResourceHelper.getObjectMapper().writeValueAsBytes(entityMessages)))
                .willReturn(aResponse()
                        .withStatus(200)));
        final var testableExecutor = new HttpMessageExecutor<>(backend, () -> "Bearer 1234", ResourceHelper.getObjectMapper()) {
            @Override
            public String backendName() {
                return backend.getBackendName();
            }

            @Override
            public void handleException(List<LeiaMessage> messages, Exception exception) {
                log.error("Error sending messages to backend {}: {}", this.getBackendConfig().getBackendName(), exception.getMessage());
            }

            @Override
            public Object getRequestData(LeiaHttpEntity leiaHttpEntity) {
                return leiaHttpEntity;
            }

            @Override
            public Optional<LeiaHttpEndPoint> getEndPoint(HttpBackendConfig backendConfig) {
                return Optional.of(LeiaHttpEndPoint.builder()
                        .host("127.0.0.1")
                        .port(port)
                        .secure(backendConfig.isSecure())
                        .uri(backendConfig.getUri())
                        .build());
            }
        };
        testableExecutor.send(messages);
    }


    @Test
    @SneakyThrows
    void testExceptionHandlerWithRetryLogic(WireMockRuntimeInfo wireMockRuntimeInfo) {
        final var clientConfig = ResourceHelper.getResource("httpClientConfig.json", HttpClientConfig.class);
        HttpClientUtils.initialize(clientConfig);
        final var backend = clientConfig.getBackendConfigs().stream().findFirst().orElse(null);
        Assertions.assertNotNull(backend);
        final var port = wireMockRuntimeInfo.getHttpPort();
        backend.setPort(port);
        backend.setUri("/ingest");
        backend.setRetryCount(1);

        final var backendName = backend.getBackendName();

        final var messages = ResourceHelper.getResource("mux/leiaMessages.json", new TypeReference<List<LeiaMessage>>() {
        });

        final var testableExecutor = new HttpMessageExecutor<>(
                backend,
                () -> "Bearer 1234",
                ResourceHelper.getObjectMapper()) {
            @Override
            public String backendName() {
                return backendName;
            }

            @Override
            public void handleException(List<LeiaMessage> messages, Exception exception) {
                final var backendName = backendName();
                log.error("Message send failed for backend: {}. Attempting custom retry logic...", backendName);
                log.error("Exception details: Type={}, Message={}", exception.getClass().getName(), exception.getMessage());

                // Categorize errors and handle differently
                if (isRetryableError(exception)) {
                    log.warn("Retryable error detected for backend {}. Will attempt retry using executor", backendName);
                } else if (isClientError(exception)) {
                    log.error("Client error detected for backend {}. Messages need manual intervention", backendName);
                    sendToDeadLetterQueue(messages, backendName);
                } else {
                    log.error("Unknown error for backend {}. Sending to DLQ", backendName);
                    sendToDeadLetterQueue(messages, backendName);
                }

                logFailedMessages(messages);
            }

            @Override
            public Object getRequestData(LeiaHttpEntity leiaHttpEntity) {
                return leiaHttpEntity;
            }

            @Override
            public Optional<LeiaHttpEndPoint> getEndPoint(HttpBackendConfig backendConfig) {
                return Optional.of(LeiaHttpEndPoint.builder()
                        .host("127.0.0.1")
                        .port(port)
                        .secure(backendConfig.isSecure())
                        .uri(backendConfig.getUri())
                        .build());
            }
        };

        stubFor(post(urlEqualTo("/ingest"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withBody("Service Unavailable")));
        testableExecutor.send(messages);
    }

    private boolean isRetryableError(Exception exception) {
        return exception.getMessage().contains("500") ||
                exception.getMessage().contains("503") ||
                exception.getMessage().contains("429");
    }

    private boolean isClientError(Exception exception) {
        return exception.getMessage().contains("400") ||
                exception.getMessage().contains("401") ||
                exception.getMessage().contains("403");
    }

    private void sendToDeadLetterQueue(List<LeiaMessage> messages, String backendName) {
        log.warn("Sending {} messages to DLQ for backend {}", messages.size(), backendName);
        // Implementation: Send to DLQ, persist to database, or alert
    }

    private void logFailedMessages(List<LeiaMessage> messages) {
        messages.forEach(msg ->
                log.debug("Failed message - Schema: {}, Tags: {}",
                        msg.getSchemaKey(), msg.getTags())
        );
    }

}
