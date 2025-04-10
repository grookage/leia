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

package com.grookage.leia.models.request;

import com.grookage.leia.models.ResourceHelper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SearchRequestTest {

    @Test
    @SneakyThrows
    void testNamespaceRequest() {
        final var namespaceRequest = ResourceHelper.getResource("request/searchRequest.json", SearchRequest.class);
        Assertions.assertNotNull(namespaceRequest);
        Assertions.assertTrue(namespaceRequest.getNamespaces().contains("test"));
        Assertions.assertTrue(namespaceRequest.getOrgIds().contains("testOrgId"));
    }

    @Test
    @SneakyThrows
    void testNamespaceRequestAlias() {
        final var namespaceRequest = ResourceHelper.getResource("request/searchRequestAlias.json", SearchRequest.class);
        Assertions.assertNotNull(namespaceRequest);
        Assertions.assertTrue(namespaceRequest.getNamespaces().contains("test"));
        Assertions.assertTrue(namespaceRequest.getOrgIds().contains("testOrg"));
    }
}
