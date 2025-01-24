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
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.google.common.base.Preconditions;
import com.grookage.leia.elastic.client.ElasticClientManager;
import com.grookage.leia.elastic.config.ElasticConfig;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.engine.SchemaState;
import com.grookage.leia.repository.AbstractSchemaRepository;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class ElasticRepository extends AbstractSchemaRepository {
    private static final String SCHEMA_INDEX = "schema_registry";
    private static final String NAMESPACE = "namespace";
    private static final String SCHEMA_NAME = "schemaName";
    private static final String VERSION = "version";
    private static final String SCHEMA_STATE = "schemaState";
    private final ElasticsearchClient client;
    private final ElasticConfig elasticConfig;

    public ElasticRepository(ElasticConfig elasticConfig) {
        super();
        Preconditions.checkNotNull(elasticConfig, "Elastic Config can't be null");
        this.elasticConfig = elasticConfig;
        this.client = new ElasticClientManager(elasticConfig).getElasticClient();
        this.initialize();
    }

    /* Fields and values are being lower-cased, before adding as clauses, since elasticsearch deals with lowercase only */
    private List<FieldValue> getNormalizedValues(Set<String> terms) {
        return terms.stream().map(FieldValue::of).toList();
    }

    @SneakyThrows
    private void initialize() {
        final var indexExists = client.indices()
                .exists(ExistsRequest.of(s -> s.index(SCHEMA_INDEX)))
                .value();
        if (!indexExists) {
            final var registryInitialized = client.indices().create(CreateIndexRequest.of(idx -> idx.index(SCHEMA_INDEX)
                    .settings(IndexSettings.of(s -> s.numberOfShards("1")
                            .numberOfReplicas("2"))))
            ).shardsAcknowledged();
            if (!registryInitialized) {
                throw new IllegalStateException("Registry index creation seems to have failed, please try again!");
            }
        }
    }

    @Override
    public boolean createdRecordExists(String namespace, String schemaName) {
        final var namespaceQuery = TermQuery.of(p -> p.field(NAMESPACE).value(namespace))._toQuery();
        final var configQuery = TermQuery.of(p -> p.field(SCHEMA_NAME).value(schemaName))._toQuery();
        final var configStateQuery = TermQuery.of(p -> p.field(SCHEMA_STATE).value(SchemaState.CREATED.name()))._toQuery();
        final var searchQuery = BoolQuery.of(q -> q.must(List.of(namespaceQuery, configQuery, configStateQuery)))._toQuery();
        final var searchResponse = client.search(SearchRequest.of(
                        s -> s.query(searchQuery)
                                .requestCache(true)
                                .index(List.of(SCHEMA_INDEX))
                                .size(elasticConfig.getMaxResultSize()) //If you have more than 10K schemas, this will hold you up!
                                .timeout(elasticConfig.getTimeout())),
                SchemaDetails.class
        );
        return !searchResponse.hits().hits().isEmpty();
    }

    @Override
    @SneakyThrows
    public void create(SchemaDetails schema) {
        final var createDocument = new IndexRequest.Builder<>().document(schema)
                .index(SCHEMA_INDEX)
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
                .index(SCHEMA_INDEX)
                .id(schema.getReferenceId())
                .doc(schema)
                .refresh(Refresh.WaitFor)
                .timeout(Time.of(s -> s.time(elasticConfig.getTimeout())))
                .build();
        client.update(updateRequest, SchemaDetails.class);
    }

    @Override
    @SneakyThrows
    public Optional<SchemaDetails> get(SchemaKey schemaKey) {
        final var getResponse = client.get(GetRequest.of(request -> request.index(SCHEMA_INDEX).id(schemaKey.getReferenceId())), SchemaDetails.class);
        return Optional.ofNullable(getResponse.source());
    }

    @Override
    @SneakyThrows
    public List<SchemaDetails> getSchemas(final Set<String> namespaces,
                                          final Set<SchemaState> schemaStates) {
        final var namespaceQuery = namespaces.isEmpty() ?
                MatchAllQuery.of(q -> q)._toQuery() :
                TermsQuery.of(q -> q.field(NAMESPACE).terms(t -> t.value(getNormalizedValues(namespaces))
                ))._toQuery();
        final var stateQuery = schemaStates.isEmpty() ?
                MatchAllQuery.of(q -> q)._toQuery() :
                TermsQuery.of(q -> q.field(SCHEMA_STATE)
                        .terms(t -> t.value(getNormalizedValues(schemaStates.stream().map(Enum::name).collect(Collectors.toSet())))))._toQuery();
        final var searchQuery = BoolQuery.of(q -> q.must(List.of(namespaceQuery, stateQuery)))._toQuery();
        final var searchResponse = client.search(SearchRequest.of(
                        s -> s.query(searchQuery)
                                .requestCache(true)
                                .index(List.of(SCHEMA_INDEX))
                                .size(elasticConfig.getMaxResultSize()) //If you have more than 10K schemas, this will hold you up!
                                .timeout(elasticConfig.getTimeout())),
                SchemaDetails.class
        );
        return searchResponse.hits().hits().stream().map(Hit::source).toList();
    }
}
