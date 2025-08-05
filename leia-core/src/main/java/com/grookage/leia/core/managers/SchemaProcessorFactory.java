package com.grookage.leia.core.managers;

import com.grookage.leia.models.schema.engine.LeiaProcessorKey;

import java.util.Optional;


public interface SchemaProcessorFactory {
    Optional<SchemaEventProcessor> getProcessor(final LeiaProcessorKey processorKey);
}