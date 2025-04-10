package com.grookage.leia.aerospike.repository;

import com.grookage.leia.aerospike.client.AerospikeConfig;
import com.grookage.leia.aerospike.client.AerospikeManager;
import com.grookage.leia.aerospike.storage.AerospikeRecord;
import com.grookage.leia.models.request.SearchRequest;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.engine.SchemaState;
import com.grookage.leia.models.utils.MapperUtils;
import com.grookage.leia.repository.SchemaRepository;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Optional;

@Getter
public class AerospikeRepository implements SchemaRepository {

    private final AerospikeManager aerospikeManager;

    public AerospikeRepository(AerospikeConfig aerospikeConfig) {
        this.aerospikeManager = new AerospikeManager(aerospikeConfig);
    }

    @SneakyThrows
    private AerospikeRecord toStorageRecord(SchemaDetails schemaDetails) {
        return AerospikeRecord.builder()
                .data(MapperUtils.mapper().writeValueAsBytes(schemaDetails))
                .schemaState(schemaDetails.getSchemaState())
                .schemaKey(schemaDetails.getSchemaKey())
                .build();
    }

    @SneakyThrows
    private SchemaDetails toSchemaDetails(AerospikeRecord aerospikeRecord) {
        return MapperUtils.mapper().readValue(aerospikeRecord.getData(), SchemaDetails.class);
    }


    @Override
    public boolean createdRecordExists(SchemaKey schemaKey) {
        return aerospikeManager.exists(
                schemaKey.getOrgId(),
                schemaKey.getNamespace(),
                schemaKey.getTenantId(),
                schemaKey.getSchemaName(),
                SchemaState.CREATED.name()
        );
    }

    @Override
    public boolean recordExists(SchemaKey schemaKey) {
        return aerospikeManager.exists(schemaKey.getReferenceId());
    }

    @Override
    public void create(SchemaDetails schemaDetails) {
        aerospikeManager.save(toStorageRecord(schemaDetails));
    }

    @Override
    public void update(SchemaDetails schemaDetails) {
        aerospikeManager.save(toStorageRecord(schemaDetails));
    }

    @Override
    public Optional<SchemaDetails> get(SchemaKey schemaKey) {
        return aerospikeManager.getRecord(schemaKey.getReferenceId())
                .map(this::toSchemaDetails);
    }

    @Override
    public List<SchemaDetails> getSchemas(SearchRequest searchRequest) {
        return aerospikeManager.getRecords(searchRequest)
                .stream().map(this::toSchemaDetails).toList();
    }
}
