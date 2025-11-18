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

package com.grookage.leia.models.schema.engine;


public enum SchemaEvent {

	CREATE_SCHEMA {
		@Override
		public <T> T accept(SchemaEventVisitor<T> eventVisitor) {
			return eventVisitor.schemaCreate();
		}
	},

	UPDATE_SCHEMA {
		@Override
		public <T> T accept(SchemaEventVisitor<T> eventVisitor) {
			return eventVisitor.schemaUpdate();
		}
	},

	APPROVE_SCHEMA {
		@Override
		public <T> T accept(SchemaEventVisitor<T> eventVisitor) {
			return eventVisitor.schemaApprove();
		}
	},

	REJECT_SCHEMA {
		@Override
		public <T> T accept(SchemaEventVisitor<T> eventVisitor) {
			return eventVisitor.schemaReject();
		}
	};

	public abstract <T> T accept(SchemaEventVisitor<T> eventVisitor);
}
