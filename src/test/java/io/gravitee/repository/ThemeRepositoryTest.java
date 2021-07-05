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
import io.gravitee.repository.management.model.Theme;
import org.junit.Test;

import java.util.*;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.Assert.*;

public class ThemeRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/theme-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Theme> themes = themeRepository.findAll();

        assertNotNull(themes);
        assertEquals(3, themes.size());
        final Theme themeSimple = themes.stream().filter(theme -> "simple".equals(theme.getId())).findAny().get();
        assertEquals("Theme simple", themeSimple.getName());
        assertEquals("backgroundImage", themeSimple.getBackgroundImage());
        assertEquals("{\"def\": \"value\"}", themeSimple.getDefinition());
        assertEquals("logo", themeSimple.getLogo());
        assertEquals("optionalLogo", themeSimple.getOptionalLogo());
        assertEquals("TEST", themeSimple.getReferenceId());
        assertEquals("ENVIRONMENT", themeSimple.getReferenceType());
        assertTrue(compareDate(1111111111111L, themeSimple.getUpdatedAt().getTime()));
        assertTrue(compareDate(1000002222222L, themeSimple.getCreatedAt().getTime()));
        assertEquals(false, themeSimple.isEnabled());
    }

    @Test
    public void shouldFindByReference() throws Exception {
        final Set<Theme> themes = themeRepository.findByReferenceIdAndReferenceType("DEFAULT", "ENVIRONMENT");
        assertNotNull(themes);
        assertEquals(2, themes.size());
        final Theme darkTheme = themes.stream().filter(theme -> "dark".equals(theme.getId())).findAny().get();
        assertEquals("Theme dark", darkTheme.getName());
        final Theme lightTheme = themes.stream().filter(theme -> "light".equals(theme.getId())).findAny().get();
        assertEquals("Light", lightTheme.getName());
    }

    @Test
    public void shouldCreate() throws Exception {
        final Theme theme = new Theme();
        theme.setId("new-theme");
        theme.setName("Theme dark");
        theme.setCreatedAt(new Date(1000000000000L));
        theme.setUpdatedAt(new Date(1111111111111L));
        theme.setReferenceId("DEFAULT");
        theme.setDefinition("{\"def\": \"value\"}");
        theme.setReferenceType("ENVIRONMENT");
        theme.setLogo("logo");
        theme.setOptionalLogo("optionalLogo");
        theme.setBackgroundImage("backgroundImage");
        theme.setEnabled(true);

        int nbThemesBeforeCreation = themeRepository.findAll().size();
        themeRepository.create(theme);
        int nbThemesAfterCreation = themeRepository.findAll().size();

        assertEquals(nbThemesBeforeCreation + 1, nbThemesAfterCreation);

        Optional<Theme> optional = themeRepository.findById("new-theme");
        assertTrue("Theme saved not found", optional.isPresent());

        final Theme themeSaved = optional.get();
        assertEquals("Invalid saved theme name.", theme.getName(), themeSaved.getName());
        assertEquals("backgroundImage", themeSaved.getBackgroundImage());
        assertEquals("{\"def\": \"value\"}", themeSaved.getDefinition());
        assertEquals("logo", themeSaved.getLogo());
        assertEquals("optionalLogo", themeSaved.getOptionalLogo());
        assertEquals("DEFAULT", themeSaved.getReferenceId());
        assertEquals("ENVIRONMENT", themeSaved.getReferenceType());
        assertTrue(compareDate(1111111111111L, themeSaved.getUpdatedAt().getTime()));
        assertTrue(compareDate(1000000000000L, themeSaved.getCreatedAt().getTime()));
        assertEquals(true, themeSaved.isEnabled());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Theme> optional = themeRepository.findById("light");
        assertTrue("Theme to update not found", optional.isPresent());
        assertEquals("Invalid saved theme name.", "Light", optional.get().getName());

        final Theme theme = optional.get();
        theme.setName("Awesome");
        theme.setLogo("updateLogo");
        theme.setOptionalLogo(null);
        theme.setBackgroundImage("updateBackground");
        theme.setEnabled(true);
        theme.setDefinition("{\"def\": \"test\"}");
        theme.setReferenceType("PLATFORM");
        theme.setReferenceId("TEST");
        theme.setCreatedAt(new Date(1010101010101L));
        theme.setUpdatedAt(new Date(1030141710801L));

        int nbThemeBeforeUpdate = themeRepository.findAll().size();
        themeRepository.update(theme);
        int nbThemeAfterUpdate = themeRepository.findAll().size();

        assertEquals(nbThemeBeforeUpdate, nbThemeAfterUpdate);

        Optional<Theme> optionalUpdated = themeRepository.findById("light");
        assertTrue("Theme to update not found", optionalUpdated.isPresent());

        final Theme themeUpdated = optionalUpdated.get();

        assertEquals("Invalid saved theme name.", "Awesome", themeUpdated.getName());
        assertEquals("updateBackground", themeUpdated.getBackgroundImage());
        assertEquals("{\"def\": \"test\"}", themeUpdated.getDefinition());
        assertEquals("updateLogo", themeUpdated.getLogo());
        assertEquals(null, themeUpdated.getOptionalLogo());
        assertEquals("TEST", themeUpdated.getReferenceId());
        assertEquals("PLATFORM", themeUpdated.getReferenceType());
        assertTrue(compareDate(1030141710801L, themeUpdated.getUpdatedAt().getTime()));
        assertTrue(compareDate(1010101010101L, themeUpdated.getCreatedAt().getTime()));
        assertEquals(true, themeUpdated.isEnabled());
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbThemesBeforeDeletion = themeRepository.findAll().size();
        themeRepository.delete("dark");
        int nbThemesAfterDeletion = themeRepository.findAll().size();
        assertEquals(nbThemesBeforeDeletion - 1, nbThemesAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownTheme() throws Exception {
        Theme unknownTheme = new Theme();
        unknownTheme.setId("unknown");
        themeRepository.update(unknownTheme);
        fail("An unknown theme should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        themeRepository.update(null);
        fail("A null theme should not be updated");
    }
}
