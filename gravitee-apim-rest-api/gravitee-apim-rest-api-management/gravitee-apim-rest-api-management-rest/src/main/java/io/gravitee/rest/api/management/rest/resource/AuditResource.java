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

import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.management.rest.resource.param.AuditParam;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.audit.AuditEntity;
import io.gravitee.rest.api.model.audit.AuditQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.AuditService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.*;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.reflections.Reflections;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/audit")
public class AuditResource extends AbstractResource {

    private static final List<Audit.AuditEvent> events = new ArrayList<>();

    @Context
    private ResourceContext resourceContext;

    @Inject
    private AuditService auditService;

    @GET
    @ApiOperation(
        value = "Retrieve audit logs for the platform",
        notes = "User must have the MANAGEMENT_AUDIT[READ] permission to use this service"
    )
    @ApiResponses({ @ApiResponse(code = 200, message = "List of audits"), @ApiResponse(code = 500, message = "Internal server error") })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUDIT, acls = RolePermissionAction.READ) })
    public MetadataPage<AuditEntity> getAudits(@BeanParam AuditParam param) {
        AuditQuery query = new AuditQuery();
        query.setFrom(param.getFrom());
        query.setTo(param.getTo());
        query.setPage(param.getPage());
        query.setSize(param.getSize());
        if (param.isManagementLogsOnly()) {
            query.setManagementLogsOnly(true);
        } else {
            if (param.getApiId() != null) {
                query.setApiIds(Collections.singletonList(param.getApiId()));
            }
            if (param.getApplicationId() != null) {
                query.setApplicationIds(Collections.singletonList(param.getApplicationId()));
            }
        }

        if (param.getEvent() != null) {
            query.setEvents(Collections.singletonList(param.getEvent()));
        }

        return auditService.search(query);
    }

    @Path("/events")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "List available audit event type for platform",
        notes = "User must have the MANAGEMENT_AUDIT[READ] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "List of audits", response = Audit.AuditEvent.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUDIT, acls = RolePermissionAction.READ) })
    public Response getAuditEvents() {
        if (events.isEmpty()) {
            Set<Class<? extends Audit.AuditEvent>> subTypesOf = new Reflections("io.gravitee.repository.management.model")
            .getSubTypesOf(Audit.AuditEvent.class);
            for (Class<? extends Audit.AuditEvent> clazz : subTypesOf) {
                if (clazz.isEnum()) {
                    events.addAll(Arrays.asList(clazz.getEnumConstants()));
                }
            }

            events.sort(Comparator.comparing(Audit.AuditEvent::name));
        }
        return Response.ok(events).build();
    }
}
