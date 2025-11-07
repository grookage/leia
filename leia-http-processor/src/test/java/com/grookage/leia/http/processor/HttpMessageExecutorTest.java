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
import com.grookage.leia.mux.executor.MessageExceptionHandler;
import com.grookage.leia.mux.executor.MessageExecutor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

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
    void testSyncMessageSendingWithExceptionHandling(WireMockRuntimeInfo wireMockRuntimeInfo) {
        final var clientConfig = ResourceHelper.getResource("httpClientConfig.json", HttpClientConfig.class);
        HttpClientUtils.initialize(clientConfig);
        Assertions.assertNotNull(clientConfig);
        final var backend = clientConfig.getBackendConfigs().stream().findFirst().orElse(null);
        Assertions.assertNotNull(backend);
        final var port = wireMockRuntimeInfo.getHttpPort();
        backend.setPort(port);
        backend.setUri("/ingest");
        backend.setRetryCount(2);

        final var messages = ResourceHelper.getResource("mux/leiaMessages.json", new TypeReference<List<LeiaMessage>>() {
        });
        Assertions.assertNotNull(messages);
        Assertions.assertFalse(messages.isEmpty());

        stubFor(post(urlEqualTo("/ingest"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        final AtomicInteger exceptionHandlerCallCount = new AtomicInteger(0);
        final List<LeiaMessage> failedMessages = new ArrayList<>();

        // Create exception handler implementation
        MessageExceptionHandler exceptionHandler = new MessageExceptionHandler() {
            @Override
            public void handleException(List<LeiaMessage> msgs, Exception ex, String backendName, MessageExecutor executor) {
                exceptionHandlerCallCount.incrementAndGet();
                log.error("Exception handler invoked for {} messages on backend {}. Error: {}",
                        msgs.size(), backendName, ex.getMessage());

                msgs.forEach(msg -> {
                    log.error("Failed message - SchemaKey: {}, Tags: {}",
                            msg.getSchemaKey(),
                            msg.getTags());
                });

                failedMessages.addAll(msgs);
                log.info("Storing {} failed messages for later retry or DLQ processing", msgs.size());
            }
        };

        final var testableExecutor = new HttpMessageExecutor<>(
                backend,
                () -> "Bearer 1234",
                ResourceHelper.getObjectMapper(),
                exceptionHandler) {
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

        Assertions.assertThrows(Exception.class, () -> testableExecutor.send(messages));
        Assertions.assertEquals(1, exceptionHandlerCallCount.get());
        Assertions.assertEquals(messages.size(), failedMessages.size());

        log.info("Test completed. Failed messages can now be retried or sent to DLQ");
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

        final var messages = ResourceHelper.getResource("mux/leiaMessages.json", new TypeReference<List<LeiaMessage>>() {
        });

        final var exceptionHandler = new RetryableMessageExceptionHandler();

        final var testableExecutor = new HttpMessageExecutor<>(
                backend,
                () -> "Bearer 1234",
                ResourceHelper.getObjectMapper(),
                exceptionHandler) {
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

        Assertions.assertThrows(Exception.class, () -> testableExecutor.send(messages));
    }

    @Test
    @SneakyThrows
    void testExceptionHandlerWithExecutorRetry(WireMockRuntimeInfo wireMockRuntimeInfo) {
        final var clientConfig = ResourceHelper.getResource("httpClientConfig.json", HttpClientConfig.class);
        HttpClientUtils.initialize(clientConfig);
        final var backend = clientConfig.getBackendConfigs().stream().findFirst().orElse(null);
        Assertions.assertNotNull(backend);
        final var port = wireMockRuntimeInfo.getHttpPort();
        backend.setPort(port);
        backend.setUri("/ingest");

        final var messages = ResourceHelper.getResource("mux/leiaMessages.json", new TypeReference<List<LeiaMessage>>() {
        });

        final AtomicInteger retryCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);

        // Handler that retries using the executor
        MessageExceptionHandler retryHandler = new MessageExceptionHandler() {
            @Override
            public void handleException(List<LeiaMessage> msgs, Exception ex, String backendName, MessageExecutor executor) {
                failureCount.incrementAndGet();
                log.error("Handling exception for {} messages on backend {}", msgs.size(), backendName);

                // Retry logic: attempt up to 2 retries
                if (retryCount.incrementAndGet() <= 2) {
                    log.info("Attempting retry {} for {} messages", retryCount.get(), msgs.size());
                    try {
                        // Use the executor to retry sending
                        Thread.sleep(100); // Brief delay before retry
                        executor.send(msgs);
                        log.info("Retry {} succeeded", retryCount.get());
                    } catch (Exception retryEx) {
                        log.error("Retry {} failed: {}", retryCount.get(), retryEx.getMessage());
                    }
                } else {
                    log.error("Max retries exceeded. Sending {} messages to DLQ", msgs.size());
                    // Send to DLQ or dead letter handling
                }
            }
        };

        final var testableExecutor = new HttpMessageExecutor<>(
                backend,
                () -> "Bearer 1234",
                ResourceHelper.getObjectMapper(),
                retryHandler) {
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

        // First attempt fails, then succeeds on retry
        stubFor(post(urlEqualTo("/ingest"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error"))
                .willSetStateTo("First Retry"));

        stubFor(post(urlEqualTo("/ingest"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("First Retry")
                .willReturn(aResponse()
                        .withStatus(200)));

        testableExecutor.send(messages);

        log.info("Test completed with {} retries", retryCount.get());
    }

    /**
     * Sample implementation with retry logic using MessageExecutor
     */
    static class RetryableMessageExceptionHandler implements MessageExceptionHandler {

        private static final int MAX_RETRIES = 3;

        @Override
        public void handleException(List<LeiaMessage> messages, Exception exception, String backendName, MessageExecutor executor) {
            log.error("Message send failed for backend: {}. Attempting custom retry logic...", backendName);
            log.error("Exception details: Type={}, Message={}", exception.getClass().getName(), exception.getMessage());

            // Categorize errors and handle differently
            if (isRetryableError(exception)) {
                log.warn("Retryable error detected for backend {}. Will attempt retry using executor", backendName);
                retryWithBackoff(messages, executor, backendName);
            } else if (isClientError(exception)) {
                log.error("Client error detected for backend {}. Messages need manual intervention", backendName);
                sendToDeadLetterQueue(messages, backendName);
            } else {
                log.error("Unknown error for backend {}. Sending to DLQ", backendName);
                sendToDeadLetterQueue(messages, backendName);
            }

            logFailedMessages(messages);
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

        private void retryWithBackoff(List<LeiaMessage> messages, MessageExecutor executor, String backendName) {
            if (executor == null) {
                log.warn("Executor is null, cannot retry. Sending to DLQ");
                sendToDeadLetterQueue(messages, backendName);
                return;
            }

            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    log.info("Retry attempt {} of {} for backend {}", attempt, MAX_RETRIES, backendName);
                    Thread.sleep(attempt * 1000L); // Exponential backoff
                    executor.send(messages);
                    log.info("Retry attempt {} succeeded for backend {}", attempt, backendName);
                    return; // Success, exit
                } catch (Exception e) {
                    log.error("Retry attempt {} failed for backend {}: {}", attempt, backendName, e.getMessage());
                    if (attempt == MAX_RETRIES) {
                        log.error("All retry attempts exhausted for backend {}. Sending to DLQ", backendName);
                        sendToDeadLetterQueue(messages, backendName);
                    }
                }
            }
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

    /**
     * Sample implementation for logging-only exception handler
     */
    static class LoggingMessageExceptionHandler implements MessageExceptionHandler {

        @Override
        public void handleException(List<LeiaMessage> messages, Exception exception, String backendName, MessageExecutor executor) {
            log.error("Backend: {} - Failed to send {} messages", backendName, messages.size());
            log.error("Exception: ", exception);

            messages.forEach(msg ->
                    log.error("Failed message - SchemaKey: {}, Tags: {}",
                            msg.getSchemaKey(), msg.getTags())
            );
        }
    }
}
