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
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import org.junit.Test;

import java.util.*;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.Assert.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/page-tests/";
    }

    @Test
    public void shouldFindApiPageById() throws Exception {
        final Optional<Page> page = pageRepository.findById("FindApiPage");

        assertNotNull(page);
        assertTrue(page.isPresent());
        assertFindPage(page.get());
    }

    private void assertFindPage(Page page) {
        assertEquals("id", "FindApiPage", page.getId());
        assertEquals("name", "Find apiPage by apiId or Id", page.getName());
        assertEquals("content", "Content of the page", page.getContent());
        assertEquals("reference id", "my-api", page.getReferenceId());
        assertEquals("reference type", PageReferenceType.API, page.getReferenceType());
        assertEquals("type", "MARKDOWN", page.getType());
        assertEquals("last contributor", "john_doe", page.getLastContributor());
        assertEquals("order", 2, page.getOrder());
        assertTrue("published", page.isPublished());

        assertEquals("source type", "sourceType", page.getSource().getType());
        assertEquals("source configuration", "sourceConfiguration", page.getSource().getConfiguration());

        assertEquals("configuration try it", "true", page.getConfiguration().get("tryIt"));
        assertEquals("configuration try it URL", "http://company.com", page.getConfiguration().get("tryItURL"));
        assertEquals("configuration show URL", "true", page.getConfiguration().get("showURL"));
        assertEquals("configuration display operation id", "true", page.getConfiguration().get("displayOperationId"));
        assertEquals("configuration doc expansion", "FULL", page.getConfiguration().get("docExpansion"));
        assertEquals("configuration enable filtering", "true", page.getConfiguration().get("enableFiltering"));
        assertEquals("configuration show extensions", "true", page.getConfiguration().get("showExtensions"));
        assertEquals("configuration show common extensions", "true", page.getConfiguration().get("showCommonExtensions"));
        assertEquals("configuration maxDisplayedTags", "1234", page.getConfiguration().get("maxDisplayedTags"));
        assertEquals("metadata edit_url", "http://provider.com/edit/page", page.getMetadata().get("edit_url"));
        assertEquals("metadata size", "256", page.getMetadata().get("size"));

        assertTrue("homepage", page.isHomepage());
        assertEquals("excludedGroups", Arrays.asList("grp1", "grp2"), page.getExcludedGroups());
        assertTrue("created at", compareDate(new Date(1486771200000L), page.getCreatedAt()));
        assertTrue("updated at", compareDate(new Date(1486771200000L), page.getUpdatedAt()));
    }

    @Test
    public void shouldCreateApiPage() throws Exception {
        final Page page = new Page();
        page.setId("new-page");
        page.setName("Page name");
        page.setContent("Page content");
        page.setOrder(3);
        page.setReferenceId("my-api");
        page.setReferenceType(PageReferenceType.API);
        page.setHomepage(true);
        page.setType("MARKDOWN");
        page.setParentId("2");
        page.setCreatedAt(new Date());
        page.setUpdatedAt(new Date());

        final Map<String, String> configuration = new HashMap<>();
        configuration.put("displayOperationId", "true");
        configuration.put("docExpansion", "FULL");
        configuration.put("showCommonExtensions", "true");
        configuration.put("maxDisplayedTags", "1234");
        configuration.put("enableFiltering", "true");
        configuration.put("tryItURL", "http://company.com");
        configuration.put("showURL", "true");
        configuration.put("showExtensions", "true");
        configuration.put("tryIt", "true");
        page.setConfiguration(configuration);

        final Map<String, String> metadata = new HashMap<>();
        metadata.put("edit_url", "url");
        metadata.put("size", "10");
        page.setMetadata(metadata);

        Optional<Page> optionalBefore = pageRepository.findById("new-page");
        pageRepository.create(page);
        Optional<Page> optionalAfter = pageRepository.findById("new-page");
        assertFalse("Page not found before", optionalBefore.isPresent());
        assertTrue("Page saved not found", optionalAfter.isPresent());

        final Page pageSaved = optionalAfter.get();
        assertEquals("Invalid saved page name.", page.getName(), pageSaved.getName());
        assertEquals("Invalid page content.", page.getContent(), pageSaved.getContent());
        assertEquals("Invalid page order.", page.getOrder(), pageSaved.getOrder());
        assertEquals("Invalid page type.", page.getType(), pageSaved.getType());
        assertEquals("Invalid homepage flag.", page.isHomepage(), pageSaved.isHomepage());
        assertEquals("Invalid parentId.", page.getParentId(), pageSaved.getParentId());
        assertNull("Invalid page source.", page.getSource());
        assertEquals("Invalid configuration.", page.getConfiguration(), pageSaved.getConfiguration());
        assertEquals("Invalid metadata.", page.getMetadata(), pageSaved.getMetadata());
    }

    @Test
    public void shouldCreateApiFolderPage() throws Exception {
        final Page page = new Page();
        page.setId("new-page-folder");
        page.setName("Folder name");
        page.setOrder(3);
        page.setReferenceId("my-api");
        page.setReferenceType(PageReferenceType.API);
        page.setHomepage(false);
        page.setParentId("");
        page.setType("FOLDER");
        page.setCreatedAt(new Date());
        page.setUpdatedAt(new Date());

        Optional<Page> optionalBefore = pageRepository.findById("new-page-folder");
        pageRepository.create(page);
        Optional<Page> optionalAfter = pageRepository.findById("new-page-folder");
        assertFalse("Page not found before", optionalBefore.isPresent());
        assertTrue("Page saved not found", optionalAfter.isPresent());

        final Page pageSaved = optionalAfter.get();
        assertEquals("Invalid saved page name.", page.getName(), pageSaved.getName());
        assertEquals("Invalid page content.", page.getContent(), pageSaved.getContent());
        assertEquals("Invalid page order.", page.getOrder(), pageSaved.getOrder());
        assertEquals("Invalid page type.", page.getType(), pageSaved.getType());
        assertEquals("Invalid ParentId.", page.getParentId(), pageSaved.getParentId());
        assertNull("Invalid page source.", page.getSource());
    }

    @Test
    public void shouldCreatePortalPage() throws Exception {
        final Page page = new Page();
        page.setId("new-portal-page");
        page.setReferenceId("DEFAULT");
        page.setReferenceType(PageReferenceType.ENVIRONMENT);
        page.setName("Page name");
        page.setContent("Page content");
        page.setOrder(3);
        page.setType("MARKDOWN");
        page.setParentId("2");
        page.setCreatedAt(new Date());
        page.setUpdatedAt(new Date());

        final Map<String, String> configuration = new HashMap<>();
        configuration.put("displayOperationId", "true");
        configuration.put("docExpansion", "FULL");
        configuration.put("showCommonExtensions", "true");
        configuration.put("maxDisplayedTags", "1234");
        configuration.put("enableFiltering", "true");
        configuration.put("tryItURL", "http://company.com");
        configuration.put("showURL", "true");
        configuration.put("showExtensions", "true");
        configuration.put("tryIt", "true");
        page.setConfiguration(configuration);

        final Map<String, String> metadata = new HashMap<>();
        metadata.put("edit_url", "url");
        metadata.put("size", "10");
        page.setMetadata(metadata);

        Optional<Page> optionalBefore = pageRepository.findById("new-portal-page");
        pageRepository.create(page);
        Optional<Page> optionalAfter = pageRepository.findById("new-portal-page");
        assertFalse("Page not found before", optionalBefore.isPresent());
        assertTrue("Page saved not found", optionalAfter.isPresent());

        final Page pageSaved = optionalAfter.get();
        assertEquals("Invalid saved page name.", page.getName(), pageSaved.getName());
        assertEquals("Invalid page content.", page.getContent(), pageSaved.getContent());
        assertEquals("Invalid page order.", page.getOrder(), pageSaved.getOrder());
        assertEquals("Invalid page type.", page.getType(), pageSaved.getType());
        assertEquals("Invalid parentId.", page.getParentId(), pageSaved.getParentId());
        assertEquals("Invalid homepage flag.", page.isHomepage(), pageSaved.isHomepage());
        assertNull("Invalid page source.", page.getSource());
        assertEquals("Invalid configuration.", page.getConfiguration(), pageSaved.getConfiguration());
        assertEquals("Invalid metadata.", page.getMetadata(), pageSaved.getMetadata());
    }

    @Test
    public void shouldCreatePortalFolderPage() throws Exception {
        final Page page = new Page();
        page.setId("new-portal-page-folder");
        page.setReferenceId("DEFAULT");
        page.setReferenceType(PageReferenceType.ENVIRONMENT);
        page.setName("Folder name");
        page.setOrder(3);
        page.setType("FOLDER");
        page.setCreatedAt(new Date());
        page.setUpdatedAt(new Date());
        page.setParentId("");

        Optional<Page> optionalBefore = pageRepository.findById("new-portal-page-folder");
        pageRepository.create(page);
        Optional<Page> optionalAfter = pageRepository.findById("new-portal-page-folder");
        assertFalse("Page not found before", optionalBefore.isPresent());
        assertTrue("Page saved not found", optionalAfter.isPresent());

        final Page pageSaved = optionalAfter.get();
        assertEquals("Invalid saved page name.", page.getName(), pageSaved.getName());
        assertEquals("Invalid page content.", page.getContent(), pageSaved.getContent());
        assertEquals("Invalid page order.", page.getOrder(), pageSaved.getOrder());
        assertEquals("Invalid page type.", page.getType(), pageSaved.getType());
        assertEquals("Invalid ParentId.", page.getParentId(), pageSaved.getParentId());
        assertNull("Invalid page source.", page.getSource());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Page> optionalBefore = pageRepository.findById("updatePage");
        assertTrue("Page to update not found", optionalBefore.isPresent());
        assertEquals("Invalid page name.", "Update Page", optionalBefore.get().getName());
        assertEquals("Invalid page content.", "Content of the update page", optionalBefore.get().getContent());
        final Page page = optionalBefore.get();
        page.setId("updatePage");
        page.setName("New name");
        page.setContent("New content");
        page.setReferenceId("my-api-2");
        page.setReferenceType(PageReferenceType.API);
        page.setType("SWAGGER");
        page.setOrder(1);
        page.setUpdatedAt(new Date(1486771200000L));
        page.setCreatedAt(new Date(1486772200000L));
        page.setParentId("parent-123");
        page.setHomepage(true);
        page.setExcludedGroups(Collections.singletonList("excluded"));
        page.setLastContributor("me");
        page.setPublished(true);
        page.setConfiguration(new HashMap<>());
        page.getConfiguration().put("tryIt", "true");
        page.getConfiguration().put("tryItURL", "http://company.com");
        page.getConfiguration().put("showURL", "true");
        page.getConfiguration().put("displayOperationId", "true");
        page.getConfiguration().put("docExpansion", "FULL");
        page.getConfiguration().put("enableFiltering", "true");
        page.getConfiguration().put("showExtensions", "true");
        page.getConfiguration().put("showCommonExtensions", "true");
        page.getConfiguration().put("maxDisplayedTags", "1234");

        final Map<String, String> metadata = new HashMap<>();
        metadata.put("edit_url", "url");
        metadata.put("size", "10");
        page.setMetadata(metadata);

        assertUpdatedPage(pageRepository.update(page));
        assertUpdatedPage(pageRepository.findById("updatePage").get());
    }

    private void assertUpdatedPage(final Page updatedPage) {
        assertNotNull("Page to update not found", updatedPage);
        assertEquals("Invalid saved page name.", "New name", updatedPage.getName());
        assertEquals("Invalid page content.", "New content", updatedPage.getContent());
        assertEquals("Invalid reference id.", "my-api-2", updatedPage.getReferenceId());
        assertEquals("Invalid reference type.", PageReferenceType.API, updatedPage.getReferenceType());
        assertEquals("Invalid type.", "SWAGGER", updatedPage.getType());
        assertEquals("Invalid order.", 1, updatedPage.getOrder());
        assertTrue("Invalid updatedAt.", compareDate(new Date(1486771200000L), updatedPage.getUpdatedAt()));
        assertTrue("Invalid createdAt.", compareDate(new Date(1486772200000L), updatedPage.getCreatedAt()));
        assertEquals("Invalid parent id.", "parent-123", updatedPage.getParentId());
        assertTrue("Invalid homepage.", updatedPage.isHomepage());
        assertTrue("Invalid excluded groups.", updatedPage.isHomepage());
        assertEquals("Invalid last contributor.", "me", updatedPage.getLastContributor());
        assertTrue("Invalid published.", updatedPage.isPublished());

        assertEquals("configuration try it", "true", updatedPage.getConfiguration().get("tryIt"));
        assertEquals("configuration try it URL", "http://company.com", updatedPage.getConfiguration().get("tryItURL"));
        assertEquals("configuration show URL", "true", updatedPage.getConfiguration().get("showURL"));
        assertEquals("configuration display operation id", "true", updatedPage.getConfiguration().get("displayOperationId"));
        assertEquals("configuration doc expansion", "FULL", updatedPage.getConfiguration().get("docExpansion"));
        assertEquals("configuration enable filtering", "true", updatedPage.getConfiguration().get("enableFiltering"));
        assertEquals("configuration show extensions", "true", updatedPage.getConfiguration().get("showExtensions"));
        assertEquals("configuration show common extensions", "true", updatedPage.getConfiguration().get("showCommonExtensions"));
        assertEquals("configuration maxDisplayedTags", "1234", updatedPage.getConfiguration().get("maxDisplayedTags"));
        assertEquals("metadata edit_url", "url", updatedPage.getMetadata().get("edit_url"));
        assertEquals("metadata size", "10", updatedPage.getMetadata().get("size"));
    }

    @Test
    public void shouldUpdateFolderPage() throws Exception {
        Optional<Page> optionalBefore = pageRepository.findById("updatePageFolder");
        assertTrue("Page to update not found", optionalBefore.isPresent());
        assertEquals("Invalid page name.", "Update Page Folder", optionalBefore.get().getName());
        assertEquals("Invalid page content.", "Content of the update page folder", optionalBefore.get().getContent());
        assertEquals("Invalid page parentId.", "2", optionalBefore.get().getParentId());

        final Page page = optionalBefore.get();
        page.setId("updatePageFolder");
        page.setName("New name page folder");
        page.setContent("New content page folder");
        page.setParentId("3");

        assertUpdatePageFolder(pageRepository.update(page));
        assertUpdatePageFolder(pageRepository.findById("updatePageFolder").get());
    }

    private void assertUpdatePageFolder(final Page updatedPage) {
        assertNotNull("Page to update not found", updatedPage);
        assertEquals("Invalid saved page name.", "New name page folder", updatedPage.getName());
        assertEquals("Invalid page content.", "New content page folder", updatedPage.getContent());
        assertEquals("Invalid page parentId.", "3", updatedPage.getParentId());
        assertEquals("Invalid page reference type.", PageReferenceType.API, updatedPage.getReferenceType());
        assertEquals("Invalid page reference id.", "my-api-3", updatedPage.getReferenceId());
    }

    @Test
    public void shouldDelete() throws Exception {
        Optional<Page> pageShouldExists = pageRepository.findById("page-to-be-deleted");
        pageRepository.delete("page-to-be-deleted");
        Optional<Page> pageShouldNotExists = pageRepository.findById("page-to-be-deleted");

        assertTrue("should exists before delete", pageShouldExists.isPresent());
        assertFalse("should not exists after delete", pageShouldNotExists.isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownPage() throws Exception {
        Page unknownPage = new Page();
        unknownPage.setId("unknown");
        unknownPage.setReferenceId("DEFAULT");
        unknownPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        pageRepository.update(unknownPage);
        fail("An unknown page should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        pageRepository.update(null);
        fail("A null page should not be updated");
    }

    @Test
    public void shouldFindMaxApiPageOrderByApiId() throws TechnicalException {
        Integer maxApiPageOrderByApiId = pageRepository.findMaxPageReferenceIdAndReferenceTypeOrder("my-api-2", PageReferenceType.API);
        assertEquals(Integer.valueOf(2), maxApiPageOrderByApiId);
    }

    @Test
    public void shouldFindDefaultMaxApiPageOrderByApiId() throws TechnicalException {
        Integer maxApiPageOrderByApiId = pageRepository.findMaxPageReferenceIdAndReferenceTypeOrder("unknown api id", PageReferenceType.API);
        assertEquals(Integer.valueOf(0), maxApiPageOrderByApiId);
    }

    @Test
    public void shouldFindMaxPortalPageOrder() throws TechnicalException {
        Integer maxPortalPageOrder = pageRepository.findMaxPageReferenceIdAndReferenceTypeOrder("DEFAULT", PageReferenceType.ENVIRONMENT);
        assertEquals(Integer.valueOf(20), maxPortalPageOrder);
    }
}
