package com.grookage.leia.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.grookage.leia.models.schema.SchemaKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidateSchemaRequest {
	@Valid
	@NotNull
	private SchemaKey schemaKey;
	@NotNull
	private JsonNode jsonNode;
}
