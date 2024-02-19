/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.core.exceptionmapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.data.ApiGlobalErrorResponse;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * An {@link ExceptionMapper} to map {@link PlatformApiDataValidationException} thrown by platform into a HTTP API
 * friendly format.
 *
 * The {@link PlatformApiDataValidationException} is typically thrown in data validation of the parameters passed in
 * with an api request.
 */
@Provider
@Component
@Scope("singleton")
@Slf4j
public class PlatformApiDataValidationExceptionMapper
        implements FineractExceptionMapper, ExceptionMapper<PlatformApiDataValidationException> {

    @Override
    public Response toResponse(final PlatformApiDataValidationException exception) {
        log.warn("Exception occurred", exception);
        final ApiGlobalErrorResponse dataValidationErrorResponse = ApiGlobalErrorResponse
                .badClientRequest(exception.getGlobalisationMessageCode(), exception.getDefaultUserMessage(), exception.getErrors());

        return Response.status(Status.BAD_REQUEST).entity(dataValidationErrorResponse).type(MediaType.APPLICATION_JSON).build();
    }

    @Override
    public int errorCode() {
        return 2002;
    }
}
