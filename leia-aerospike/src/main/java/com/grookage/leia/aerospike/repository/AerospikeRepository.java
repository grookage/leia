package com.grookage.leia.aerospike.repository;

import com.grookage.leia.aerospike.client.AerospikeConfig;
import com.grookage.leia.aerospike.client.AerospikeManager;
import com.grookage.leia.aerospike.storage.AerospikeRecord;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.engine.SchemaState;
import com.grookage.leia.models.utils.MapperUtils;
import com.grookage.leia.repository.AbstractSchemaRepository;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Getter
@EqualsAndHashCode(callSuper = true)
public class AerospikeRepository extends AbstractSchemaRepository {

    private final AerospikeManager aerospikeManager;

    public AerospikeRepository(AerospikeConfig aerospikeConfig) {
        this.aerospikeManager = new AerospikeManager(aerospikeConfig);
    }

    @SneakyThrows
    private AerospikeRecord toStorageRecord(SchemaDetails schemaDetails) {
        return AerospikeRecord.builder()
                .data(MapperUtils.mapper().writeValueAsBytes(schemaDetails))
                .histories(schemaDetails.getHistories())
                .schemaState(schemaDetails.getSchemaState())
                .version(schemaDetails.getVersion())
                .description(schemaDetails.getDescription())
                .namespace(schemaDetails.getNamespace())
                .schemaName(schemaDetails.getSchemaName())
                .schemaType(schemaDetails.getSchemaType())
                .build();
    }

    @SneakyThrows
    private SchemaDetails toConfigDetails(AerospikeRecord aerospikeRecord) {
        return MapperUtils.mapper().readValue(aerospikeRecord.getData(), SchemaDetails.class);
    }

    @Override
    public boolean createdRecordExists(String namespace, String schemaName) {
        return aerospikeManager.exists(namespace, schemaName, SchemaState.CREATED.name());
    }

    @Override
    public void create(SchemaDetails configDetails) {
        aerospikeManager.create(toStorageRecord(configDetails));
    }

    @Override
    public void update(SchemaDetails configDetails) {
        aerospikeManager.update(toStorageRecord(configDetails));
    }

    @Override
    public Optional<SchemaDetails> get(SchemaKey schemaKey) {
        return aerospikeManager.getRecord(schemaKey.getReferenceId())
                .map(this::toConfigDetails);
    }

    @Override
    public List<SchemaDetails> getSchemas(Set<String> namespaces, Set<SchemaState> schemaStates) {
        return aerospikeManager.getRecords(namespaces.stream().toList(), List.of(),
                        schemaStates.stream().map(Enum::name).toList())
                .stream().map(this::toConfigDetails).toList();
    }


}
