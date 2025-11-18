package com.grookage.leia.models.mux;

import com.fasterxml.jackson.databind.JsonNode;
import com.grookage.leia.models.schema.SchemaKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class MessageRequest {
	boolean includeSource;
	@NotEmpty
	private SchemaKey schemaKey;
	@NotNull
	private JsonNode message;
}
