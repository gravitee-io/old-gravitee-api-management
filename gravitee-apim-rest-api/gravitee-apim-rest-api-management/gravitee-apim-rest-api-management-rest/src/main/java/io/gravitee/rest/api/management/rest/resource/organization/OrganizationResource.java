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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.resource.EnvironmentsResource;
import io.gravitee.rest.api.management.rest.resource.PromotionsResource;
import io.gravitee.rest.api.management.rest.resource.auth.OAuth2AuthenticationResource;
import io.gravitee.rest.api.management.rest.resource.installation.InstallationResource;
import io.gravitee.rest.api.management.rest.resource.portal.SocialIdentityProvidersResource;
import io.gravitee.rest.api.management.rest.resource.search.SearchResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService.ActivationTarget;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api
public class OrganizationResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private IdentityProviderService identityProviderService;

    @Inject
    private IdentityProviderActivationService identityProviderActivationService;

    @Inject
    private OrganizationService organizationService;

    @GET
    @Path("/identities")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_IDENTITY_PROVIDER_ACTIVATION, acls = RolePermissionAction.READ))
    @ApiOperation(
        value = "Get the list of identity provider activations for current organization",
        notes = "User must have the ORGANIZATION_IDENTITY_PROVIDER_ACTIVATION[READ] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(
                code = 200,
                message = "List identity provider activations for current organization",
                response = IdentityProviderActivationEntity.class,
                responseContainer = "List"
            ),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Set<IdentityProviderActivationEntity> listIdentityProviderActivations() {
        return identityProviderActivationService.findAllByTarget(
            new ActivationTarget(GraviteeContext.getCurrentOrganization(), IdentityProviderActivationReferenceType.ORGANIZATION)
        );
    }

    @PUT
    @Path("/identities")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions(
        @Permission(
            value = RolePermission.ORGANIZATION_IDENTITY_PROVIDER_ACTIVATION,
            acls = { RolePermissionAction.CREATE, RolePermissionAction.DELETE, RolePermissionAction.UPDATE }
        )
    )
    @ApiOperation(value = "Update available organization identities", tags = { "Organization" })
    @ApiResponses(
        {
            @ApiResponse(code = 204, message = "Organization successfully updated"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response updateOrganizationIdentities(Set<IdentityProviderActivationEntity> identityProviderActivations) {
        this.identityProviderActivationService.updateTargetIdp(
                new ActivationTarget(GraviteeContext.getCurrentOrganization(), IdentityProviderActivationReferenceType.ORGANIZATION),
                identityProviderActivations
                    .stream()
                    .filter(
                        ipa -> {
                            final IdentityProviderEntity idp = this.identityProviderService.findById(ipa.getIdentityProvider());
                            return GraviteeContext.getCurrentOrganization().equals(idp.getOrganization());
                        }
                    )
                    .map(IdentityProviderActivationEntity::getIdentityProvider)
                    .collect(Collectors.toList())
            );
        return Response.noContent().build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions(
        @Permission(
            value = RolePermission.ORGANIZATION_POLICIES,
            acls = { RolePermissionAction.CREATE, RolePermissionAction.DELETE, RolePermissionAction.UPDATE }
        )
    )
    @ApiOperation(value = "Update organization", tags = { "Organization" })
    @ApiResponses(
        {
            @ApiResponse(code = 204, message = "Organization successfully updated"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response update(UpdateOrganizationEntity organizationEntity) {
        OrganizationEntity updatedOrganization = organizationService.update(GraviteeContext.getCurrentOrganization(), organizationEntity);
        return Response.ok(updatedOrganization).status(204).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions(
        @Permission(
            value = RolePermission.ORGANIZATION_POLICIES,
            acls = { RolePermissionAction.CREATE, RolePermissionAction.DELETE, RolePermissionAction.UPDATE }
        )
    )
    @ApiOperation(value = "GET organization", tags = { "Organization" })
    @ApiResponses({ @ApiResponse(code = 200, message = "Organization"), @ApiResponse(code = 500, message = "Internal server error") })
    public Response get() {
        return Response.ok(organizationService.findById(GraviteeContext.getCurrentOrganization())).build();
    }

    // Dynamic authentication provider endpoints
    @Path("auth/oauth2/{identity}")
    public OAuth2AuthenticationResource getOAuth2AuthenticationResource() {
        return resourceContext.getResource(OAuth2AuthenticationResource.class);
    }

    @Path("configuration")
    public OrganizationConfigurationResource getConfigurationResource() {
        return resourceContext.getResource(OrganizationConfigurationResource.class);
    }

    @Path("console")
    public ConsoleResource getConsoleResource() {
        return resourceContext.getResource(ConsoleResource.class);
    }

    @Path("environments")
    public EnvironmentsResource getEnvironmentsResource() {
        return resourceContext.getResource(EnvironmentsResource.class);
    }

    @Path("social-identities")
    public SocialIdentityProvidersResource getSocialIdentityProvidersResource() {
        return resourceContext.getResource(SocialIdentityProvidersResource.class);
    }

    @Path("search")
    public SearchResource getSearchResource() {
        return resourceContext.getResource(SearchResource.class);
    }

    @Path("settings")
    public ConsoleSettingsResource getConsoleSettingsResource() {
        return resourceContext.getResource(ConsoleSettingsResource.class);
    }

    @Path("users")
    public UsersResource getUsersResource() {
        return resourceContext.getResource(UsersResource.class);
    }

    @Path("user")
    public CurrentUserResource getCurrentUserResource() {
        return resourceContext.getResource(CurrentUserResource.class);
    }

    @Path("installation")
    public InstallationResource getInstallationResource() {
        return resourceContext.getResource(InstallationResource.class);
    }

    @Path("groups")
    public GroupsResource getGroupsResource() {
        return resourceContext.getResource(GroupsResource.class);
    }

    @Path("promotions")
    public PromotionsResource getPromotionsResource() {
        return resourceContext.getResource(PromotionsResource.class);
    }
}
