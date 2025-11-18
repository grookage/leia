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

package com.grookage.leia.elastic.utils;

import com.grookage.leia.elastic.config.ElasticConfig;
import lombok.experimental.UtilityClass;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@UtilityClass
public class ElasticClientUtils {

	public static CredentialsProvider getAuthCredentials(ElasticConfig configuration) {
		if (!configuration.getAuthConfig().isEnabled()) return null;
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(
				AuthScope.ANY,
				new UsernamePasswordCredentials(
						configuration.getAuthConfig().getUsername(),
						configuration.getAuthConfig().getPassword()
				)
		);
		return credentialsProvider;
	}

	public static SSLContext getSslContext(ElasticConfig configuration)
			throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, KeyManagementException {
		if (!configuration.getAuthConfig().isTlsEnabled()) return null;
		final var trustStorePath = Paths.get(configuration.getAuthConfig().getTrustStorePath());
		final var truststore = KeyStore.getInstance(configuration.getAuthConfig().getKeyStoreType());
		try (final var is = Files.newInputStream(trustStorePath)) {
			truststore.load(is, configuration.getAuthConfig().getKeyStorePass().toCharArray());
		}
		return SSLContexts.custom()
				.loadTrustMaterial(truststore, null)
				.build();
	}

	public static String getScheme(ElasticConfig configuration) {
		if (null == configuration.getAuthConfig()) return "http";
		return configuration.getAuthConfig().getScheme();
	}

}
