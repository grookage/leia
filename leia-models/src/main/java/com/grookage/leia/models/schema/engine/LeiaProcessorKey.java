package com.grookage.leia.models.schema.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.grookage.leia.models.schema.SchemaKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeiaProcessorKey {
    private SchemaKey schemaKey;

    private SchemaEvent schemaEvent;
}
