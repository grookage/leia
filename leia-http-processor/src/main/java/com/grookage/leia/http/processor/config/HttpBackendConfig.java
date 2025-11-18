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
import lombok.Data;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpBackendConfig {
	@NotEmpty
	String backendName;
	String backendId;
	String host;
	int port;
	boolean secure;
	String uri;
	String requestEnv;
	@NotEmpty
	@ToString.Exclude
	String hasher;
	@Builder.Default
	BackendType backendType = BackendType.SYNC;
	@Builder.Default
	int retryCount = 3;
	@Builder.Default
	String queuePath = "leia-messages";
	@Builder.Default
	int queueThreshold = 5;
	@Builder.Default
	Map<String, String> headers = new HashMap<>();

	@JsonIgnore
	public boolean headersProvided() {
		return null != headers && !headers.isEmpty();
	}
}
