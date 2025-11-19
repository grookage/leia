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

package com.grookage.leia.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grookage.leia.client.refresher.LeiaClientRefresher;
import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.validator.LeiaSchemaValidator;
import lombok.Builder;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

class SchemaClientTest {

    @Test
    @SneakyThrows
    void testSchemaClient() {
        final var clientRefresher = Mockito.mock(LeiaClientRefresher.class);
        final var schemaValidator = Mockito.mock(LeiaSchemaValidator.class);
        final var schemaClient = TestableSchemaClient.builder()
                .refresher(clientRefresher)
                .schemaValidator(schemaValidator)
                .build();
        Assertions.assertNull(schemaClient.getSchemaDetails());
        final var requestKey = SchemaKey.builder()
                        .build();
        Assertions.assertThrows(IllegalStateException.class, () -> schemaClient.getSchemaDetails(Set.of(requestKey)));
        Assertions.assertFalse(schemaClient.valid(SchemaKey.builder().build()));
        final var schemaDetails = ResourceHelper
                .getResource("schema/schemaDetails.json", SchemaDetails.class);
        Assertions.assertNotNull(schemaDetails);
        final var schemaKey = schemaDetails.getSchemaKey();
        Mockito.when(clientRefresher.getData()).thenReturn(List.of(schemaDetails));
        final var details = schemaClient.getSchemaDetails();
        Assertions.assertNotNull(details);
        Assertions.assertTrue(schemaClient.getSchemaDetails(Set.of()).isEmpty());
        Assertions.assertFalse(schemaClient.valid(schemaKey));
    }

    static class TestableSchemaClient extends AbstractSchemaClient {

        @Builder
        public TestableSchemaClient(LeiaClientRefresher refresher, LeiaSchemaValidator schemaValidator) {
            super(new ObjectMapper(), refresher, schemaValidator);
        }

        @Override
        public void start() {
            //Noop
        }
    }
}
