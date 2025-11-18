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
import co.elastic.clients.elasticsearch._types.HealthStatus;
import com.google.common.base.Preconditions;
import com.grookage.leia.dropwizard.bundle.health.LeiaHealthCheck;
import com.grookage.leia.elastic.config.ElasticConfig;
import lombok.SneakyThrows;

public class ElasticHealthCheck extends LeiaHealthCheck {

	private final ElasticConfig elasticConfig;
	private final ElasticsearchClient elasticClient;

	public ElasticHealthCheck(ElasticConfig elasticConfig,
	                          ElasticsearchClient elasticClient) {
		super("elastic-health-check");
		Preconditions.checkNotNull(elasticConfig, "Elastic config can't be null");
		Preconditions.checkNotNull(elasticClient, "Elastic Client can't be null");
		this.elasticConfig = elasticConfig;
		this.elasticClient = elasticClient;
	}

	@Override
	@SneakyThrows
	protected Result check() {
		final var healthStatus = elasticClient.cluster().health().status();
		if (healthStatus == HealthStatus.Red || (elasticConfig.isFailOnYellow() && healthStatus == HealthStatus.Yellow)) {
			return Result.unhealthy("Last status: %s", healthStatus.name());
		} else {
			return Result.healthy("Last status: %s", healthStatus.name());
		}
	}
}
