package com.grookage.leia.es.dropwizard;



import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.grookage.conceirge.dwserver.health.ConciergeHealthCheck;
import com.grookage.concierge.elastic.config.ElasticConfig;
import com.grookage.concierge.elastic.repository.ElasticRepository;
import com.grookage.concierge.elasticdw.ElasticHealthCheck;
import com.grookage.concierge.models.ConfigUpdater;
import com.grookage.concierge.repository.ConciergeRepository;
import com.grookage.leia.dropwizard.bundle.LeiaBundle;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Supplier;

@NoArgsConstructor
@Getter
public abstract class LeiaElasticBundle<T extends Configuration, U extends ConfigUpdater> extends LeiaBundle<T, U> {

    private ElasticConfig elasticConfig;
    private ElasticsearchClient elasticsearchClient;
    private ElasticRepository elasticRepository;

    protected abstract ElasticConfig getElasticConfig(T configuration);

    @Override
    protected Supplier<ConciergeRepository> getRepositorySupplier(T configuration) {
        return () -> elasticRepository;
    }
    @Override
    protected List<ConciergeHealthCheck> withHealthChecks(T configuration) {
        return List.of(new ElasticHealthCheck(elasticConfig, elasticsearchClient));
    }

    @Override
    public void run(T configuration, Environment environment) {
        this.elasticConfig = getElasticConfig(configuration);
        this.elasticRepository = new ElasticRepository(elasticConfig);
        this.elasticsearchClient = elasticRepository.getClient();
        super.run(configuration, environment);
    }
}