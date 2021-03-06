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
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.validator.CustomApiKey;
import io.swagger.annotations.*;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 *
 * @deprecated Use ApiSubscriptionKeysResource instead
 */
@Deprecated(forRemoval = true, since = "v3.12")
@Api(tags = { "API Keys" })
public class ApiKeysResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiKeyService apiKeyService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @ApiParam(name = "api", hidden = true)
    private String api;

    @Deprecated(forRemoval = true, since = "v3.12")
    @DELETE
    @Path("{key}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Deprecated, use DELETE /apis/{apiId}/subscriptions/{subscriptionId}/apikeys/{keyId} endpoint instead",
        notes = "User must have the API_SUBSCRIPTION:DELETE permission to use this service"
    )
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.DELETE) })
    public Response revokeApiKey(@PathParam("key") @ApiParam("The API key") String apiKey) {
        apiKeyService.revoke(apiKey, api, true);

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Deprecated(forRemoval = true, since = "v3.12")
    @PUT
    @Path("{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Deprecated, use PUT /apis/{apiId}/subscriptions/{subscriptionId}/apikeys/{keyId} endpoint instead",
        notes = "User must have the API_SUBSCRIPTION:UPDATE permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "API Key successfully updated", response = ApiKeyEntity.class),
            @ApiResponse(code = 400, message = "Bad plan format"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response updateApiKey(@PathParam("key") @ApiParam("The API key") String apiKey, @Valid @NotNull ApiKeyEntity apiKeyEntity) {
        if (apiKeyEntity.getKey() != null && !apiKey.equals(apiKeyEntity.getKey())) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("'apiKey' parameter does not correspond to the api-key to update")
                .build();
        }

        // Force API Key
        apiKeyEntity.setKey(apiKey);

        ApiKeyEntity keyEntity = apiKeyService.update(apiKeyEntity);
        return Response.ok(keyEntity).build();
    }

    @Deprecated(forRemoval = true, since = "v3.12")
    @POST
    @Path("_verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Deprecated, use GET /apis/{apiId}/subscriptions/_canCreate endpoint instead",
        notes = "User must have the API_SUBSCRIPTION:READ permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "API Key successfully checked", response = Boolean.class),
            @ApiResponse(code = 400, message = "Bad API Key parameter"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public Response verifyApiKeyAvailability(
        @ApiParam(name = "apiKey", required = true) @CustomApiKey @NotNull @QueryParam("apiKey") String apiKey
    ) {
        boolean available = apiKeyService.findByKey(apiKey).isEmpty();
        return Response.ok(available).build();
    }
}
