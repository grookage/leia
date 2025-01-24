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

package com.grookage.leia.dropwizard.bundle.mapper;

import com.grookage.leia.models.GenericResponse;
import com.grookage.leia.models.exception.LeiaException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class LeiaExceptionMapper implements ExceptionMapper<LeiaException> {

    @Override
    public Response toResponse(LeiaException e) {
        return Response.status(e.getStatus())
                .entity(
                        GenericResponse.builder()
                                .success(false)
                                .code(e.getCode())
                                .message(e.getMessage())
                                .data(e.getContext())
                                .build()
                )
                .build();
    }
}
