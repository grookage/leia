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

package com.grookage.leia.elastic.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthConfig {

	private boolean enabled;
	private String username;
	private String password;
	private boolean tlsEnabled;
	private String trustStorePath;
	private String keyStoreType;
	private String keyStorePass;

	public boolean valid() {
		if (isEnabled() && invalidAuthConfiguration()) {
			return false;
		}
		return !isTlsEnabled() || !invalidTlsConfiguration();
	}

	@JsonIgnore
	private boolean invalidAuthConfiguration() {
		return Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password);
	}

	@JsonIgnore
	private boolean invalidTlsConfiguration() {
		return Strings.isNullOrEmpty(trustStorePath) || Strings.isNullOrEmpty(keyStoreType)
				|| Strings.isNullOrEmpty(keyStorePass);
	}

	@JsonIgnore
	public String getScheme() {
		return isTlsEnabled() ? "https" : "http";
	}

}
