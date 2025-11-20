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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.*;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.grookage.leia.http.processor.config.BackendType;
import com.grookage.leia.http.processor.config.HttpBackendConfig;
import com.grookage.leia.http.processor.config.LeiaHttpEndPoint;
import com.grookage.leia.http.processor.exception.LeiaHttpErrorCode;
import com.grookage.leia.http.processor.request.LeiaHttpEntity;
import com.grookage.leia.http.processor.utils.HttpClientUtils;
import com.grookage.leia.http.processor.utils.HttpRequestUtils;
import com.grookage.leia.models.exception.LeiaException;
import com.grookage.leia.models.mux.LeiaMessage;
import com.grookage.leia.models.utils.MetricConstants;
import com.grookage.leia.mux.executor.MessageExecutor;
import com.leansoft.bigqueue.BigQueueImpl;
import com.leansoft.bigqueue.IBigQueue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.net.URIBuilder;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@AllArgsConstructor
@Slf4j
@Getter
public abstract class HttpMessageExecutor<T> extends MessageExecutor {

	private final String name;
	private final HttpBackendConfig backendConfig;
	private final Supplier<String> authSupplier;
	private final ObjectMapper mapper;
	private final Retryer<String> retryer;
	private QueuedSender queuedSender;
	private MetricRegistry metricRegistry;

	protected HttpMessageExecutor(HttpBackendConfig backendConfig,
	                              Supplier<String> authSupplier,
			ObjectMapper mapper, MetricRegistry metricRegistry) {
		super();
		Preconditions.checkNotNull(metricRegistry, "Metric Registry can't be null");
		this.metricRegistry = metricRegistry;
		this.name = backendConfig.getBackendName();
		this.backendConfig = backendConfig;
		this.authSupplier = authSupplier;
		this.mapper = mapper;
		this.retryer = RetryerBuilder.<String>newBuilder()
				.retryIfExceptionOfType(HttpResponseException.class)
				.withWaitStrategy(
						WaitStrategies.fixedWait(0, TimeUnit.MILLISECONDS))
				.withStopStrategy(StopStrategies.stopAfterAttempt(backendConfig.getRetryCount()))
				.withBlockStrategy(BlockStrategies.threadSleepStrategy())
				.build();
		if (backendConfig.getBackendType() == BackendType.QUEUED) {
			this.queuedSender = new QueuedSender(backendConfig, mapper, messages -> {
				executeRequest(messages);
				return messages;
			});
		}
	}

	public abstract T getRequestData(LeiaHttpEntity leiaHttpEntity);

	public abstract Optional<LeiaHttpEndPoint> getEndPoint(HttpBackendConfig backendConfig);

	@SneakyThrows
	private void executeRequest(List<LeiaMessage> messages) {
		try {
			retryer.call(() -> {
				final var leiaHttpEntity = HttpRequestUtils.toHttpEntity(messages, backendConfig);
				final var requestData = getRequestData(leiaHttpEntity);
				final var endPoint = getEndPoint(backendConfig).orElse(null);
				if (null == endPoint) {
					log.debug("No valid end point found for backendConfig {}", backendConfig);
					throw LeiaException.error(LeiaHttpErrorCode.INVALID_ENDPOINT);
				}
				final var httpUrl = new URIBuilder()
						.setScheme(endPoint.isSecure()
								? "https"
								: "http")
						.setHost(endPoint.getHost())
						.setPort(endPoint.getPort() == 0
								? endPoint.defaultPort()
								: endPoint.getPort())
						.setPath(endPoint.getUri())
						.build();
				var request = Request.post(httpUrl)
						.body(new ByteArrayEntity(mapper.writeValueAsBytes(requestData), ContentType.APPLICATION_JSON))
						.addHeader("Authorization", authSupplier.get());
				if (backendConfig.headersProvided()) {
					request = request.setHeaders(backendConfig.getHeaders()
							.entrySet().stream().map(each -> new BasicHeader(each.getKey(), each.getValue()))
							.toArray(BasicHeader[]::new));
				}
				final var response = HttpClientUtils.getExecutor().execute(request).handleResponse(httpResponse -> {
					final var code = httpResponse.getCode();
					if (code >= HttpStatus.SC_REDIRECTION) {
						throw new HttpResponseException(code, httpResponse.getReasonPhrase());
					}
					final var responseEntity = httpResponse.getEntity();
					return null == responseEntity ? null : EntityUtils.toString(responseEntity);
				});
				log.debug("Call to backend with backendConfig {} was successful and returned response {}", backendConfig, response);
				publishMetric(messages, Joiner.on(".").join(MetricConstants.SEND, MetricConstants.SUCCESS));
				return response;
			});
		} catch (Exception e) {
			log.error("Sending message to the backend {} has failed with exception {}", backendConfig.getBackendName(), e.getMessage(), e);
			publishMetric(messages, Joiner.on(".").join(MetricConstants.SEND, MetricConstants.FAILURE));
			handleException(messages, e.getCause() != null ? (Exception) e.getCause() : e);
		}
	}

