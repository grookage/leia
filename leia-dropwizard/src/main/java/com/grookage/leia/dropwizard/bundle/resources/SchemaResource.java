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

package com.grookage.leia.dropwizard.bundle.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.grookage.leia.core.retrieval.SchemaRetriever;
import com.grookage.leia.models.GenericResponse;
import com.grookage.leia.models.request.NamespaceRequest;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Singleton
@Getter
@Setter
@Path("/v1/schema")
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AllArgsConstructor
public class SchemaResource {

    private final SchemaRetriever schemaRetriever;

    @POST
    @Timed
    @ExceptionMetered
    @Path("/details")
    public GenericResponse<SchemaDetails> getSchemaDetails(@Valid final SchemaKey schemaKey) {
        return GenericResponse.<SchemaDetails>builder()
                .success(true)
                .data(schemaRetriever.getSchemaDetails(schemaKey).orElse(null))
                .build();
    }

    @POST
    @Timed
    @ExceptionMetered
    @Path("/details/current")
    public List<SchemaDetails> getCurrentSchemaDetails(@Valid final NamespaceRequest namespaceRequest) {
        return schemaRetriever.getCurrentSchemaDetails(namespaceRequest.getNamespaces());
    }

    @POST
    @Timed
    @ExceptionMetered
    @Path("/details/all")
    public List<SchemaDetails> getAllSchemaDetails(@Valid final NamespaceRequest namespaceRequest) {
        return schemaRetriever.getAllSchemaDetails(namespaceRequest.getNamespaces());
    }

}
