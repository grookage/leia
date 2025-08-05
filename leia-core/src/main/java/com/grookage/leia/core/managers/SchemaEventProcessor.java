package com.grookage.leia.core.managers;

import com.grookage.leia.models.schema.engine.LeiaProcessorKey;
import com.grookage.leia.models.schema.engine.SchemaContext;

public interface SchemaEventProcessor {

    void preProcess(final LeiaProcessorKey leiaProcessorKey , final SchemaContext schemaContext);

    void postProcess(final LeiaProcessorKey leiaProcessorKey,final SchemaContext schemaContext );
}
