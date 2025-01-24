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

package com.grookage.leia.core.ingestion.utils;

import com.grookage.leia.core.exception.LeiaSchemaErrorCode;
import com.grookage.leia.models.exception.LeiaException;
import com.grookage.leia.models.schema.engine.SchemaContext;
import com.grookage.leia.models.user.SchemaUpdater;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.util.function.Supplier;

@UtilityClass
public class ContextUtils {

    private static final String USER = "USER";
    private static final String EMAIL = "EMAIL";
    private static final String USER_ID = "USER_ID";

    public static void addSchemaUpdaterContext(final SchemaContext schemaContext,
                                               final SchemaUpdater schemaUpdater) {
        schemaContext.addContext(USER, schemaUpdater.name());
        schemaContext.addContext(EMAIL, schemaUpdater.email());
        schemaContext.addContext(USER_ID, schemaUpdater.userId());
    }

    @SneakyThrows
    public static String getUser(final SchemaContext schemaContext) {
        return schemaContext.getValue(USER)
                .orElseThrow((Supplier<Throwable>) () -> LeiaException.error(LeiaSchemaErrorCode.VALUE_NOT_FOUND));
    }

    @SneakyThrows
    public static String getEmail(final SchemaContext schemaContext) {
        return schemaContext.getValue(EMAIL)
                .orElseThrow((Supplier<Throwable>) () -> LeiaException.error(LeiaSchemaErrorCode.VALUE_NOT_FOUND));
    }

    @SneakyThrows
    public static String getUserId(final SchemaContext schemaContext) {
        return schemaContext.getValue(USER_ID)
                .orElseThrow((Supplier<Throwable>) () -> LeiaException.error(LeiaSchemaErrorCode.VALUE_NOT_FOUND));
    }
}
