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
import io.gravitee.rest.api.model.ResourceListItem;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.platform.plugin.PluginEntity;
import io.gravitee.rest.api.service.ServiceDiscoveryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * Defines the REST resources to manage service discovery.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Plugins" })
public class ServicesDiscoveryResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ServiceDiscoveryService serviceDiscoveryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "List service discovery plugins",
        notes = "User must have the MANAGEMENT_API[READ] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(
                code = 200,
                message = "List of service discovery plugins",
                response = ResourceListItem.class,
                responseContainer = "List"
            ),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public Collection<ResourceListItem> getServicesDiscoverResources(@QueryParam("expand") List<String> expand) {
        Stream<ResourceListItem> stream = serviceDiscoveryService.findAll().stream().map(this::convert);

        if (expand != null && !expand.isEmpty()) {
            for (String s : expand) {
                switch (s) {
                    case "schema":
                        stream =
                            stream.map(
                                resourceListItem -> {
                                    resourceListItem.setSchema(serviceDiscoveryService.getSchema(resourceListItem.getId()));
                                    return resourceListItem;
                                }
                            );
                        break;
                    default:
                        break;
                }
            }
        }

        return stream.sorted(Comparator.comparing(ResourceListItem::getName)).collect(Collectors.toList());
    }

    @Path("{plugin}")
    public ServiceDiscoveryResource getServiceDiscoveryResource() {
        return resourceContext.getResource(ServiceDiscoveryResource.class);
    }

    private ResourceListItem convert(PluginEntity pluginEntity) {
        ResourceListItem item = new ResourceListItem();

        item.setId(pluginEntity.getId());
        item.setName(pluginEntity.getName());
        item.setDescription(pluginEntity.getDescription());
        item.setVersion(pluginEntity.getVersion());

        return item;
    }
}
