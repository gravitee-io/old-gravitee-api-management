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
import io.gravitee.repository.management.model.DashboardReferenceType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.DashboardEntity;
import io.gravitee.rest.api.model.NewDashboardEntity;
import io.gravitee.rest.api.model.UpdateDashboardEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.DashboardService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Dashboards" })
public class DashboardsResource extends AbstractResource {

    @Inject
    private DashboardService dashboardService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve the list of platform dashboards")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "List of platform dashboards", response = DashboardEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public List<DashboardEntity> getDashboards(final @QueryParam("reference_type") DashboardReferenceType referenceType) {
        if (
            !hasPermission(RolePermission.ENVIRONMENT_DASHBOARD, RolePermissionAction.READ) &&
            !hasPermission(RolePermission.ENVIRONMENT_API, RolePermissionAction.READ) &&
            !canReadAPIConfiguration() &&
            !DashboardReferenceType.HOME.equals(referenceType)
        ) {
            throw new ForbiddenAccessException();
        }
        if (referenceType == null) {
            return dashboardService.findAll();
        } else {
            return dashboardService.findByReferenceType(referenceType);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Create a platform dashboard",
        notes = "User must have the MANAGEMENT_DASHBOARD[CREATE] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "Dashboard successfully created", response = DashboardEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = RolePermissionAction.CREATE) })
    public DashboardEntity createDashboard(@Valid @NotNull final NewDashboardEntity dashboard) {
        return dashboardService.create(dashboard);
    }

    @GET
    @Path("{dashboardId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Retrieve a platform dashboard",
        notes = "User must have the MANAGEMENT_DASHBOARD[READ] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Platform dashboard", response = DashboardEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = RolePermissionAction.READ) })
    public DashboardEntity getDashboard(final @PathParam("dashboardId") String dashboardId) {
        return dashboardService.findById(dashboardId);
    }

    @Path("{dashboardId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Update a platform dashboard",
        notes = "User must have the MANAGEMENT_DASHBOARD[UPDATE] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Updated dashboard", response = DashboardEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = RolePermissionAction.UPDATE) })
    public DashboardEntity updateDashboard(
        @PathParam("dashboardId") String dashboardId,
        @Valid @NotNull final UpdateDashboardEntity dashboard
    ) {
        dashboard.setId(dashboardId);
        return dashboardService.update(dashboard);
    }

    @Path("{dashboardId}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Delete a platform dashboard",
        notes = "User must have the MANAGEMENT_DASHBOARD[DELETE] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 204, message = "Dashboard successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = RolePermissionAction.DELETE) })
    public void deleteDashboard(@PathParam("dashboardId") String dashboardId) {
        dashboardService.delete(dashboardId);
    }
}
