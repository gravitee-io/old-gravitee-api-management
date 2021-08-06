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

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.common.http.HttpStatusCode.UNAUTHORIZED_401;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPagesResourceNotAdminTest extends AbstractResourceTest {

    private static final String API_NAME = "my-api";
    private static final String PAGE_NAME = "p";

    @Override
    protected String contextPath() {
        return "apis/" + API_NAME + "/pages/" + PAGE_NAME;
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
                        return () -> USER_NAME;
                    }

                    @Override
                    public boolean isUserInRole(String string) {
                        return false;
                    }

                    @Override
                    public boolean isSecure() {
                        return true;
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return "BASIC";
                    }
                }
            );
        }
    }

    @Test
    public void shouldGetPublicApiPublishedPage() {
        reset(apiService, pageService, membershipService);
        final ApiEntity apiMock = mock(ApiEntity.class);
        when(apiMock.getVisibility()).thenReturn(Visibility.PUBLIC);
        when(apiMock.getName()).thenReturn(API_NAME);
        doReturn(apiMock).when(apiService).findById(API_NAME);
        final PageEntity pageMock = new PageEntity();
        pageMock.setPublished(true);
        pageMock.setName(PAGE_NAME);
        when(groupService.isUserAuthorizedToAccessApiData(any(), any(), any())).thenReturn(Boolean.TRUE);
        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
        doReturn(pageMock).when(pageService).findById(PAGE_NAME, null);
        doReturn(true).when(pageService).isDisplayable(apiMock, pageMock.isPublished(), USER_NAME);

        final Response response = target().request().get();

        assertEquals(OK_200, response.getStatus());
        final PageEntity responsePage = response.readEntity(PageEntity.class);
        assertNotNull(responsePage);
        assertEquals(PAGE_NAME, responsePage.getName());
        verify(membershipService, never()).getRoles(any(), any(), any(), any());
        verify(apiService, times(1)).findById(API_NAME);
        verify(pageService, times(1)).findById(PAGE_NAME, null);
        verify(pageService, times(1)).isDisplayable(apiMock, pageMock.isPublished(), USER_NAME);
    }

    @Test
    public void shouldNotGetPrivateApiPublishedPage() {
        reset(apiService, pageService, membershipService);

        final ApiEntity apiMock = mock(ApiEntity.class);
        when(apiMock.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(apiMock.getName()).thenReturn(API_NAME);
        doReturn(apiMock).when(apiService).findById(API_NAME);
        final PageEntity pageMock = new PageEntity();
        pageMock.setPublished(true);
        pageMock.setName(PAGE_NAME);
        doReturn(pageMock).when(pageService).findById(PAGE_NAME, null);
        doReturn(false).when(pageService).isDisplayable(apiMock, pageMock.isPublished(), USER_NAME);
        doReturn(true)
            .when(roleService)
            .hasPermission(any(), eq(ApiPermission.DOCUMENTATION), eq(new RolePermissionAction[] { RolePermissionAction.READ }));
        when(groupService.isUserAuthorizedToAccessApiData(any(), any(), any())).thenReturn(Boolean.FALSE);
        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);

        final Response response = target().request().get();

        assertEquals(UNAUTHORIZED_401, response.getStatus());
        verify(apiService, atLeastOnce()).findById(API_NAME);
        verify(pageService, times(1)).findById(PAGE_NAME, null);
        verify(pageService, times(1)).isDisplayable(apiMock, pageMock.isPublished(), USER_NAME);
    }
}
