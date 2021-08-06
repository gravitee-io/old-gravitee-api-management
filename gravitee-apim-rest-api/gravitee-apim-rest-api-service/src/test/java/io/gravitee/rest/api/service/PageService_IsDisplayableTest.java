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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.impl.PageServiceImpl;
import java.util.Collections;
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
public class PageService_IsDisplayableTest {

    public static final String USERNAME = "johndoe";

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private MembershipService membershipServiceMock;

    /**
     * *************************
     * Tests for published pages
     * *************************
     */
    @Test
    public void shouldBeDisplayablePublicApiAndPublishedPageAsUnauthenticated() throws TechnicalException {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(Visibility.PUBLIC).when(apiEntityMock).getVisibility();

        boolean displayable = pageService.isDisplayable(apiEntityMock, true, null);

        assertTrue(displayable);
        verify(membershipServiceMock, never()).getUserMember(any(), any(), any());
    }

    @Test
    public void shouldBeDisplayablePublicApiAndPublishedPageAsAuthenticated() throws TechnicalException {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(Visibility.PUBLIC).when(apiEntityMock).getVisibility();

        boolean displayable = pageService.isDisplayable(apiEntityMock, true, USERNAME);

        assertTrue(displayable);
        verify(membershipServiceMock, never()).getUserMember(any(), any(), any());
    }

    @Test
    public void shouldNotBeDisplayablePrivateApiAndPublishedPageAsUnauthenticated() throws TechnicalException {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(Visibility.PRIVATE).when(apiEntityMock).getVisibility();

        boolean displayable = pageService.isDisplayable(apiEntityMock, true, null);

        assertFalse(displayable);
        verify(membershipServiceMock, never()).getUserMember(any(), any(), any());
    }

    @Test
    public void shouldNotBeDisplayablePrivateApiAndPublishedPageAsNotApiMember() throws TechnicalException {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(Visibility.PRIVATE).when(apiEntityMock).getVisibility();
        doReturn(null).when(membershipServiceMock).getUserMember(eq(MembershipReferenceType.API), any(), eq(USERNAME));

        boolean displayable = pageService.isDisplayable(apiEntityMock, true, USERNAME);

        assertFalse(displayable);
        verify(membershipServiceMock, times(1)).getUserMember(eq(MembershipReferenceType.API), any(), eq(USERNAME));
        verify(membershipServiceMock, never()).getUserMember(eq(MembershipReferenceType.GROUP), any(), any());
    }

    @Test
    public void shouldBeDisplayablePrivateApiAndPublishedPageAsApiMember() throws TechnicalException {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(Visibility.PRIVATE).when(apiEntityMock).getVisibility();
        doReturn(mock(MemberEntity.class)).when(membershipServiceMock).getUserMember(eq(MembershipReferenceType.API), any(), eq(USERNAME));

        boolean displayable = pageService.isDisplayable(apiEntityMock, true, USERNAME);

        assertTrue(displayable);
        verify(membershipServiceMock, times(1)).getUserMember(eq(MembershipReferenceType.API), any(), eq(USERNAME));
        verify(membershipServiceMock, never()).getUserMember(eq(MembershipReferenceType.GROUP), any(), any());
    }

    @Test
    public void shouldBeDisplayablePrivateApiAndPublishedPageAsGroupApiMember() throws TechnicalException {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(Visibility.PRIVATE).when(apiEntityMock).getVisibility();
        when(apiEntityMock.getGroups()).thenReturn(Collections.singleton("groupid"));
        doReturn(null).when(membershipServiceMock).getUserMember(eq(MembershipReferenceType.API), any(), eq(USERNAME));
        doReturn(mock(MemberEntity.class))
            .when(membershipServiceMock)
            .getUserMember(eq(MembershipReferenceType.GROUP), any(), eq(USERNAME));

        boolean displayable = pageService.isDisplayable(apiEntityMock, true, USERNAME);

        assertTrue(displayable);
        verify(membershipServiceMock, times(1)).getUserMember(eq(MembershipReferenceType.API), any(), eq(USERNAME));
        verify(membershipServiceMock, times(1)).getUserMember(eq(MembershipReferenceType.GROUP), any(), eq(USERNAME));
    }

    /**
     * *****************************
     * Tests for not published pages
     * *****************************
     */
    @Test
    public void shouldNotBeDisplayablePublicApiAndUnpublishedPageAsUnauthenticated() throws TechnicalException {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(Visibility.PUBLIC).when(apiEntityMock).getVisibility();

        boolean displayable = pageService.isDisplayable(apiEntityMock, false, null);

        assertFalse(displayable);
        verify(membershipServiceMock, never()).getUserMember(any(), any(), any());
    }
}
