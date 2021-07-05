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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.Environment;

public class EnvironmentRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/environment-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final Environment environment = new Environment();
        environment.setId("DEFAULT-create");
        environment.setName("Default env for create");
        environment.setDescription("Default env description for create");
        environment.setOrganizationId("DEFAULT-ORG");
        environment.setDomainRestrictions(Arrays.asList("domain", "restriction"));

        final Environment createdEnv = environmentRepository.create(environment);

        assertEquals(environment.getId(), createdEnv.getId());
        assertEquals(environment.getName(), createdEnv.getName());
        assertEquals(environment.getDescription(), createdEnv.getDescription());
        assertEquals(environment.getOrganizationId(), createdEnv.getOrganizationId());
        List<String> domainRestrictions = createdEnv.getDomainRestrictions();
        assertNotNull(domainRestrictions);
        assertEquals(2, domainRestrictions.size());
        assertTrue(domainRestrictions.contains("domain"));
        assertTrue(domainRestrictions.contains("restriction"));
        
        Optional<Environment> optional = environmentRepository.findById("DEFAULT-create");
        Assert.assertTrue("Environment to create not found", optional.isPresent());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Environment> optional = environmentRepository.findById("DEFAULT-update");
        Assert.assertTrue("Environment to update not found", optional.isPresent());
        assertEquals("Invalid saved Environment name.", "Default env for update", optional.get().getName());

        final Environment env = optional.get();
        env.setName("New name");

        final Environment fetchedEnvironment = environmentRepository.update(env);
        assertEquals(env.getName(), fetchedEnvironment.getName());
        
        optional = environmentRepository.findById("DEFAULT-update");
        Assert.assertTrue("Environment to update not found", optional.isPresent());
    }

    @Test
    public void shouldDelete() throws Exception {
        Optional<Environment> optional = environmentRepository.findById("DEFAULT-delete");
        Assert.assertTrue("Environment to delete not found", optional.isPresent());
        environmentRepository.delete("DEFAULT-delete");
        optional = environmentRepository.findById("DEFAULT-delete");
        Assert.assertFalse("Environment to delete has not been deleted", optional.isPresent());
    }

    @Test
    public void shouldFindById() throws Exception {
        Optional<Environment> optional = environmentRepository.findById("DEFAULT-findById");
        Assert.assertTrue("Environment to find not found", optional.isPresent());
    }
    
    @Test
    public void shouldFindAll() throws Exception {
        Set<Environment> allEnvironments = environmentRepository.findAll();
        Assert.assertTrue("No environment found", !allEnvironments.isEmpty());
    }
    
    @Test
    public void shouldFindByOrganization() throws Exception {
        Set<Environment> orgEnvironments = environmentRepository.findByOrganization("DEFAULT-ORG");
        Assert.assertTrue("No environment found", !orgEnvironments.isEmpty());
        Assert.assertEquals(1, orgEnvironments.size());
    }
}
