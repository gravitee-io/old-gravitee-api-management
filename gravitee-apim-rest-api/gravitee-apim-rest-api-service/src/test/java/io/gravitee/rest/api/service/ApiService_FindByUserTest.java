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
package io.gravitee.rest.api.service;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.impl.ApiServiceImpl;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_FindByUserTest {

    private static final String USER_NAME = "myUser";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private GroupService groupService;

    @Mock
    private RoleService roleService;

    @Mock
    private SubscriptionService subscriptionService;

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private Api api;

    @Mock
    private Api privateApi;

    @Mock
    private SubscriptionEntity subscription;

    @Mock
    private UserService userService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private CategoryService categoryService;

    @Spy
    private ApiConverter apiConverter;

    @Before
    public void setUp() {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(
            new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter))
        );
    }

    @Test
    public void shouldFindByUser() throws TechnicalException {
        when(apiRepository.search(new ApiCriteria.Builder().environmentId("DEFAULT").ids(api.getId()).build()))
            .thenReturn(singletonList(api));

        MembershipEntity membership = new MembershipEntity();
        membership.setId("id");
        membership.setMemberId(USER_NAME);
        membership.setMemberType(MembershipMemberType.USER);
        membership.setReferenceId(api.getId());
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoleId("API_USER");

        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.API))
            .thenReturn(Collections.singleton(membership));

        RoleEntity poRole = new RoleEntity();
        poRole.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(poRole);

        MemberEntity poMember = new MemberEntity();
        poMember.setId("admin");
        poMember.setRoles(Collections.singletonList(poRole));
        when(
            membershipService.getMembersByReferencesAndRole(
                MembershipReferenceType.API,
                Collections.singletonList(api.getId()),
                "API_PRIMARY_OWNER"
            )
        )
            .thenReturn(new HashSet(Arrays.asList(poMember)));

        final ApplicationListItem application = new ApplicationListItem();
        application.setId("appId");

        final Set<ApiEntity> apiEntities = apiService.findByUser(USER_NAME, null, false);

        assertNotNull(apiEntities);
        assertEquals(1, apiEntities.size());
    }

    @Test
    public void shouldFindByUserPaginated() throws TechnicalException {
        final Api api1 = new Api();
        api1.setId("api1");
        api1.setName("api1");
        final Api api2 = new Api();
        api2.setId("api2");
        api2.setName("api2");

        MembershipEntity membership1 = new MembershipEntity();
        membership1.setId("id1");
        membership1.setMemberId(USER_NAME);
        membership1.setMemberType(MembershipMemberType.USER);
        membership1.setReferenceId(api1.getId());
        membership1.setReferenceType(MembershipReferenceType.API);
        membership1.setRoleId("API_USER");

        MembershipEntity membership2 = new MembershipEntity();
        membership2.setId(api2.getId());
        membership2.setMemberId(USER_NAME);
        membership2.setMemberType(MembershipMemberType.USER);
        membership2.setReferenceId("api2");
        membership2.setReferenceType(MembershipReferenceType.API);
        membership2.setRoleId("API_USER");

        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.API))
            .thenReturn(new HashSet<>(Arrays.asList(membership1, membership2)));
        when(apiRepository.search(new ApiCriteria.Builder().environmentId("DEFAULT").ids(api1.getId(), api2.getId()).build()))
            .thenReturn(Arrays.asList(api1, api2));

        RoleEntity poRole = new RoleEntity();
        poRole.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(poRole);

        MemberEntity poMember = new MemberEntity();
        poMember.setId("admin");
        poMember.setRoles(Collections.singletonList(poRole));
        when(
            membershipService.getMembersByReferencesAndRole(
                MembershipReferenceType.API,
                Collections.singletonList(api1.getId()),
                "API_PRIMARY_OWNER"
            )
        )
            .thenReturn(new HashSet<>(singletonList(poMember)));

        final Page<ApiEntity> apiPage = apiService.findByUser(
            USER_NAME,
            null,
            new SortableImpl("name", false),
            new PageableImpl(2, 1),
            false
        );

        assertNotNull(apiPage);
        assertEquals(1, apiPage.getContent().size());
        assertEquals(api1.getId(), apiPage.getContent().get(0).getId());
        assertEquals(2, apiPage.getPageNumber());
        assertEquals(1, apiPage.getPageElements());
        assertEquals(2, apiPage.getTotalElements());
    }

    @Test
    public void shouldNotFindByUserBecauseNotExists() throws TechnicalException {
        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.API))
            .thenReturn(Collections.emptySet());

        final Set<ApiEntity> apiEntities = apiService.findByUser(USER_NAME, null, false);

        assertNotNull(apiEntities);
        assertTrue(apiEntities.isEmpty());
    }

    @Test
    public void shouldFindPublicApisOnlyWithAnonymousUser() throws TechnicalException {
        final Set<ApiEntity> apiEntities = apiService.findByUser(null, null, false);

        assertNotNull(apiEntities);
        assertEquals(0, apiEntities.size());

        verify(membershipService, times(0))
            .getMembershipsByMemberAndReference(MembershipMemberType.USER, null, MembershipReferenceType.API);
        verify(membershipService, times(0))
            .getMembershipsByMemberAndReference(MembershipMemberType.USER, null, MembershipReferenceType.GROUP);
        verify(applicationService, times(0)).findByUser(null);
    }
}
