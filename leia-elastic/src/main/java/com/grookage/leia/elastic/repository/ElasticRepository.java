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

package com.grookage.leia.elastic.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.grookage.leia.elastic.client.ElasticClientManager;
import com.grookage.leia.elastic.config.ElasticConfig;
import com.grookage.leia.elastic.storage.StoredElasticRecord;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.engine.SchemaState;
import com.grookage.leia.repository.SchemaRepository;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class ElasticRepository implements SchemaRepository {

    private static final String DEFAULT_SCHEMA_INDEX = "schemas";
    private static final String NAMESPACE = "namespace";
    private static final String ORG = "orgId";
    private static final String TENANT = "tenantId";
    private static final String SCHEMA_NAME = "schemaName";
    private static final String SCHEMA_STATE = "schemaState";
    private final ElasticsearchClient client;
    private final ElasticConfig elasticConfig;
    private final String schemaIndex;

    public ElasticRepository(ElasticConfig elasticConfig) {
        super();
        Preconditions.checkNotNull(elasticConfig, "Elastic Config can't be null");
        this.elasticConfig = elasticConfig;
        this.client = new ElasticClientManager(elasticConfig).getElasticClient();
        this.schemaIndex = !Strings.isNullOrEmpty(elasticConfig.getSchemaIndex())
                ? elasticConfig.getSchemaIndex()
                : DEFAULT_SCHEMA_INDEX;
        this.initialize();
    }

    @SneakyThrows
    private void initialize() {
        final var indexExists = client.indices()
                .exists(co.elastic.clients.elasticsearch.indices.ExistsRequest.of(s -> s.index(schemaIndex)))
                .value();
        if (!indexExists) {
            final var registryInitialized = client.indices().create(CreateIndexRequest.of(idx -> idx.index(schemaIndex)
                    .settings(IndexSettings.of(s -> s.numberOfShards("1")
                            .numberOfReplicas("2"))))
            ).shardsAcknowledged();
            if (!registryInitialized) {
                throw new IllegalStateException("Registry index creation seems to have failed, please try again!");
            }
        }
    }

    private FieldValue getFieldValue(final String value) {
        return FieldValue.of(value.toLowerCase(Locale.ROOT));
    }

    private List<FieldValue> getFieldValues(final Set<String> values) {
        return values.stream().map(this::getFieldValue).toList();
    }

    private StoredElasticRecord toStorageRecord(final SchemaDetails schemaDetails) {
        final var schemaKey = schemaDetails.getSchemaKey();
        return StoredElasticRecord.builder()
                .orgId(schemaKey.getOrgId())
                .namespace(schemaKey.getNamespace())
                .tenantId(schemaKey.getTenantId())
                .schemaName(schemaKey.getSchemaName())
                .type(schemaKey.getType())
                .version(schemaKey.getVersion())
                .schemaState(schemaDetails.getSchemaState())
                .schemaType(schemaDetails.getSchemaType())
                .description(schemaDetails.getDescription())
                .validationType(schemaDetails.getValidationType())
                .attributes(schemaDetails.getAttributes())
                .transformationTargets(schemaDetails.getTransformationTargets())
                .histories(schemaDetails.getHistories())
                .tags(schemaDetails.getTags())
                .build();
    }

    private SchemaDetails toSchemaDetails(final StoredElasticRecord storedElasticRecord) {
        return SchemaDetails.builder()
                .schemaKey(storedElasticRecord.getSchemaKey())
                .schemaState(storedElasticRecord.getSchemaState())
                .schemaType(storedElasticRecord.getSchemaType())
                .description(storedElasticRecord.getDescription())
                .validationType(storedElasticRecord.getValidationType())
                .attributes(storedElasticRecord.getAttributes())
                .transformationTargets(storedElasticRecord.getTransformationTargets())
                .histories(storedElasticRecord.getHistories())
                .tags(storedElasticRecord.getTags())
                .build();
    }

    @Override
    @SneakyThrows
    public boolean createdRecordExists(SchemaKey schemaKey) {
        final var orgQuery = TermQuery.of(p -> p.field(ORG).value(getFieldValue(schemaKey.getOrgId())))._toQuery();
        final var namespaceQuery = TermQuery.of(p -> p.field(NAMESPACE).value(getFieldValue(schemaKey.getNamespace())))._toQuery();
        final var tenantQuery = TermQuery.of(p -> p.field(TENANT).value(getFieldValue(schemaKey.getTenantId())))._toQuery();
        final var schemaQuery = TermQuery.of(p -> p.field(SCHEMA_NAME).value(getFieldValue(schemaKey.getSchemaName())))._toQuery();
        final var schemaStateQuery = TermQuery.of(p -> p.field(SCHEMA_STATE).value(getFieldValue(SchemaState.CREATED.name())))._toQuery();
        final var searchQuery = BoolQuery.of(q -> q.must(List.of(orgQuery, namespaceQuery, tenantQuery, schemaQuery, schemaStateQuery
        )))._toQuery();
        final var searchResponse = client.search(SearchRequest.of(s -> s.query(searchQuery)
                        .requestCache(true)
                        .index(List.of(schemaIndex))
                        .size(elasticConfig.getMaxResultSize()) //If you have more than 10K schemas, this will hold you up!
                        .timeout(elasticConfig.getTimeout())),
                StoredElasticRecord.class
        );
        return !searchResponse.hits().hits().isEmpty();
    }

    @SneakyThrows
    @Override
    public boolean recordExists(SchemaKey schemaKey) {
        return client.exists(ExistsRequest.of(request -> request.index(schemaIndex).id(schemaKey.getReferenceId())))
                .value();
    }

    @Override
    @SneakyThrows
    public void create(SchemaDetails schema) {
        final var createDocument = new IndexRequest.Builder<>()
                .document(toStorageRecord(schema))
                .index(schemaIndex)
                .refresh(Refresh.WaitFor)
                .id(schema.getReferenceId())
                .timeout(Time.of(s -> s.time(elasticConfig.getTimeout())))
                .build();
        client.index(createDocument);
    }

    @Override
    @SneakyThrows
    public void update(SchemaDetails schema) {
        final var updateRequest = new UpdateRequest.Builder<>()
                .index(schemaIndex)
                .id(schema.getReferenceId())
                .doc(toStorageRecord(schema))
                .refresh(Refresh.WaitFor)
                .timeout(Time.of(s -> s.time(elasticConfig.getTimeout())))
                .build();
        client.update(updateRequest, StoredElasticRecord.class);
    }

    @Override
    @SneakyThrows
    public Optional<SchemaDetails> get(SchemaKey schemaKey) {
        final var getResponse = client.get(GetRequest.of(request -> request.index(schemaIndex).id(schemaKey.getReferenceId())), StoredElasticRecord.class);
        return Optional.ofNullable(getResponse.source()).map(this::toSchemaDetails);
    }

    @Override
    @SneakyThrows
    public List<SchemaDetails> getSchemas(final com.grookage.leia.models.request.SearchRequest searchRequest) {
        final var filters = new ArrayList<Query>();
        if (!searchRequest.getOrgIds().isEmpty()) {
            filters.add(TermsQuery.of(q -> q.field(ORG)
                    .terms(t -> t.value(getFieldValues(searchRequest.getOrgIds()))
                    ))._toQuery());
        }
        if (!searchRequest.getNamespaces().isEmpty()) {
            filters.add(TermsQuery.of(q -> q.field(NAMESPACE)
                    .terms(t -> t.value(getFieldValues(searchRequest.getNamespaces()))
                    ))._toQuery());
        }
        if (!searchRequest.getTenants().isEmpty()) {
            filters.add(TermsQuery.of(q -> q.field(TENANT)
                    .terms(t -> t.value(getFieldValues(searchRequest.getTenants()))
                    ))._toQuery());
        }
        if (!searchRequest.getSchemaNames().isEmpty()) {
            filters.add(TermsQuery.of(q -> q.field(SCHEMA_NAME)
                    .terms(t -> t.value(getFieldValues(searchRequest.getSchemaNames()))
                    ))._toQuery());
        }
        if (!searchRequest.getStates().isEmpty()) {
            filters.add(TermsQuery.of(q -> q.field(SCHEMA_STATE)
                    .terms(t -> t.value(getFieldValues(searchRequest.getStates().stream()
                            .map(Enum::name)
                            .collect(Collectors.toSet())))
                    ))._toQuery());
        }
        final var searchQuery = filters.isEmpty()
                ? MatchAllQuery.of(q -> q)._toQuery()
                : BoolQuery.of(q -> q.must(filters))._toQuery();
        final var searchResponse = client.search(SearchRequest.of(
                        s -> s.query(searchQuery)
                                .requestCache(true)
                                .index(List.of(schemaIndex))
                                .size(elasticConfig.getMaxResultSize()) //If you have more than 10K schemas, this will hold you up!
                                .timeout(elasticConfig.getTimeout())),
                StoredElasticRecord.class
        );
        return searchResponse.hits()
                .hits()
                .stream()
                .map(each -> toSchemaDetails(Objects.requireNonNull(each.source()))).toList();
    }
}
