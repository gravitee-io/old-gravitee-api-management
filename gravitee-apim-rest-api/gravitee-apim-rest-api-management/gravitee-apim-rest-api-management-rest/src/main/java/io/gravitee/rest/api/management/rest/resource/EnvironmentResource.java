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
import io.gravitee.rest.api.management.rest.resource.auth.OAuth2AuthenticationResource;
import io.gravitee.rest.api.management.rest.resource.search.SearchResource;
import io.gravitee.rest.api.model.UpdateEnvironmentEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.annotations.*;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api
public class EnvironmentResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EnvironmentService environmentService;

    /**
     * Create a new Environment.
     * @param environmentEntity
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create an Environment", tags = { "Environment" })
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "Environment successfully created"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response createEnvironment(
        @ApiParam(name = "environmentEntity", required = true) @Valid @NotNull final UpdateEnvironmentEntity environmentEntity
    ) {
        environmentEntity.setId(GraviteeContext.getCurrentEnvironment());
        return Response.status(Status.CREATED).entity(environmentService.createOrUpdate(environmentEntity)).build();
    }

    /**
     * Delete an existing Environment.
     *
     * @return
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete an Environment", tags = { "Environment" })
    @ApiResponses(
        {
            @ApiResponse(code = 204, message = "Environment successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response deleteEnvironment() {
        environmentService.delete(GraviteeContext.getCurrentEnvironment());
        //TODO: should delete all items that refers to this environment
        return Response.status(Status.NO_CONTENT).build();
    }

    @Path("alerts")
    public AlertsResource getAlertsResource() {
        return resourceContext.getResource(AlertsResource.class);
    }

    @Path("apis")
    public ApisResource getApisResource() {
        return resourceContext.getResource(ApisResource.class);
    }

    @Path("applications")
    public ApplicationsResource getApplicationsResource() {
        return resourceContext.getResource(ApplicationsResource.class);
    }

    @Path("configuration")
    public ConfigurationResource getConfigurationResource() {
        return resourceContext.getResource(ConfigurationResource.class);
    }

    @Path("user")
    public CurrentUserResource getCurrentUserResource() {
        return resourceContext.getResource(CurrentUserResource.class);
    }

    @Path("subscriptions")
    public SubscriptionsResource getSubscriptionsResource() {
        return resourceContext.getResource(SubscriptionsResource.class);
    }

    @Path("audit")
    public AuditResource getAuditResource() {
        return resourceContext.getResource(AuditResource.class);
    }

    @Path("portal")
    public PortalResource getPortalResource() {
        return resourceContext.getResource(PortalResource.class);
    }

    // Dynamic authentication provider endpoints
    @Path("auth/oauth2/{identity}")
    public OAuth2AuthenticationResource getOAuth2AuthenticationResource() {
        return resourceContext.getResource(OAuth2AuthenticationResource.class);
    }

    @Path("users")
    public UsersResource getUsersResource() {
        return resourceContext.getResource(UsersResource.class);
    }

    @Path("search")
    public SearchResource getSearchResource() {
        return resourceContext.getResource(SearchResource.class);
    }

    @Path("fetchers")
    public FetchersResource getFetchersResource() {
        return resourceContext.getResource(FetchersResource.class);
    }

    @Path("policies")
    public PoliciesResource getPoliciesResource() {
        return resourceContext.getResource(PoliciesResource.class);
    }

    @Path("resources")
    public ResourcesResource getResourcesResource() {
        return resourceContext.getResource(ResourcesResource.class);
    }

    @Path("services-discovery")
    public ServicesDiscoveryResource getServicesDiscoveryResource() {
        return resourceContext.getResource(ServicesDiscoveryResource.class);
    }

    @Path("instances")
    public InstancesResource getInstancesResource() {
        return resourceContext.getResource(InstancesResource.class);
    }

    @Path("platform")
    public PlatformResource getPlatformResource() {
        return resourceContext.getResource(PlatformResource.class);
    }

    @Path("messages")
    public MessagesResource getMessagesResource() {
        return resourceContext.getResource(MessagesResource.class);
    }

    @Path("tickets")
    public PlatformTicketsResource getPlatformTicketsResource() {
        return resourceContext.getResource(PlatformTicketsResource.class);
    }

    @Path("entrypoints")
    public PortalEntrypointsResource getPortalEntryPointsResource() {
        return resourceContext.getResource(PortalEntrypointsResource.class);
    }

    @Path("notifiers")
    public NotifiersResource getNotifiersResource() {
        return resourceContext.getResource(NotifiersResource.class);
    }
}
