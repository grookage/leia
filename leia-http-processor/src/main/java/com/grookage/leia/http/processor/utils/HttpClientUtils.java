package com.grookage.leia.http.processor.utils;

import com.google.common.base.Preconditions;
import com.grookage.leia.http.processor.config.HttpClientConfig;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;

import java.util.concurrent.TimeUnit;

@UtilityClass
@Slf4j
public class HttpClientUtils {
	static Executor executor;

	public static void initialize(final HttpClientConfig httpClientConfig) {
		executor = Executor.newInstance(getCloseableClient(httpClientConfig));
	}

	public Executor getExecutor() {
		Preconditions.checkNotNull(executor, "Executor seems to not have been initialized, please call initialize first");
		return executor;
	}

	public static CloseableHttpClient getCloseableClient(final HttpClientConfig clientConfig) {
		return HttpClientBuilder.create()
				.setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
						.useSystemProperties()
						.setMaxConnPerRoute(clientConfig.getMaxConnPerRoute())
						.setMaxConnTotal(clientConfig.getMaxConnTotal())
						.setDefaultSocketConfig(SocketConfig.custom()
								.setTcpNoDelay(true)
								.setSoTimeout(clientConfig.getOperationTimeout(), TimeUnit.MILLISECONDS)
								.build())
						.setDefaultConnectionConfig(ConnectionConfig.custom()
								.setConnectTimeout(clientConfig.getConnectionTimeoutMs(), TimeUnit.MILLISECONDS)
								.setSocketTimeout(clientConfig.getOperationTimeout(), TimeUnit.MILLISECONDS)
								.setValidateAfterInactivity(TimeValue.ofMilliseconds(clientConfig.getValidateAfterInactivityMs()))
								.setTimeToLive(clientConfig.getTtlMs(), TimeUnit.MILLISECONDS)
								.build())
						.build())
				.useSystemProperties()
				.evictExpiredConnections()
				.evictIdleConnections(TimeValue.ofMilliseconds(clientConfig.getIdleConnEvictMs()))
				.setDefaultRequestConfig(RequestConfig.custom()
						.setResponseTimeout(clientConfig.getOperationTimeout(), TimeUnit.MILLISECONDS)
						.build())
				.build();
	}
}
