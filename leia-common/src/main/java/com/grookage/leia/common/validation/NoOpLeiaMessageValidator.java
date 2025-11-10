package com.grookage.leia.common.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.grookage.leia.common.violation.LeiaMessageViolation;
import com.grookage.leia.models.schema.SchemaDetails;

import java.util.Collections;
import java.util.List;

public class NoOpLeiaMessageValidator implements LeiaMessageValidator {
    @Override
    public List<LeiaMessageViolation> validate(final SchemaDetails schemaDetails, final JsonNode message) {
        return Collections.emptyList();
    }
}
