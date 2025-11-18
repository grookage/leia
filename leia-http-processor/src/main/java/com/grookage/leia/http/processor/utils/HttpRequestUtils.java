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

package com.grookage.leia.http.processor.utils;

import com.google.common.base.Joiner;
import com.google.common.hash.Hashing;
import com.grookage.leia.http.processor.config.HttpBackendConfig;
import com.grookage.leia.http.processor.request.LeiaHttpEntity;
import com.grookage.leia.http.processor.request.LeiaMessageEntity;
import com.grookage.leia.models.mux.LeiaMessage;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.util.List;

@UtilityClass
public class HttpRequestUtils {

	public static String getMessageSignature(LeiaMessage message, String passKey) {
		return Hashing.murmur3_128()
				.hashString(Joiner.on(".").join(
								passKey, message.getSchemaKey().getReferenceId()),
						StandardCharsets.UTF_8).toString();
	}

	public static LeiaHttpEntity toHttpEntity(final List<LeiaMessage> messages, final HttpBackendConfig backendConfig) {
		return new LeiaHttpEntity(
				messages.stream()
						.map(each ->
								new LeiaMessageEntity(getMessageSignature(each, backendConfig.getHasher()),
										backendConfig.getBackendName(),
										each))
						.toList());
	}
}
