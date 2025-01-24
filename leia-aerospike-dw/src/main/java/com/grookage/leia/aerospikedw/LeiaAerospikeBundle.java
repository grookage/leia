package com.grookage.leia.aerospikedw;

import com.google.common.base.Preconditions;
import com.grookage.leia.aerospike.client.AerospikeConfig;
import com.grookage.leia.aerospike.repository.AerospikeRepository;
import com.grookage.leia.dropwizard.bundle.LeiaBundle;
import com.grookage.leia.dropwizard.bundle.health.LeiaHealthCheck;
import com.grookage.leia.models.user.SchemaUpdater;
import com.grookage.leia.repository.SchemaRepository;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Supplier;

@NoArgsConstructor
@Getter
public abstract class LeiaAerospikeBundle<T extends Configuration, U extends SchemaUpdater> extends LeiaBundle<T, U> {


    private AerospikeRepository aerospikeRepository;

    protected abstract AerospikeConfig getAerospikeConfig(T configuration);

    @Override
    protected Supplier<SchemaRepository> getRepositorySupplier(T configuration) {
        return () -> aerospikeRepository;
    }

    @Override
    protected List<LeiaHealthCheck> withHealthChecks(T configuration) {
        return List.of(new AerospikeHealthCheck(aerospikeRepository.getAerospikeManager()));
    }

    @Override
    public void run(T configuration, Environment environment) {
        final var aerospikeConfig = getAerospikeConfig(configuration);
        Preconditions.checkNotNull(aerospikeConfig, "Aerospike Config can't be null");
        this.aerospikeRepository = new AerospikeRepository(aerospikeConfig);
        super.run(configuration, environment);
    }
}
