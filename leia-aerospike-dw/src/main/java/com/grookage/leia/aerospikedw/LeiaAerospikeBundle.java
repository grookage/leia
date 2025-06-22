package com.grookage.leia.aerospikedw;

import com.google.common.base.Preconditions;
import com.grookage.concierge.aerospike.client.AerospikeConfig;
import com.grookage.concierge.models.ConfigUpdater;
import com.grookage.concierge.repository.ConciergeRepository;
import com.grookage.concierge.aerospike.repository.AerospikeRepository;
import com.grookage.leia.dropwizard.bundle.LeiaBundle;
import com.grookage.leia.dropwizard.bundle.health.LeiaHealthCheck;
import com.grookage.leia.models.user.SchemaUpdater;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Supplier;

@NoArgsConstructor
@Getter
public abstract class LeiaAerospikeBundle<T extends Configuration, U extends SchemaUpdater & ConfigUpdater> extends LeiaBundle<T, U> {

private AerospikeRepository aerospikeRepository;


    protected abstract AerospikeConfig getAerospikeConfig(T configuration);

    @Override
    protected Supplier<ConciergeRepository> getRepositorySupplier(T configuration) {
        return () -> aerospikeRepository;
    }





    @Override
    public void run(T configuration, Environment environment) {
        final var aerospikeConfig = getAerospikeConfig(configuration);
        Preconditions.checkNotNull(aerospikeConfig, "Aerospike Config can't be null");
        this.aerospikeRepository = new AerospikeRepository(aerospikeConfig);
        super.run(configuration, environment);
    }
}
