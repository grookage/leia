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

package com.grookage.leia.http.processor.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Locale;

@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpClientConfig {

	@NotEmpty
	String clientId;
	@Builder.Default
	int maxConnPerRoute = 10;
	@Builder.Default
	int maxConnTotal = 20;
	@Builder.Default
	int operationTimeout = 10000;
	@Builder.Default
	long connectionTimeoutMs = 10000;
	@Builder.Default
	long validateAfterInactivityMs = 10000;
	@Builder.Default
	long ttlMs = 60000;
	@Builder.Default
	long idleConnEvictMs = 60000;
	@Builder.Default
	boolean failOnUnavailableBackend = true;
	long messageProcessingThresholdMs = 10_000L;
	@NotEmpty
	List<HttpBackendConfig> backendConfigs;

	@JsonIgnore
	public List<String> getBackends() {
		if (null == backendConfigs) return List.of();
		return backendConfigs.stream()
				.map(each -> each.getBackendName().toUpperCase(Locale.ROOT)).toList();
	}

}
