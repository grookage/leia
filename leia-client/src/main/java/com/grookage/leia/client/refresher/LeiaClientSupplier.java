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

package com.grookage.leia.client.refresher;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.grookage.korg.config.KorgHttpConfiguration;
import com.grookage.korg.suppliers.KorgHttpSupplier;
import com.grookage.leia.client.datasource.LeiaClientRequest;
import com.grookage.leia.models.request.SearchRequest;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.engine.SchemaState;
import com.grookage.leia.models.utils.MapperUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@SuppressWarnings({"deprecation", "KotlinInternalInJava"})
@Getter
public class LeiaClientSupplier extends KorgHttpSupplier<List<SchemaDetails>> {

    private final Supplier<LeiaClientRequest> clientRequestSupplier;
    private final Supplier<String> authHeaderSupplier;

    @Builder
    public LeiaClientSupplier(KorgHttpConfiguration httpConfiguration,
                              Supplier<LeiaClientRequest> clientRequestSupplier,
                              Supplier<String> authHeaderSupplier) {
        super(httpConfiguration, LeiaClientMarshaller.getInstance(), "getClientNamespaces");
        this.clientRequestSupplier = clientRequestSupplier;
        this.authHeaderSupplier = authHeaderSupplier;
    }

    @Override
    protected String url() {
        return "/v1/schema/details/all";
    }

    @Override
    @SneakyThrows
    protected Request getRequest(String url) {
        final var clientRequest = clientRequestSupplier.get();
        final var requestBody = RequestBody.create(
                okhttp3.MediaType.parse("application/json; charset=utf-8"),
                MapperUtils.mapper().writeValueAsString(SearchRequest.builder()
                        .namespaces(clientRequest.getNamespaces())
                        .tenants(clientRequest.getTenants())
                        .orgIds(clientRequest.getOrgIds())
                        .schemaNames(clientRequest.getSchemaNames())
                        .states(Set.of(SchemaState.APPROVED))
                        .build()));
        final var requestBuilder = new Request.Builder()
                .url(endPoint(url))
                .post(requestBody);
        final var suppliedHeader = authHeaderSupplier.get();
        if (!Strings.isNullOrEmpty(suppliedHeader)) {
            requestBuilder.addHeader(HttpHeaders.AUTHORIZATION, suppliedHeader);
        }
        return requestBuilder.build();
    }
}
