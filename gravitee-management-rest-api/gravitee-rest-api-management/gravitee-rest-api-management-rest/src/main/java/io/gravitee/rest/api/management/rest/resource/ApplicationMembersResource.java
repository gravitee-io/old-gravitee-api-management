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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.ApplicationMembership;
import io.gravitee.rest.api.management.rest.model.TransferOwnership;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.ApplicationPermission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.swagger.annotations.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.DELETE;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Application Memberships" })
public class ApplicationMembersResource extends AbstractResource {

    @Inject
    private MembershipService membershipService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private UserService userService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @ApiParam(name = "application", hidden = true)
    private String application;

    @GET
    @Path("/permissions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get application members", notes = "User must have the APPLICATION_MEMBER permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(
                code = 200,
                message = "Application member's permissions",
                response = MemberEntity.class,
                responseContainer = "List"
            ),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response getApplicationMemberPermissions() {
        Map<String, char[]> permissions = new HashMap<>();
        if (isAuthenticated()) {
            final String username = getAuthenticatedUser();
            final ApplicationEntity applicationEntity = applicationService.findById(application);
            if (isAdmin()) {
                final char[] rights = new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() };
                for (ApplicationPermission perm : ApplicationPermission.values()) {
                    permissions.put(perm.getName(), rights);
                }
            } else {
                permissions = membershipService.getUserMemberPermissions(applicationEntity, username);
            }
        }
        return Response.ok(permissions).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List application members", notes = "User must have the READ permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(
                code = 200,
                message = "Application successfully deleted",
                response = MembershipListItem.class,
                responseContainer = "List"
            ),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.READ) })
    public List<MembershipListItem> getApplicationMembers() {
        applicationService.findById(application);
        return membershipService
            .getMembersByReference(MembershipReferenceType.APPLICATION, application)
            .stream()
            .map(MembershipListItem::new)
            .sorted(Comparator.comparing(MembershipListItem::getId))
            .collect(Collectors.toList());
    }

    @POST
    @ApiOperation(value = "Add or update an application member", notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "Member has been added or updated successfully"),
            @ApiResponse(code = 400, message = "Membership parameter is not valid"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions(
        {
            @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.UPDATE),
        }
    )
    public Response addOrUpdateApplicationMember(@Valid @NotNull ApplicationMembership applicationMembership) {
        if (PRIMARY_OWNER.name().equals(applicationMembership.getRole())) {
            throw new SinglePrimaryOwnerException(RoleScope.APPLICATION);
        }

        applicationService.findById(application);

        MembershipService.MembershipReference reference = new MembershipService.MembershipReference(
            MembershipReferenceType.APPLICATION,
            application
        );
        MembershipService.MembershipMember member = new MembershipService.MembershipMember(
            applicationMembership.getId(),
            applicationMembership.getReference(),
            MembershipMemberType.USER
        );
        MembershipService.MembershipRole role = new MembershipService.MembershipRole(
            RoleScope.APPLICATION,
            applicationMembership.getRole()
        );

        MemberEntity membership = null;
        if (applicationMembership.getId() != null) {
            MemberEntity userMember = membershipService.getUserMember(
                MembershipReferenceType.APPLICATION,
                application,
                applicationMembership.getId()
            );
            if (userMember != null && userMember.getRoles() != null && !userMember.getRoles().isEmpty()) {
                membership = membershipService.updateRoleToMemberOnReference(reference, member, role);
            }
        }
        if (membership == null) {
            membershipService.addRoleToMemberOnReference(reference, member, role);
        }

        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @ApiOperation(value = "Remove an application member", notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Member has been removed successfully"),
            @ApiResponse(code = 400, message = "User does not exist"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.DELETE) })
    public Response deleteApplicationMember(@ApiParam(name = "user", required = true) @NotNull @QueryParam("user") String userId) {
        applicationService.findById(application);
        try {
            userService.findById(userId);
        } catch (UserNotFoundException unfe) {
            return Response.status(Response.Status.BAD_REQUEST).entity(unfe.getMessage()).build();
        }

        membershipService.deleteReferenceMember(MembershipReferenceType.APPLICATION, application, MembershipMemberType.USER, userId);
        return Response.ok().build();
    }

    @POST
    @Path("transfer_ownership")
    @ApiOperation(
        value = "Transfer the ownership of the APPLICATION",
        notes = "User must have the TRANSFER_OWNERSHIP permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Ownership has been transferred successfully"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.UPDATE) })
    public Response transferApplicationOwnership(@Valid @NotNull TransferOwnership transferOwnership) {
        List<RoleEntity> newRoles = new ArrayList<>();
        Optional<RoleEntity> optNewPORole = roleService.findByScopeAndName(RoleScope.APPLICATION, transferOwnership.getPoRole());
        if (optNewPORole.isPresent()) {
            newRoles.add(optNewPORole.get());
        } else {
            //it doesn't matter
        }

        applicationService.findById(application);
        membershipService.transferApplicationOwnership(
            application,
            new MembershipService.MembershipMember(transferOwnership.getId(), transferOwnership.getReference(), MembershipMemberType.USER),
            newRoles
        );
        return Response.ok().build();
    }
}
