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

import static io.gravitee.rest.api.model.alert.AlertReferenceType.PLATFORM;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.param.AlertEventSearchParam;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.AlertEventQuery;
import io.gravitee.rest.api.model.alert.*;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.AlertService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Platform Alerts" })
public class PlatformAlertsResource extends AbstractResource {

    private static final String PLATFORM_REFERENCE_ID = "default";

    @Inject
    private AlertService alertService;

    @GET
    @ApiOperation(
        value = "List configured alerts of the platform",
        notes = "User must have the MANAGEMENT_ALERT[READ] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "List of alerts", response = AlertTriggerEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = READ) })
    public List<AlertTriggerEntity> getPlatformAlerts() {
        return alertService.findByReference(PLATFORM, PLATFORM_REFERENCE_ID);
    }

    @GET
    @Path("status")
    @ApiOperation(value = "Get alerting status", notes = "User must have the MANAGEMENT_ALERT[READ] permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Alerting status", response = AlertStatusEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = READ) })
    public AlertStatusEntity getPlatformAlertStatus() {
        return alertService.getStatus();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Create an alert for the platform",
        notes = "User must have the MANAGEMENT_ALERT[CREATE] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Alert successfully created", response = AlertTriggerEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = RolePermissionAction.CREATE) })
    public AlertTriggerEntity createPlatformAlert(@Valid @NotNull final NewAlertTriggerEntity alertEntity) {
        alertEntity.setReferenceType(PLATFORM);
        alertEntity.setReferenceId(PLATFORM_REFERENCE_ID);
        return alertService.create(alertEntity);
    }

    @Path("{alert}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Update an alert for the platform",
        notes = "User must have the MANAGEMENT_ALERT[UPDATE] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Alert successfully updated", response = AlertTriggerEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = RolePermissionAction.UPDATE) })
    public AlertTriggerEntity updatePlatformAlert(
        @PathParam("alert") String alert,
        @Valid @NotNull final UpdateAlertTriggerEntity alertEntity
    ) {
        alertEntity.setId(alert);
        alertEntity.setReferenceType(PLATFORM);
        alertEntity.setReferenceId(PLATFORM_REFERENCE_ID);
        return alertService.update(alertEntity);
    }

    @POST
    @Path("{alert}")
    @ApiOperation(
        value = "Associate the alert to multiple references (API, APPLICATION",
        notes = "User must have the MANAGEMENT_ALERT[UPDATE] permission to use this service"
    )
    @ApiResponses(
        { @ApiResponse(code = 200, message = "Alert successfully associated"), @ApiResponse(code = 500, message = "Internal server error") }
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = RolePermissionAction.UPDATE) })
    public Response associatePlatformAlert(@PathParam("alert") String alert, @QueryParam("type") String type) {
        alertService.applyDefaults(alert, AlertReferenceType.valueOf(type.toUpperCase()));
        return Response.ok().build();
    }

    @Path("{alert}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Delete an alert for the platform",
        notes = "User must have the MANAGEMENT_ALERT[DELETE] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 204, message = "Alert successfully deleted", response = AlertTriggerEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = RolePermissionAction.DELETE) })
    public void deletePlatformAlert(@PathParam("alert") String alert) {
        alertService.delete(alert, PLATFORM_REFERENCE_ID);
    }

    @GET
    @Path("{alert}/events")
    @ApiOperation(
        value = "Retrieve the list of events for an alert",
        notes = "User must have the MANAGEMENT_ALERT[READ] permission to use this service"
    )
    @ApiResponses({ @ApiResponse(code = 200, message = "List of events"), @ApiResponse(code = 500, message = "Internal server error") })
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = READ) })
    public Page<AlertEventEntity> getPlatformAlertEvents(@PathParam("alert") String alert, @BeanParam AlertEventSearchParam param) {
        return alertService.findEvents(
            alert,
            new AlertEventQuery.Builder()
                .from(param.getFrom())
                .to(param.getTo())
                .pageNumber(param.getPage())
                .pageSize(param.getSize())
                .build()
        );
    }
}
