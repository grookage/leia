package com.grookage.leia.aerospike.storage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Joiner;
import com.grookage.leia.models.schema.SchemaHistoryItem;
import com.grookage.leia.models.schema.SchemaType;
import com.grookage.leia.models.schema.engine.SchemaState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AerospikeRecord {

    @NotBlank String namespace;
    @NotBlank String schemaName;
    @NotBlank String version;
    @NotNull SchemaState schemaState;
    SchemaType schemaType;
    String description;
    @NotNull byte[] data;
    @Builder.Default
    Set<SchemaHistoryItem> histories = new HashSet<>();

    @JsonIgnore
    public String getReferenceId() {
        return Joiner.on(".").join(namespace, schemaName, version).toUpperCase(Locale.ROOT);
    }
}
