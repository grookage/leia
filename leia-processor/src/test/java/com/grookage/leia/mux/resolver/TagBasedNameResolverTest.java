/*
 * Copyright (c) 2025. Koushik R <rkoushik.14@gmail.com>.
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

package com.grookage.leia.mux.resolver;

import com.grookage.leia.models.ResourceHelper;
import com.grookage.leia.models.mux.LeiaMessage;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TagBasedNameResolverTest {

	@Test
	@SneakyThrows
	void testNameResolver() {
		final var resolver = new TagBasedNameResolver();
		final var leiaMessage = ResourceHelper.getResource("mux/leiaMessage.json", LeiaMessage.class);
		Assertions.assertNotNull(leiaMessage);
		final var eligibleBackends = resolver.getEligibleBackends(leiaMessage);
		Assertions.assertFalse(eligibleBackends.isEmpty());
		Assertions.assertEquals(2, eligibleBackends.size());
		Assertions.assertTrue(eligibleBackends.contains("BACKEND1"));
		Assertions.assertTrue(eligibleBackends.contains("BACKEND2"));
		Assertions.assertFalse(eligibleBackends.contains("BACKEND3"));
	}
}
