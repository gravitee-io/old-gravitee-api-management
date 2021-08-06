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

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.google.common.collect.Sets;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserMembership;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.impl.MembershipServiceImpl;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MembershipService_FindUserMembershipTest {

    private static final String USER_ID = "john-doe";

    @InjectMocks
    private MembershipService membershipService = new MembershipServiceImpl();

    @Mock
    private MembershipRepository mockMembershipRepository;

    @Mock
    private GroupService mockGroupService;

    @Mock
    private RoleService mockRoleService;

    @Test
    public void shouldGetEmptyResultForEnvironmentType() {
        List<UserMembership> references = membershipService.findUserMembership(
            io.gravitee.rest.api.model.MembershipReferenceType.ENVIRONMENT,
            USER_ID
        );

        assertTrue(references.isEmpty());
    }

    @Test
    public void shouldGetEmptyResultIfNoApiNorGroups() throws Exception {
        when(mockRoleService.findByScope(any())).thenReturn(Collections.emptyList());
        when(
            mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                eq(USER_ID),
                eq(MembershipMemberType.USER),
                eq(MembershipReferenceType.API)
            )
        )
            .thenReturn(Collections.emptySet());

        doReturn(Collections.emptySet()).when(mockGroupService).findByUser(USER_ID);

        List<UserMembership> references = membershipService.findUserMembership(
            io.gravitee.rest.api.model.MembershipReferenceType.API,
            USER_ID
        );

        assertTrue(references.isEmpty());
    }

    @Test
    public void shouldGetApiWithoutGroups() throws Exception {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId("role");
        roleEntity.setName("PO");
        roleEntity.setScope(RoleScope.API);
        when(mockRoleService.findByScope(any())).thenReturn(Collections.singletonList(roleEntity));
        Membership mApi = mock(Membership.class);
        when(mApi.getReferenceId()).thenReturn("api-id1");
        when(mApi.getRoleId()).thenReturn("role");

        when(
            mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                eq(USER_ID),
                eq(MembershipMemberType.USER),
                eq(MembershipReferenceType.API)
            )
        )
            .thenReturn(Collections.singleton(mApi));
        doReturn(Collections.emptySet()).when(mockGroupService).findByUser(USER_ID);

        List<UserMembership> references = membershipService.findUserMembership(
            io.gravitee.rest.api.model.MembershipReferenceType.API,
            USER_ID
        );

        assertFalse(references.isEmpty());
        assertEquals(1, references.size());
        assertEquals("api-id1", references.get(0).getReference());
        assertEquals("API", references.get(0).getType());
    }

    @Test
    public void shouldGetApiWithOnlyGroups() throws Exception {
        when(mockRoleService.findByScope(any())).thenReturn(Collections.emptyList());
        Membership mGroup = mock(Membership.class);
        when(mGroup.getReferenceId()).thenReturn("api-id2");

        when(
            mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                eq(USER_ID),
                eq(MembershipMemberType.USER),
                eq(MembershipReferenceType.API)
            )
        )
            .thenReturn(Collections.emptySet());

        when(
            mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                eq("GROUP"),
                eq(MembershipMemberType.GROUP),
                eq(MembershipReferenceType.API)
            )
        )
            .thenReturn(Collections.singleton(mGroup));
        GroupEntity group1 = mock(GroupEntity.class);
        doReturn("GROUP").when(group1).getId();
        doReturn(new HashSet<>(asList(group1))).when(mockGroupService).findByUser(USER_ID);

        List<UserMembership> references = membershipService.findUserMembership(
            io.gravitee.rest.api.model.MembershipReferenceType.API,
            USER_ID
        );

        assertFalse(references.isEmpty());
        assertEquals(1, references.size());
        assertEquals("api-id2", references.get(0).getReference());
        assertEquals("API", references.get(0).getType());
    }

    @Test
    public void shouldGetApiWithApiAndGroups() throws Exception {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId("role");
        roleEntity.setName("PO");
        roleEntity.setScope(RoleScope.API);
        when(mockRoleService.findByScope(any())).thenReturn(Collections.singletonList(roleEntity));
        Membership mApi = mock(Membership.class);
        when(mApi.getReferenceId()).thenReturn("api-id1");
        when(mApi.getRoleId()).thenReturn("role");

        Membership mGroup = mock(Membership.class);
        when(mGroup.getReferenceId()).thenReturn("api-id2");
        when(mApi.getRoleId()).thenReturn("role");

        when(
            mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                eq(USER_ID),
                eq(MembershipMemberType.USER),
                eq(MembershipReferenceType.API)
            )
        )
            .thenReturn(Collections.singleton(mApi));

        when(
            mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                eq("GROUP"),
                eq(MembershipMemberType.GROUP),
                eq(MembershipReferenceType.API)
            )
        )
            .thenReturn(Collections.singleton(mGroup));
        GroupEntity group1 = mock(GroupEntity.class);
        doReturn("GROUP").when(group1).getId();
        doReturn(new HashSet<>(asList(group1))).when(mockGroupService).findByUser(USER_ID);

        List<UserMembership> references = membershipService.findUserMembership(
            io.gravitee.rest.api.model.MembershipReferenceType.API,
            USER_ID
        );

        assertFalse(references.isEmpty());
        assertEquals(2, references.size());
        assertNotEquals(references.get(0).getReference(), references.get(1).getReference());
        assertTrue(references.get(0).getReference().equals("api-id1") || references.get(0).getReference().equals("api-id2"));
        assertTrue(references.get(1).getReference().equals("api-id1") || references.get(1).getReference().equals("api-id2"));
        assertEquals("API", references.get(0).getType());
    }

    @Test
    public void shouldGetMembershipForGivenSource() throws Exception {
        RoleEntity roleApi = new RoleEntity();
        roleApi.setId("roleApi");
        roleApi.setName("PO");
        roleApi.setScope(RoleScope.API);
        RoleEntity roleApp = new RoleEntity();
        roleApp.setId("roleApp");
        roleApp.setName("PO");
        roleApp.setScope(RoleScope.API);
        when(mockRoleService.findAll()).thenReturn(asList(roleApi, roleApp));

        Membership mApi = mock(Membership.class);
        when(mApi.getReferenceId()).thenReturn("api-id1");
        when(mApi.getRoleId()).thenReturn("roleApi");
        when(mApi.getSource()).thenReturn("oauth2");

        Membership mApp = mock(Membership.class);
        when(mApp.getReferenceId()).thenReturn("app-id1");
        when(mApp.getRoleId()).thenReturn("roleApp");
        when(mApp.getSource()).thenReturn("oauth2");

        when(
            mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndSource(
                eq(USER_ID),
                eq(MembershipMemberType.USER),
                eq(MembershipReferenceType.GROUP),
                eq("oauth2")
            )
        )
            .thenReturn(Sets.newHashSet(mApi, mApp));

        List<UserMembership> references = membershipService.findUserMembershipBySource(
            io.gravitee.rest.api.model.MembershipReferenceType.GROUP,
            USER_ID,
            "oauth2"
        );

        assertFalse(references.isEmpty());
        assertEquals(2, references.size());
        assertNotEquals(references.get(0).getReference(), references.get(1).getReference());
        assertTrue(references.get(0).getReference().equals("api-id1") || references.get(0).getReference().equals("app-id1"));
        assertTrue(references.get(1).getReference().equals("api-id1") || references.get(1).getReference().equals("app-id1"));
    }
}
