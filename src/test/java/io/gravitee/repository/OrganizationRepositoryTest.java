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
import io.gravitee.repository.management.model.Organization;

public class OrganizationRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/organization-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final Organization organization = new Organization();
        organization.setId("DEFAULT-ORG-create");
        organization.setName("Default org for create");
        organization.setDescription("Default org description for create");
        organization.setDomainRestrictions(Arrays.asList("domain", "restriction"));

        final Organization createdOrg = organizationRepository.create(organization);

        assertEquals(organization.getId(), createdOrg.getId());
        assertEquals(organization.getName(), createdOrg.getName());
        assertEquals(organization.getDescription(), createdOrg.getDescription());
        List<String> domainRestrictions = createdOrg.getDomainRestrictions();
        assertNotNull(domainRestrictions);
        assertEquals(2, domainRestrictions.size());
        assertTrue(domainRestrictions.contains("domain"));
        assertTrue(domainRestrictions.contains("restriction"));

        Optional<Organization> optional = organizationRepository.findById("DEFAULT-ORG-create");
        Assert.assertTrue("Organization to create not found", optional.isPresent());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Organization> optional = organizationRepository.findById("DEFAULT-ORG-update");
        Assert.assertTrue("Organization to update not found", optional.isPresent());
        assertEquals("Invalid saved Organization name.", "Default org for update", optional.get().getName());

        final Organization org = optional.get();
        org.setName("New name");

        final Organization fetchedOrganization = organizationRepository.update(org);
        assertEquals(org.getName(), fetchedOrganization.getName());
        
        optional = organizationRepository.findById("DEFAULT-ORG-update");
        Assert.assertTrue("Organization to update not found", optional.isPresent());
    }

    @Test
    public void shouldDelete() throws Exception {
        Optional<Organization> optional = organizationRepository.findById("DEFAULT-ORG-delete");
        Assert.assertTrue("Organization to delete not found", optional.isPresent());
        organizationRepository.delete("DEFAULT-ORG-delete");
        optional = organizationRepository.findById("DEFAULT-ORG-delete");
        Assert.assertFalse("Organization to delete has not been deleted", optional.isPresent());
    }

    @Test
    public void shouldFindById() throws Exception {
        Optional<Organization> optional = organizationRepository.findById("DEFAULT-ORG-findById");
        Assert.assertTrue("Organization to find not found", optional.isPresent());
    }
    
    @Test
    public void shouldFindAll() throws Exception {
        Set<Organization> allOrganizations = organizationRepository.findAll();
        Assert.assertTrue("No organization found", !allOrganizations.isEmpty());
    }
}
