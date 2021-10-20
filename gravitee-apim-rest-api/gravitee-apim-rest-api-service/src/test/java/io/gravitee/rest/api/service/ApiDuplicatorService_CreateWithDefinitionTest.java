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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import io.gravitee.rest.api.service.impl.ApiDuplicatorServiceImpl;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author Nicolas Geraud (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiDuplicatorService_CreateWithDefinitionTest {

    private static final String API_ID = "id-api";
    private static final String SOURCE = "source";

    protected ApiDuplicatorService apiDuplicatorService;

    @Spy
    private ObjectMapper objectMapper = (new ServiceConfiguration()).objectMapper();

    @Mock
    private ApiService apiService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private PageService pageService;

    @Mock
    private UserService userService;

    @Mock
    private PlanService planService;

    @Mock
    private GroupService groupService;

    @Mock
    private RoleService roleService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private HttpClientService httpClientService;

    @Mock
    private ImportConfiguration importConfiguration;

    @Mock
    private MediaService mediaService;

    @Before
    public void setup() {
        apiDuplicatorService =
            new ApiDuplicatorServiceImpl(
                httpClientService,
                importConfiguration,
                mediaService,
                objectMapper,
                apiMetadataService,
                membershipService,
                roleService,
                pageService,
                planService,
                groupService,
                userService,
                apiService
            );
    }

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }

    @Test
    public void shouldCreateImportApiWithMembersAndPages() throws IOException, TechnicalException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);
        when(userService.findBySource(anyString(), anyString(), eq(false))).thenReturn(new UserEntity());
        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), eq(RoleScope.API))).thenReturn(poRoleEntity);
        RoleEntity ownerRoleEntity = new RoleEntity();
        ownerRoleEntity.setId("API_OWNER");

        MemberEntity po = new MemberEntity();
        po.setId("admin");
        po.setReferenceId(API_ID);
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Arrays.asList(poRoleEntity));
        MemberEntity owner = new MemberEntity();
        owner.setId("user");
        owner.setReferenceId(API_ID);
        owner.setReferenceType(MembershipReferenceType.API);
        owner.setRoles(Arrays.asList(ownerRoleEntity));
        when(membershipService.getMembersByReference(any(), any())).thenReturn(Collections.singleton(po));

        UserEntity admin = new UserEntity();
        admin.setId(po.getId());
        admin.setSource(SOURCE);
        admin.setSourceId("ref-admin");
        UserEntity user = new UserEntity();
        user.setId(owner.getId());
        user.setSource(SOURCE);
        user.setSourceId("ref-user");
        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(admin.getId());
        memberEntity.setRoles(Collections.singletonList(poRoleEntity));
        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);
        when(userService.findById(memberEntity.getId())).thenReturn(admin);

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            "admin",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1))
            .duplicatePages(argThat(pagesList -> pagesList.size() == 2), eq(GraviteeContext.getCurrentEnvironment()), eq(API_ID));
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(MembershipReferenceType.API, API_ID, MembershipMemberType.USER, user.getId(), "API_OWNER");
    }

    @Test
    public void shouldCreateImportApiWithMembers() throws IOException, TechnicalException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId("ref-admin");
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId("ref-user");
        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);
        when(userService.findById(admin.getId())).thenReturn(admin);

        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), eq(RoleScope.API))).thenReturn(poRoleEntity);
        RoleEntity ownerRoleEntity = new RoleEntity();
        ownerRoleEntity.setId("API_OWNER");

        MemberEntity po = new MemberEntity();
        po.setId("admin");
        po.setReferenceId(API_ID);
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Arrays.asList(poRoleEntity));
        MemberEntity owner = new MemberEntity();
        owner.setId("user");
        owner.setReferenceId(API_ID);
        owner.setReferenceType(MembershipReferenceType.API);
        owner.setRoles(Arrays.asList(ownerRoleEntity));
        when(membershipService.getMembersByReference(any(), any())).thenReturn(Collections.singleton(po));

        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(admin.getId());
        memberEntity.setRoles(Collections.singletonList(poRoleEntity));

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            "admin",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1)).createPage(eq(API_ID), any(NewPageEntity.class), eq(GraviteeContext.getCurrentEnvironment()));
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(MembershipReferenceType.API, API_ID, MembershipMemberType.USER, user.getId(), "API_OWNER");
        verify(membershipService, never()).transferApiOwnership(any(), any(), any());
    }

    @Test
    public void shouldCreateImportApiWithPages() throws IOException, TechnicalException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId("ref-admin");
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId("ref-user");

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            "admin",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1))
            .duplicatePages(argThat(pagesList -> pagesList.size() == 2), eq(GraviteeContext.getCurrentEnvironment()), eq(API_ID));
    }

    @Test
    public void shouldCreateImportApiWithOnlyDefinition() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId(API_ID);

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            "admin",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1)).createPage(any(), any(NewPageEntity.class), eq(GraviteeContext.getCurrentEnvironment()));
    }

    @Test
    public void shouldCreateImportApiWithOnlyDefinitionWithPrimaryOwner() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+primaryOwner.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId(API_ID);

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            "admin",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1)).createPage(any(), any(NewPageEntity.class), eq(GraviteeContext.getCurrentEnvironment()));
    }

    @Test
    public void shouldCreateImportApiWithOnlyNewDefinition() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-new-api.definition.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId(API_ID);

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            "admin",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());

        verify(pageService, times(1)).createPage(any(), any(NewPageEntity.class), eq(GraviteeContext.getCurrentEnvironment()));
    }

    @Test
    public void shouldCreateImportApiWithOnlyDefinitionEnumLowerCase() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition_enum_lowercase.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            "admin",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1)).createPage(eq(API_ID), any(NewPageEntity.class), eq(GraviteeContext.getCurrentEnvironment()));
    }

    @Test
    public void shouldCreateImportApiWithPlans() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+plans.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId(API_ID);

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            "admin",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());

        verify(planService, times(2)).createOrUpdatePlan(any(PlanEntity.class), any(String.class));
    }

    @Test
    public void shouldCreateImportApiWithMetadata() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+metadata.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            "admin",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());

        verify(apiMetadataService, times(2)).update(any(UpdateApiMetadataEntity.class));
    }

    @Test
    public void shouldCreateImportApiEvenIfMemberRoleIsInvalid() throws IOException, TechnicalException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId("ref-admin");
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId("ref-user");
        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);
        when(userService.findById(admin.getId())).thenReturn(admin);

        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), eq(RoleScope.API))).thenReturn(poRoleEntity);

        MemberEntity po = new MemberEntity();
        po.setId("admin");
        po.setReferenceId(API_ID);
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Arrays.asList(poRoleEntity));
        when(membershipService.getMembersByReference(any(), any())).thenReturn(Collections.singleton(po));

        when(
            membershipService.addRoleToMemberOnReference(
                MembershipReferenceType.API,
                API_ID,
                MembershipMemberType.USER,
                user.getId(),
                "API_OWNER"
            )
        )
            .thenThrow(new RoleNotFoundException("API_OWNER Not found"));

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            "admin",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1)).createPage(eq(API_ID), any(NewPageEntity.class), eq(GraviteeContext.getCurrentEnvironment()));
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(MembershipReferenceType.API, API_ID, MembershipMemberType.USER, user.getId(), "API_OWNER");
        verify(membershipService, never()).transferApiOwnership(any(), any(), any());
    }
}
