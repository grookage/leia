/*
 * Copyright (c) 2024. Koushik R <rkoushik.14@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.grookage.leia.validator;

import com.google.inject.Injector;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.validator.annotations.SchemaValidator;
import com.grookage.leia.validator.exception.SchemaValidationException;
import com.grookage.leia.validator.exception.ValidationErrorCode;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
public class InjectableSchemaValidator implements LeiaSchemaValidator {

    private final ConcurrentHashMap<SchemaKey, Boolean> validationRegistry = new ConcurrentHashMap<>();
    private final Supplier<List<SchemaDetails>> supplier;
    private final Set<String> packageRoots;
    private final Injector injector;

    @Builder
    public InjectableSchemaValidator(Supplier<List<SchemaDetails>> supplier,
                                     Set<String> packageRoots,
                                     Injector injector) {
        this.supplier = supplier;
        this.packageRoots = packageRoots;
        this.injector = injector;
    }

    public Optional<SchemaDetails> getSchemaDetails(final SchemaKey schemaKey) {
        return supplier.get().stream()
                .filter(each -> each.match(schemaKey)).findFirst();
    }

    @SneakyThrows
    private <T extends SchemaValidatable> boolean validate(final SchemaKey schemaKey, T data) {
        final var details = getSchemaDetails(schemaKey).orElse(null);
        if (null == details) {
            throw SchemaValidationException.error(ValidationErrorCode.NO_SCHEMA_FOUND);
        }
        return true; //TODO:: Run the validator against schemaDetails here.
    }

    @Override
    public void start() {
        log.info("Starting the schema validator");
        packageRoots.forEach(handlerPackage -> {
            final var reflections = new Reflections(handlerPackage);
            final var annotatedClasses = reflections.getTypesAnnotatedWith(SchemaValidator.class);
            annotatedClasses.forEach(annotatedClass -> {
                if (SchemaValidatable.class.isAssignableFrom(annotatedClass)) {
                    final var instance = (SchemaValidatable) injector.getInstance(annotatedClass);
                    final var schemaKey = SchemaKey.builder()
                            .schemaName(instance.schemaName())
                            .version(instance.versionId())
                            .namespace(instance.namespace())
                            .build();
                    validationRegistry.putIfAbsent(schemaKey, validate(schemaKey, instance));
                }
            });
        });
    }

    @Override
    public void stop() {
        log.info("Stopping the schema validator");
    }

    @Override
    public boolean valid(SchemaKey schemaKey) {
        return validationRegistry.get(schemaKey);
    }
}