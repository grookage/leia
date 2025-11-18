package com.grookage.leia.aerospike.storage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.SchemaType;
import com.grookage.leia.models.schema.engine.SchemaState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AerospikeRecord {

	@NotNull SchemaKey schemaKey;
	@NotNull SchemaState schemaState;
	SchemaType schemaType;
	@NotNull byte[] data;

	@JsonIgnore
	public String getReferenceId() {
		return schemaKey.getReferenceId();
	}
}
