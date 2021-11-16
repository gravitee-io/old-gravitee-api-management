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
package io.gravitee.rest.api.management.rest.resource.organization;

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.management.rest.model.Pageable;
import io.gravitee.rest.api.management.rest.model.PagedResult;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.NewPreRegisterUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.validator.ValidNewPreRegisterUser;
import io.swagger.annotations.*;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Defines the REST resources to manage Users.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Users" })
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class UsersResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private UserService userService;

    @GET
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = READ))
    @ApiOperation(
        value = "Search for users using the search engine",
        notes = "User must have the ORGANIZATION_USERS[READ] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "List users matching the query criteria", response = PagedResult.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public PagedResult<UserEntity> getAllUsers(@ApiParam(name = "q") @QueryParam("q") String query, @Valid @BeanParam Pageable pageable) {
        Page<UserEntity> users = userService.search(query, pageable.toPageable());
        return new PagedResult<>(users, pageable.getSize());
    }

    @POST
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = CREATE))
    @ApiOperation(value = "Create a user", notes = "User must have the ORGANIZATION_USERS[CREATE] permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "List users matching the query criteria", response = UserEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response createUser(@ValidNewPreRegisterUser NewPreRegisterUserEntity newPreRegisterUserEntity) {
        UserEntity newUser = userService.create(newPreRegisterUserEntity);
        if (newUser != null) {
            return Response.ok().entity(newUser).build();
        }

        return Response.serverError().build();
    }

    @Path("{userId}")
    public UserResource getUserResource() {
        return resourceContext.getResource(UserResource.class);
    }

    @Path("registration")
    public UsersRegistrationResource getUsersRegistrationResource() {
        return resourceContext.getResource(UsersRegistrationResource.class);
    }
}
