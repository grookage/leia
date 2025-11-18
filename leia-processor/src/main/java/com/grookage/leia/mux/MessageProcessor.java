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

package com.grookage.leia.mux;

import com.grookage.leia.models.mux.LeiaMessage;
import com.grookage.leia.mux.filter.BackendFilter;
import com.grookage.leia.mux.filter.NoOpBackendFilter;

import java.util.List;

public interface MessageProcessor {

	String getName();

	@Deprecated(forRemoval = true, since = "1.1.0")
	default void processMessages(List<LeiaMessage> messages) {
		processMessages(messages, new NoOpBackendFilter());
	}

	void processMessages(List<LeiaMessage> messages, BackendFilter backendFilter);
}
