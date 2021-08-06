/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.FetcherEntity;
import io.gravitee.rest.api.service.FetcherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Plugins" })
public class FetcherResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private FetcherService fetcherService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a fetcher plugin")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Fetcher", response = FetcherEntity.class),
            @ApiResponse(code = 404, message = "Fetcher not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public FetcherEntity getFetcher(@PathParam("fetcher") String fetcher) {
        return fetcherService.findById(fetcher);
    }

    @GET
    @Path("schema")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a fetcher plugin's schema")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Fetcher's schema"),
            @ApiResponse(code = 404, message = "Fetcher not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public String getFetcherSchema(@PathParam("fetcher") String fetcher) {
        // Check that the fetcher exists
        fetcherService.findById(fetcher);

        return fetcherService.getSchema(fetcher);
    }
}
