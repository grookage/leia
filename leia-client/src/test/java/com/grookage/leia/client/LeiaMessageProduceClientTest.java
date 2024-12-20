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
import com.grookage.leia.client.processor.MessageProcessor;
import com.grookage.leia.client.refresher.LeiaClientRefresher;
import com.grookage.leia.client.stubs.TargetSchema;
import com.grookage.leia.client.stubs.TestSchema;
import com.grookage.leia.client.stubs.TestSchemaUnit;
import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.mux.LeiaMessage;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.validator.LeiaSchemaValidator;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class LeiaMessageProduceClientTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    @SneakyThrows
    void testLeiaMessageProduceClient() {
        final var clientRefresher = Mockito.mock(LeiaClientRefresher.class);
        final var schemaValidator = Mockito.mock(LeiaSchemaValidator.class);
        final var sourceSchema = SchemaKey.builder()
                .namespace("testNamespace")
                .schemaName("testSchema")
                .version("V1234")
                .build();
        final var targetSchema = SchemaKey.builder()
                .namespace("testNamespace")
                .schemaName("testSchema")
                .version("v")
                .build();
        final var schemaDetails = ResourceHelper
                .getResource("schema/schemaDetails.json", SchemaDetails.class);
        Assertions.assertNotNull(schemaDetails);
        Mockito.when(clientRefresher.getData()).thenReturn(List.of(schemaDetails));
        Mockito.when(schemaValidator.getKlass(sourceSchema)).thenReturn(Optional.of(TestSchema.class));
        Mockito.when(schemaValidator.getKlass(targetSchema)).thenReturn(Optional.of(TargetSchema.class));
        Mockito.when(schemaValidator.valid(Mockito.any(SchemaKey.class))).thenReturn(true);
        final var schemaClient = LeiaMessageProduceClient.builder()
                .mapper(new ObjectMapper())
                .refresher(clientRefresher)
                .schemaValidator(schemaValidator)
                .messageProcessor(() -> messages -> {
                    Assertions.assertFalse(messages.isEmpty());
                    Assertions.assertEquals(2, messages.size());
                })
                .build();
        schemaClient.start();
        final var testSchema = TestSchema.builder()
                .userName("testUser")
                .schemaUnits(List.of(TestSchemaUnit.builder()
                        .registeredName("testRegisteredName").build()))
                .build();
        schemaClient.processMessages(sourceSchema, mapper.writeValueAsBytes(testSchema));
    }
}
