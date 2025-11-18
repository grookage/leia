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

package com.grookage.leia.mux.executor;

import com.grookage.leia.models.mux.LeiaMessage;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

@Slf4j
@NoArgsConstructor
public abstract class MessageExecutor {

	public abstract String getName();

	public Set<Class<?>> getDroppableExceptions() {
		return Set.of();
	}

	public abstract void sendEnvelope(List<LeiaMessage> messages);

	public boolean isExceptionIgnorable(Throwable t) {
		return getDroppableExceptions().stream()
				.anyMatch(exceptionType -> exceptionType.isAssignableFrom(t.getClass()));
	}

	public void send(List<LeiaMessage> messages) {
		try {
			sendEnvelope(messages);
		} catch (Exception e) {
			log.error("There is an error trying to send the messages to executor name {}. Trying the exception handler", getName());
			final var exceptionIgnorable = isExceptionIgnorable(e);
			if (exceptionIgnorable) {
				log.debug("The exception occurred has been marked as ignorable, ignoring the exception processing", e);
			} else {
				handleException(messages, e);
			}
		}
	}

	public abstract void handleException(List<LeiaMessage> messages, Exception exception);
}
