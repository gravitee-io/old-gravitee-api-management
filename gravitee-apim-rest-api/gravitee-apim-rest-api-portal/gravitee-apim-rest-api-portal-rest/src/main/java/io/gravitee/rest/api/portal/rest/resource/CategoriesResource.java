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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.mapper.CategoryMapper;
import io.gravitee.rest.api.portal.rest.model.Category;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.service.CategoryService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.checkerframework.checker.units.qual.Time;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CategoriesResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryMapper categoryMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    /*
     * Base IMPL
     * Retrieving API list took 717229458 nanoseconds (0.717229458 s)
     * Aggregating results took 31218084 nanoseconds (0.031218084 s)
     */
    public Response getCategories(@BeanParam PaginationParam paginationParam) {
        Map<String, Long> countByCategory = apiService.countPublishedByUserGroupedByCategories(getAuthenticatedUserOrNull());

        List<Category> categoriesList = categoryService
            .findAll()
            .stream()
            .filter(c -> !c.isHidden())
            .sorted(Comparator.comparingInt(CategoryEntity::getOrder))
            .peek(c -> c.setTotalApis(countByCategory.getOrDefault(c.getId(), 0L)))
            .filter(c -> c.getTotalApis() > 0)
            .map(c -> categoryMapper.convert(c, uriInfo.getBaseUriBuilder()))
            .collect(Collectors.toList());

        return createListResponse(categoriesList, paginationParam);
    }

    @Path("{categoryId}")
    public CategoryResource getCategoryResource() {
        return resourceContext.getResource(CategoryResource.class);
    }
}
