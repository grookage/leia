package com.grookage.leia.models.utils;

import com.google.common.base.Joiner;
import com.grookage.leia.models.schema.SchemaKey;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MetricUtils {

    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";
    public static final String TRANSFORMATION = "transformation";
    public static final String MESSAGE = "message";
    public static final String SEND = "send";
    public static final String PREFIX = "com.grookage.leia";
    public static final String SKIPPED = "skipped";

    public static String getMetricKey(SchemaKey schemaKey){
        return Joiner.on(".").useForNull("null")
                .join(schemaKey.getOrgId(),schemaKey.getNamespace(),
                        schemaKey.getTenantId(),schemaKey.getSchemaName());
    }

}
