package com.grookage.leia.common.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.grookage.leia.common.violation.LeiaMessageViolation;
import com.grookage.leia.models.schema.SchemaDetails;

import java.util.List;

@FunctionalInterface
public interface LeiaMessageValidator {

    /**
     * Validate a message against schema details
     *
     * @param schemaDetails Schema to validate against
     * @param message Message to validate
     * @return List of violations, empty if valid
     */
    List<LeiaMessageViolation> validate(SchemaDetails schemaDetails, JsonNode message);
}
