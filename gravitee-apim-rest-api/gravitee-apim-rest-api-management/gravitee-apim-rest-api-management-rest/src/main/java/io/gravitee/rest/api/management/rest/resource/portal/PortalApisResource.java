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
package io.gravitee.rest.api.management.rest.resource.portal;

import static io.gravitee.rest.api.model.Visibility.PUBLIC;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.RatingSummaryEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.api.ApiListItem;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.RatingService;
import io.swagger.annotations.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Defines the API to retrieve APIS from the portal.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Portal APIs" })
public class PortalApisResource extends AbstractResource {

    @Inject
    private ApiService apiService;

    @Inject
    private RatingService ratingService;

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ConfigService configService;

    @POST
    @Path("_search")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Search for API using the search engine")
    @ApiResponses(
        {
            @ApiResponse(
                code = 200,
                message = "List accessible APIs for current user",
                response = ApiListItem.class,
                responseContainer = "List"
            ),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response searchPortalApis(@ApiParam(name = "q", required = true) @NotNull @QueryParam("q") String query) {
        try {
            final Collection<ApiEntity> apis;
            final ApiQuery apiQuery = new ApiQuery();
            if (isAdmin()) {
                apis = apiService.search(apiQuery);
            } else {
                apiQuery.setLifecycleStates(singletonList(PUBLISHED));
                if (isAuthenticated()) {
                    apis = apiService.findByUser(getAuthenticatedUser(), apiQuery, true);
                } else if (configService.portalLoginForced()) {
                    // if portal requires login, this endpoint should hide the APIS even PUBLIC ones
                    return Response.ok().entity(emptyList()).build();
                } else {
                    apiQuery.setVisibility(PUBLIC);
                    apis = apiService.search(apiQuery);
                }
            }

            Map<String, Object> filters = new HashMap<>();
            filters.put("api", apis.stream().map(ApiEntity::getId).collect(Collectors.toSet()));

            return Response.ok().entity(apiService.search(query, filters).stream().map(this::convert).collect(toList())).build();
        } catch (TechnicalException te) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(te).build();
        }
    }

    private ApiListItem convert(ApiEntity api) {
        final ApiListItem apiItem = new ApiListItem();

        apiItem.setId(api.getId());
        apiItem.setName(api.getName());
        apiItem.setVersion(api.getVersion());
        apiItem.setDescription(api.getDescription());

        final UriBuilder ub = uriInfo.getBaseUriBuilder();
        final UriBuilder uriBuilder = ub.path("apis").path(api.getId()).path("picture");
        if (api.getPicture() != null) {
            // force browser to get if updated
            uriBuilder.queryParam("hash", api.getPicture().hashCode());
        }
        apiItem.setPictureUrl(uriBuilder.build().toString());
        apiItem.setCategories(api.getCategories());
        apiItem.setCreatedAt(api.getCreatedAt());
        apiItem.setUpdatedAt(api.getUpdatedAt());
        apiItem.setLabels(api.getLabels());
        apiItem.setCategories(api.getCategories());
        apiItem.setPrimaryOwner(api.getPrimaryOwner());

        if (api.getVisibility() != null) {
            apiItem.setVisibility(io.gravitee.rest.api.model.Visibility.valueOf(api.getVisibility().toString()));
        }

        if (api.getState() != null) {
            apiItem.setState(Lifecycle.State.valueOf(api.getState().toString()));
        }

        if (api.getProxy() != null) {
            apiItem.setVirtualHosts(api.getProxy().getVirtualHosts());
        }

        if (ratingService.isEnabled()) {
            final RatingSummaryEntity ratingSummary = ratingService.findSummaryByApi(api.getId());
            apiItem.setRate(ratingSummary.getAverageRate());
            apiItem.setNumberOfRatings(ratingSummary.getNumberOfRatings());
        }
        apiItem.setTags(api.getTags());

        if (api.getLifecycleState() != null) {
            apiItem.setLifecycleState(ApiLifecycleState.valueOf(api.getLifecycleState().toString()));
        }

        return apiItem;
    }
}
