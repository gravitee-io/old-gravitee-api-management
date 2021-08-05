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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.filtering.FilteredEntities;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.ApisResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
public class ApisResourceNotAuthenticatedTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "apis";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(AuthenticationFilter.class);
    }

    @Priority(50)
    public static class AuthenticationFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(
                new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return null;
                    }

                    @Override
                    public boolean isUserInRole(String string) {
                        return false;
                    }

                    @Override
                    public boolean isSecure() {
                        return false;
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return "BASIC";
                    }
                }
            );
        }
    }

    @Before
    public void init() {
        resetAllMocks();

        ApiEntity publishedApi = new ApiEntity();
        publishedApi.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi.setName("A");
        publishedApi.setId("A");

        ApiEntity unpublishedApi = new ApiEntity();
        unpublishedApi.setLifecycleState(ApiLifecycleState.UNPUBLISHED);
        unpublishedApi.setName("B");
        unpublishedApi.setId("B");

        ApiEntity anotherPublishedApi = new ApiEntity();
        anotherPublishedApi.setLifecycleState(ApiLifecycleState.PUBLISHED);
        anotherPublishedApi.setName("C");
        anotherPublishedApi.setId("C");

        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(publishedApi, anotherPublishedApi));
        doReturn(mockApis).when(apiService).findPublishedByUser(isNull(), any(ApiQuery.class));

        doReturn(new FilteredEntities<ApiEntity>(new ArrayList<>(mockApis), null)).when(filteringService).filterApis(any(), any(), any());

        doReturn(false).when(ratingService).isEnabled();

        doReturn(new Api().name("A").id("A")).when(apiMapper).convert(publishedApi);
        doReturn(new Api().name("B").id("B")).when(apiMapper).convert(unpublishedApi);
        doReturn(new Api().name("C").id("C")).when(apiMapper).convert(anotherPublishedApi);
    }

    @Test
    public void shouldGetpublishedApi() {
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(apiService).findPublishedByUser(isNull(), any(ApiQuery.class));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(2, apiResponse.getData().size());
        assertEquals("A", ((Api) apiResponse.getData().get(0)).getName());
        assertEquals("C", ((Api) apiResponse.getData().get(1)).getName());
    }
}
