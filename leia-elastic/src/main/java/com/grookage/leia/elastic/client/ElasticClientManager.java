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

package com.grookage.leia.elastic.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.google.common.base.Preconditions;
import com.grookage.leia.elastic.config.ElasticConfig;
import com.grookage.leia.elastic.utils.ElasticClientUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

@Slf4j
@Getter
public class ElasticClientManager {

	private final ElasticConfig elasticConfig;
	private ElasticsearchClient elasticClient;

	public ElasticClientManager(ElasticConfig elasticConfig) {
		this.elasticConfig = elasticConfig;
		this.start();
	}

	@SneakyThrows
	public void start() {
		log.info("Starting the elastic client");
		Preconditions.checkNotNull(elasticConfig, "ElasticConfig can't be null");
		final var hosts = elasticConfig.getServers().stream()
				.map(serverConfig -> new HttpHost(serverConfig.getHost(),
						serverConfig.getPort(), ElasticClientUtils.getScheme(elasticConfig))).toArray(HttpHost[]::new);
		final var restClientBuilder = RestClient.builder(hosts);
		if (null != elasticConfig.getAuthConfig()) {
			final var sslContext = ElasticClientUtils.getSslContext(elasticConfig);
			final var credentialsProvider = ElasticClientUtils.getAuthCredentials(elasticConfig);
			restClientBuilder.setHttpClientConfigCallback(httpAsyncClientBuilder -> {
				if (null != credentialsProvider) {
					httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
				}

				if (null != sslContext) {
					httpAsyncClientBuilder.setSSLContext(sslContext);
				}

				return httpAsyncClientBuilder;
			});
		}
		this.elasticClient = new ElasticsearchClient(new RestClientTransport(
				restClientBuilder.build(),
				new JacksonJsonpMapper()
		));
		log.info("Started the elastic client");
	}

}
