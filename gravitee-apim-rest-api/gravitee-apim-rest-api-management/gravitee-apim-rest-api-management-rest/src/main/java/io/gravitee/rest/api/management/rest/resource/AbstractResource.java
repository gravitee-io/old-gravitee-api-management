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

import static io.gravitee.rest.api.model.MembershipMemberType.USER;
import static io.gravitee.rest.api.model.MembershipReferenceType.API;
import static io.gravitee.rest.api.model.MembershipReferenceType.GROUP;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractResource {

    public static final String ORGANIZATION_ADMIN = RoleScope.ORGANIZATION.name() + ':' + SystemRole.ADMIN.name();

    @Context
    protected SecurityContext securityContext;

    @Context
    protected UriInfo uriInfo;

    @Inject
    protected MembershipService membershipService;

    @Inject
    protected RoleService roleService;

    @Inject
    protected ApiService apiService;

    @Inject
    protected PermissionService permissionService;

    protected UserDetails getAuthenticatedUserDetails() {
        return (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    protected String getAuthenticatedUser() {
        return securityContext.getUserPrincipal().getName();
    }

    protected String getAuthenticatedUserOrNull() {
        return isAuthenticated() ? getAuthenticatedUser() : null;
    }

    protected boolean isAuthenticated() {
        return securityContext.getUserPrincipal() != null;
    }

    protected boolean isAdmin() {
        return isUserInRole(ORGANIZATION_ADMIN);
    }

    private boolean isUserInRole(String role) {
        return securityContext.isUserInRole(role);
    }

    protected boolean hasPermission(RolePermission permission, RolePermissionAction... acls) {
        return hasPermission(permission, null, acls);
    }

    protected boolean hasPermission(RolePermission permission, String referenceId, RolePermissionAction... acls) {
        return isAuthenticated() && (isAdmin() || permissionService.hasPermission(permission, referenceId, acls));
    }

    protected boolean canReadAPIConfiguration() {
        if (!isAdmin()) {
            return retrieveApiMembership().findFirst().isPresent();
        }
        return true;
    }

    /**
     * @return The list of API Membership for the authenticated user (direct membership or through groups)
     */
    private Stream<MembershipEntity> retrieveApiMembership() {
        Stream<MembershipEntity> streamUserMembership = membershipService
            .getMembershipsByMemberAndReference(USER, getAuthenticatedUser(), API)
            .stream();

        Stream<MembershipEntity> streamGroupMembership = membershipService
            .getMembershipsByMemberAndReference(USER, getAuthenticatedUser(), GROUP)
            .stream()
            .filter(m -> m.getRoleId() != null && roleService.findById(m.getRoleId()).getScope().equals(RoleScope.API));

        return Stream.concat(streamUserMembership, streamGroupMembership);
    }

    protected void canReadApi(final String api) {
        if (!isAdmin()) {
            // get memberships of the current user
            List<MembershipEntity> memberships = retrieveApiMembership().collect(Collectors.toList());
            Set<String> groups = memberships
                .stream()
                .filter(m -> GROUP.equals(m.getReferenceType()))
                .map(m -> m.getReferenceId())
                .collect(Collectors.toSet());
            Set<String> directMembers = memberships
                .stream()
                .filter(m -> API.equals(m.getReferenceType()))
                .map(m -> m.getReferenceId())
                .collect(Collectors.toSet());

            // if the current user is member of the API, continue
            if (directMembers.contains(api)) {
                return;
            }

            // fetch group memberships
            final ApiQuery apiQuery = new ApiQuery();
            apiQuery.setGroups(new ArrayList<>(groups));
            apiQuery.setIds(Collections.singletonList(api));
            final Collection<String> strings = apiService.searchIds(apiQuery);
            final boolean canReadAPI = strings.contains(api);
            if (!canReadAPI) {
                throw new ForbiddenAccessException();
            }
        }
    }

    protected UriBuilder getRequestUriBuilder() {
        return this.uriInfo.getRequestUriBuilder();
    }

    protected URI getLocationHeader(String... paths) {
        final UriBuilder requestUriBuilder = this.uriInfo.getRequestUriBuilder();
        for (String path : paths) {
            requestUriBuilder.path(path);
        }
        return requestUriBuilder.build();
    }
}