	public void sendEnvelope(List<LeiaMessage> messages) {
		final var backendType = backendConfig.getBackendType();
		backendType.apply(new BackendType.BackendTypeVisitor() {
			@Override
			public void sync() {
				executeRequest(messages);
			}

			@Override
			public void queued() {
				Preconditions.checkNotNull(queuedSender, "QueuedSender can't be null in queued mode");
				queuedSender.send(messages);
			}
		});
	}

	private void publishMetric(List<LeiaMessage> messages, String status) {
		final var canonicalName = this.getClass().getCanonicalName();
		final var prefix = (null != canonicalName) ? canonicalName : MetricConstants.PREFIX;
		metricRegistry.meter(Joiner.on(".")
						.join(prefix, backendConfig.getBackendName(), MetricConstants.MESSAGE, status))
				.mark(messages.size());
		messages.forEach(message -> {
			final var metricKey = message.getSchemaKey().getReferenceId();
			metricRegistry.meter(Joiner.on(".").join(prefix, MetricConstants.MESSAGE, metricKey, status))
					.mark();
		});
	}

	public static class QueuedSender {
		private final IBigQueue messageQueue;
		private final ObjectMapper mapper;

		@SneakyThrows
		public QueuedSender(final HttpBackendConfig backendConfig,
		                    final ObjectMapper mapper,
		                    final UnaryOperator<List<LeiaMessage>> messageOperator) {
			final var perms = PosixFilePermissions.fromString("rwxrwxrwx");
			final var attr = PosixFilePermissions.asFileAttribute(perms);
			Files.createDirectories(Paths.get(backendConfig.getQueuePath()), attr);
			this.mapper = mapper;
			this.messageQueue = new BigQueueImpl(backendConfig.getQueuePath(), backendConfig.getBackendName());
			final var flushRunner = new FlushRunner(mapper, messageQueue, backendConfig, messageOperator);
			final var scheduler = Executors.newScheduledThreadPool(2);
			scheduler.scheduleWithFixedDelay(flushRunner, 0, 1, TimeUnit.SECONDS);
			scheduler.scheduleWithFixedDelay(new GcRunner(messageQueue), 0, 15, TimeUnit.SECONDS);
		}

		@SneakyThrows
		public void send(List<LeiaMessage> messages) {
			this.messageQueue.enqueue(mapper.writeValueAsBytes(messages));
		}
	}

	public static class FlushRunner implements Runnable {

		private final IBigQueue queue;
		private final ObjectMapper mapper;
		private final HttpBackendConfig backendConfig;
		private final UnaryOperator<List<LeiaMessage>> messageOperator;

		public FlushRunner(ObjectMapper mapper,
		                   IBigQueue queue,
		                   HttpBackendConfig backendConfig,
		                   UnaryOperator<List<LeiaMessage>> messageOperator) {
			this.queue = queue;
			this.mapper = mapper;
			this.backendConfig = backendConfig;
			this.messageOperator = messageOperator;
		}

		@Override
		public void run() {
			try {
				var messages = new ArrayList<LeiaMessage>();
				while (messages.size() < backendConfig.getQueueThreshold() && !queue.isEmpty()) {
					final var dqMessages = mapper.readValue(queue.dequeue(), new TypeReference<List<LeiaMessage>>() {
					});
					messages.addAll(dqMessages);
				}

				if (!messages.isEmpty()) {
					messageOperator.apply(messages);
				}
			} catch (Exception e) {
				log.error("Queue flush failed for backend config {} with exception", backendConfig, e);
				throw LeiaException.error(LeiaHttpErrorCode.QUEUE_SEND_FAILED);
			}
		}
	}

	public static class GcRunner implements Runnable {

		private final IBigQueue bigQueue;

		public GcRunner(IBigQueue bigQueue) {
			this.bigQueue = bigQueue;
		}

		@Override
		public void run() {
			try {
				log.info("Running BigQueue garbage collection");
				bigQueue.gc();
			} catch (Exception e) {
				log.error("Failed to run BigQueue garbage collection", e);
			}
		}
	}
}
