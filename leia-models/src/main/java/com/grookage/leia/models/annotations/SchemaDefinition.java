package com.grookage.leia.models.annotations;

import com.grookage.leia.models.schema.SchemaType;
import com.grookage.leia.models.schema.SchemaValidationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SchemaDefinition {
    String name();

    String namespace();

    String version();

    String orgId();

    String tenantId();

    String type();

    String description() default "";
    SchemaType schemaType() default SchemaType.JSON;

    SchemaValidationType validation() default SchemaValidationType.MATCHING;

    String[] tags() default {};

    Class<?>[] transformationTargets() default {};
}
