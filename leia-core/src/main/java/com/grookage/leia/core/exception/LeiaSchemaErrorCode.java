/*
 * Copyright (c) 2024-2025. Koushik R <rkoushik.14@gmail.com>.
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

package com.grookage.leia.core.exception;

import com.grookage.leia.models.exception.LeiaErrorCode;
import lombok.Getter;

@Getter
public enum LeiaSchemaErrorCode implements LeiaErrorCode {

    PROCESSOR_NOT_FOUND(400),

    VALUE_NOT_FOUND(400),

    SCHEMA_ALREADY_EXISTS(400),

    SCHEMA_APPROVAL_UNAUTHORIZED(400),

    NO_SCHEMA_FOUND(400);

    final int status;

    LeiaSchemaErrorCode(int status) {
        this.status = status;
    }
}
