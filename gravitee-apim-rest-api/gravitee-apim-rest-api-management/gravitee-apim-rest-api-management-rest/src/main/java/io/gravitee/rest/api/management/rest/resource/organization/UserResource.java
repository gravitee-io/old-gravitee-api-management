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

import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.UserService;
import io.swagger.annotations.*;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.*;

/**
 * Defines the REST resources to manage Users.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Users" })
public class UserResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private UserService userService;

    @Inject
    private GroupService groupService;

    @PathParam("userId")
    @ApiParam(name = "userId", required = true)
    private String userId;

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Retrieve a user", notes = "User must have the ORGANIZATION_USERS[READ] permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "A user", response = UserEntity.class),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.READ))
    public UserEntity getUser() {
        UserEntity user = userService.findByIdWithRoles(userId);

        // Delete password for security reason
        user.setPassword(null);
        user.setPicture(null);

        return user;
    }

    @DELETE
    @ApiOperation(value = "Delete a user", notes = "User must have the ORGANIZATION_USERS[DELETE] permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 204, message = "User successfully deleted"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.DELETE))
    public Response deleteUser() {
        userService.delete(userId);
        return Response.noContent().build();
    }

    @GET
    @Path("/groups")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
        value = "List of groups the user belongs to",
        notes = "User must have the ORGANIZATION_USERS[READ] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "List of user groups"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.READ))
    public List<UserGroupEntity> getUserGroups() {
        List<UserGroupEntity> groups = new ArrayList<>();
        groupService
            .findByUser(userId)
            .forEach(
                groupEntity -> {
                    UserGroupEntity userGroupEntity = new UserGroupEntity();
                    userGroupEntity.setId(groupEntity.getId());
                    userGroupEntity.setName(groupEntity.getName());
                    userGroupEntity.setRoles(new HashMap<>());
                    Set<RoleEntity> roles = membershipService.getRoles(
                        MembershipReferenceType.GROUP,
                        groupEntity.getId(),
                        MembershipMemberType.USER,
                        userId
                    );
                    if (!roles.isEmpty()) {
                        roles.forEach(role -> userGroupEntity.getRoles().put(role.getScope().name(), role.getName()));
                    }
                    groups.add(userGroupEntity);
                }
            );

        return groups;
    }

    @GET
    @Path("/memberships")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
        value = "List of memberships the user belongs to",
        notes = "User must have the ORGANIZATION_USERS[READ] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "List of user memberships"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.READ))
    public UserMembershipList getUserMemberships(@QueryParam("type") String sType) {
        MembershipReferenceType type = null;
        if (sType != null) {
            type = MembershipReferenceType.valueOf(sType.toUpperCase());
        }
        List<UserMembership> userMemberships = membershipService.findUserMembership(type, userId);
        Metadata metadata = membershipService.findUserMembershipMetadata(userMemberships, type);
        UserMembershipList userMembershipList = new UserMembershipList();
        userMembershipList.setMemberships(userMemberships);
        userMembershipList.setMetadata(metadata.getMetadata());
        return userMembershipList;
    }

    @POST
    @ApiOperation(
        value = "Reset the user's password",
        notes = "User must have the ORGANIZATION_USERS[UPDATE] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 204, message = "User's password reset"),
            @ApiResponse(code = 400, message = "reset page URL must not be null"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions(
        @Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.UPDATE)
        // if permission changes or a new one is added, please update io.gravitee.rest.api.service.impl.UserServiceImpl#canResetPassword
    )
    @Path("resetPassword")
    public Response resetUserPassword() {
        userService.resetPassword(userId);
        return Response.noContent().build();
    }

    @GET
    @Path("/avatar")
    @ApiOperation(value = "Get the user's avatar")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "User's avatar"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response getUserAvatar(@Context Request request) {
        PictureEntity picture = userService.getPicture(userId);

        if (picture instanceof UrlPictureEntity) {
            return Response.temporaryRedirect(URI.create(((UrlPictureEntity) picture).getUrl())).build();
        }

        InlinePictureEntity image = (InlinePictureEntity) picture;
        if (image == null || image.getContent() == null) {
            return Response.ok().build();
        }

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder.cacheControl(cc).build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return Response.ok().entity(baos).cacheControl(cc).tag(etag).type(image.getType()).build();
    }

    @PUT
    @Path("/roles")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.UPDATE))
    public Response updateUserRoles(@NotNull UserReferenceRoleEntity userReferenceRoles) {
        userService.updateUserRoles(
            userId,
            userReferenceRoles.getReferenceType(),
            userReferenceRoles.getReferenceId(),
            userReferenceRoles.getRoles()
        );
        return Response.ok().build();
    }

    @POST
    @Path("/changePassword")
    @ApiOperation(value = "Change user password after a reset", notes = "User registration must be enabled")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "User successfully updated", response = UserEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response finalizeResetPassword(@Valid ResetPasswordUserEntity resetPwdEntity) {
        UserEntity newUser = userService.finalizeResetPassword(resetPwdEntity);
        if (newUser != null) {
            return Response.ok().entity(newUser).build();
        }

        return Response.serverError().build();
    }

    @POST
    @Path("/_process")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.UPDATE))
    @ApiOperation(value = "Process a user registration by accepting or rejecting it")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Processed user"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response validateRegistration(boolean accepted) {
        return Response.ok(userService.processRegistration(userId, accepted)).build();
    }

    @Path("tokens")
    public UserTokensResource getUserTokensResource() {
        return resourceContext.getResource(UserTokensResource.class);
    }
}
