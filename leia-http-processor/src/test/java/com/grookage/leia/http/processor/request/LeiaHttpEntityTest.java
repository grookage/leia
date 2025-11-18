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

package com.grookage.leia.http.processor.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.grookage.leia.http.processor.config.HttpClientConfig;
import com.grookage.leia.http.processor.utils.HttpRequestUtils;
import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.mux.LeiaMessage;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class LeiaHttpEntityTest {

	@Test
	@SneakyThrows
	void testLeiaHttpEntity() {
		final var clientConfig = ResourceHelper.getResource("httpClientConfig.json", HttpClientConfig.class);
		final var backend = clientConfig.getBackendConfigs().stream().findFirst().orElse(null);
		Assertions.assertNotNull(backend);

		final var leiaMessages = ResourceHelper.getResource("mux/leiaMessages.json", new TypeReference<List<LeiaMessage>>() {
		});
		final var httpEntity = HttpRequestUtils.toHttpEntity(leiaMessages, backend);
		Assertions.assertEquals(1, httpEntity.getEntities().size());
		httpEntity.getEntities().stream().findFirst().ifPresent(leiaMessageEntity -> Assertions.assertEquals("5aed308f0de68dc585bbd09581e5ddca", leiaMessageEntity.getSignature()));
	}
}
