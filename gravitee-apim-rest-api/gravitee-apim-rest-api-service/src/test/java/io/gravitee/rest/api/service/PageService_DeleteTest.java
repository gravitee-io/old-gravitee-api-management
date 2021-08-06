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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.rest.api.model.PageConfigurationKeys;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.PageServiceImpl;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageService_DeleteTest {

    private static final String PAGE_ID = "ba01aef0-e3da-4499-81ae-f0e3daa4995a";

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private PageRepository pageRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Test
    public void shouldDeletePage() throws TechnicalException {
        Page page = mock(Page.class);
        when(page.getId()).thenReturn(PAGE_ID);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));

        pageService.delete(PAGE_ID);

        verify(pageRepository).delete(PAGE_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotDeletePageBecauseTechnicalException() throws TechnicalException {
        Page page = mock(Page.class);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));
        doThrow(TechnicalException.class).when(pageRepository).delete(PAGE_ID);

        pageService.delete(PAGE_ID);
    }

    @Captor
    ArgumentCaptor<String> idCaptor;

    @Test
    public void shouldDeleteAllByApi() throws TechnicalException {
        Page folder = mock(Page.class);
        String folderId = "folder";
        when(folder.getId()).thenReturn(folderId);
        when(folder.getType()).thenReturn(PageType.FOLDER.toString());
        String refId = "refId";
        when(folder.getReferenceId()).thenReturn(refId);
        when(folder.getOrder()).thenReturn(1);
        when(folder.getReferenceType()).thenReturn(PageReferenceType.ENVIRONMENT);
        when(pageRepository.findById(folderId)).thenReturn(Optional.of(folder));

        Page child = mock(Page.class);
        String childId = "child";
        when(child.getId()).thenReturn(childId);
        when(child.getOrder()).thenReturn(2);
        when(child.getType()).thenReturn(PageType.FOLDER.toString());
        when(child.getReferenceId()).thenReturn(refId);
        when(child.getReferenceType()).thenReturn(PageReferenceType.ENVIRONMENT);
        when(pageRepository.findById(childId)).thenReturn(Optional.of(child));

        Page childPage = mock(Page.class);
        String childPageId = "childPageId";
        when(childPage.getId()).thenReturn(childPageId);
        when(childPage.getType()).thenReturn(PageType.SWAGGER.toString());
        when(pageRepository.findById(childPageId)).thenReturn(Optional.of(childPage));

        Page link = mock(Page.class);
        String linkId = "link";
        when(link.getId()).thenReturn(linkId);
        when(link.getContent()).thenReturn(PAGE_ID);
        when(pageRepository.search(new PageCriteria.Builder().type("LINK").build())).thenReturn(Arrays.asList(link));

        Page page = mock(Page.class);
        when(page.getId()).thenReturn(PAGE_ID);
        when(page.getType()).thenReturn(PageType.MARKDOWN.toString());
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));

        Page translation = mock(Page.class);
        String translationId = "translation";
        when(translation.getId()).thenReturn(translationId);
        Map<String, String> configuration = new HashMap<>();
        configuration.put(PageConfigurationKeys.TRANSLATION_LANG, "EN");
        when(translation.getConfiguration()).thenReturn(configuration);
        when(translation.getType()).thenReturn(PageType.TRANSLATION.toString());
        when(pageRepository.search(new PageCriteria.Builder().parent(PAGE_ID).type(PageType.TRANSLATION.name()).build()))
            .thenReturn(Arrays.asList(translation));

        when(pageRepository.search(new PageCriteria.Builder().referenceType(PageReferenceType.API.name()).referenceId("apiId").build()))
            .thenReturn(Arrays.asList(folder, child, childPage, page));

        pageService.deleteAllByApi("apiId");

        verify(pageRepository, times(6)).delete(idCaptor.capture());
        assertEquals(
            "Pages are not deleted in order !",
            Arrays.asList(childPageId, PAGE_ID, linkId, translationId, childId, folderId),
            idCaptor.getAllValues()
        );
    }
}
