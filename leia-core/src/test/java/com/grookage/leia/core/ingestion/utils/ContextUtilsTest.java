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

import com.grookage.leia.models.exception.LeiaException;
import com.grookage.leia.models.schema.engine.SchemaContext;
import com.grookage.leia.models.user.SchemaUpdater;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ContextUtilsTest {

    @Test
    @SneakyThrows
    void testContextUtils() {
        Assertions.assertThrows(LeiaException.class, () -> ContextUtils.getEmail(new SchemaContext()));
        Assertions.assertThrows(LeiaException.class, () -> ContextUtils.getUser(new SchemaContext()));
        final var schemaContext = new SchemaContext();
        ContextUtils.addSchemaUpdaterContext(schemaContext, new SchemaUpdater() {
            @Override
            public String name() {
                return "name";
            }

            @Override
            public String email() {
                return "email";
            }

            @Override
            public String userId() {
                return "nameId";
            }

        });
        Assertions.assertNotNull(ContextUtils.getEmail(schemaContext));
        Assertions.assertNotNull(ContextUtils.getUser(schemaContext));
    }
}
