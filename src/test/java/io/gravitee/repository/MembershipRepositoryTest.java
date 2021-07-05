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
import io.gravitee.repository.management.model.*;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.MembershipReferenceType.API;
import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.Assert.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class MembershipRepositoryTest extends AbstractRepositoryTest {
    @Override
    protected String getTestCasesPath() {
        return "/data/membership-tests/";
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Optional<Membership> membership = membershipRepository.findById("api1_user1");
        assertTrue("There is a membership", membership.isPresent());
        assertTrue( membership.get().getRoleId().equals("API_OWNER"));
        assertEquals("api1", membership.get().getReferenceId());
        assertEquals("user1", membership.get().getMemberId());
        assertEquals(API, membership.get().getReferenceType());
        assertEquals("myIdp", membership.get().getSource());
        assertTrue(compareDate(new Date(1439022010883L), membership.get().getUpdatedAt()));
        assertTrue(compareDate(new Date(1439022010883L), membership.get().getCreatedAt()));
        assertEquals("API_OWNER", membership.get().getRoleId());
    }

    @Test
    public void shouldNotFindById() throws TechnicalException {
        Optional<Membership> membership = membershipRepository.findById( "api1");
        assertFalse(membership.isPresent());
    }

    @Test
    public void shouldFindAllApiMembers() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByReferenceAndRoleId(MembershipReferenceType.API, "api1", null);
        assertNotNull("result must not be null", memberships);
        assertTrue(!memberships.isEmpty());
        assertEquals("user1", memberships.iterator().next().getMemberId());
    }

    @Test
    public void shouldFindAllApisMembers() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByReferencesAndRoleId(MembershipReferenceType.API, Arrays.asList("api2", "api3"),  null);
        assertNotNull("result must not be null", memberships);
        assertEquals(2, memberships.size());
        Membership membership1 = new Membership("api2_user2", "user2", MembershipMemberType.USER, "api2", MembershipReferenceType.API, "API_OWNER");
        membership1.setId("api2_user2");
        Membership membership2 = new Membership("api3_user3", "user3", MembershipMemberType.USER, "api3", MembershipReferenceType.API, "API_USER");
        membership2.setId("api3_user3");
        Set<Membership> expectedResult = new HashSet<>(Arrays.asList(membership1, membership2));
        assertTrue("must contain api2 and api3", memberships.containsAll(expectedResult));
    }

    @Test
    public void shouldFindApisOwners() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByReferencesAndRoleId(MembershipReferenceType.API, Arrays.asList("api2", "api3"), "API_OWNER");
        assertNotNull("result must not be null", memberships);
        assertEquals(1, memberships.size());
        Membership membership1 = new Membership("api2_user2", "user2", MembershipMemberType.USER, "api2", MembershipReferenceType.API, "API_OWNER");
        membership1.setId("api2_user2");
        Set<Membership> expectedResult = new HashSet<>(Collections.singletonList(membership1));
        assertTrue("must contain api2", memberships.containsAll(expectedResult));
    }

    @Test
    public void shouldFindMembersApis() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByMemberIdsAndMemberTypeAndReferenceType(Arrays.asList("user2", "user3"), MembershipMemberType.USER, MembershipReferenceType.API);
        assertNotNull("result must not be null", memberships);
        assertEquals(2, memberships.size());
        Membership membership2 = new Membership("api2_user2", "user2", MembershipMemberType.USER, "api2", MembershipReferenceType.API, "API_OWNER");
        Membership membership3 = new Membership("api3_user3", "user3", MembershipMemberType.USER, "api3", MembershipReferenceType.API, "API_OWNER");
        Set<Membership> expectedResult = new HashSet<>(Arrays.asList(membership2, membership3));
        assertTrue("must contain api2 and api3", memberships.containsAll(expectedResult));
    }

    @Test
    public void shouldFindApiOwner() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByReferenceAndRoleId(MembershipReferenceType.API, "api1", "API_OWNER");
        assertNotNull("result must not be null", memberships);
        assertTrue(!memberships.isEmpty());
        assertEquals("user1", memberships.iterator().next().getMemberId());
    }

    @Test
    public void shouldFindByMemberIdAndMemberTypeAndReferenceType() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceType("user1", MembershipMemberType.USER, MembershipReferenceType.API);
        assertNotNull("result must not be null", memberships);
        assertTrue(!memberships.isEmpty());
        assertEquals("api1", memberships.iterator().next().getReferenceId());
    }

    @Test
    public void shouldfindByMemberIdAndMemberTypeAndReferenceTypeAndSource() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndSource("user1", MembershipMemberType.USER, MembershipReferenceType.API, "myIdp");
        assertNotNull("result must not be null", memberships);
        assertTrue(!memberships.isEmpty());
        assertEquals("api1", memberships.iterator().next().getReferenceId());
    }

    @Test
    public void shouldFindByMemberIdAndMemberTypeAndReferenceTypeAndRoleId() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndRoleId("user1", MembershipMemberType.USER, MembershipReferenceType.API, "API_OWNER");
        assertNotNull("result must not be null", memberships);
        assertTrue(!memberships.isEmpty());
        Membership membership = memberships.iterator().next();
        assertEquals("API_OWNER", membership.getRoleId());
        assertEquals("api1", membership.getReferenceId());
        assertEquals("user1", membership.getMemberId());
    }

    @Test
    public void shouldFindByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId("user1", MembershipMemberType.USER, MembershipReferenceType.API, "api1");
        assertNotNull("result must not be null", memberships);
        assertTrue(!memberships.isEmpty());
        Membership membership = memberships.iterator().next();
        assertEquals("API_OWNER", membership.getRoleId());
        assertEquals("api1", membership.getReferenceId());
        assertEquals("user1", membership.getMemberId());
        assertEquals("API_OWNER", membership.getRoleId());
    }

    @Test
    public void shouldFindByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId("user1", MembershipMemberType.USER, MembershipReferenceType.API, "api1", "API_OWNER");
        assertNotNull("result must not be null", memberships);
        assertTrue(!memberships.isEmpty());
        Membership membership = memberships.iterator().next();
        assertEquals("API_OWNER", membership.getRoleId());
        assertEquals("api1", membership.getReferenceId());
        assertEquals("user1", membership.getMemberId());
        assertEquals("API_OWNER", membership.getRoleId());
    }

    @Test
    public void shouldFindByRoleId() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByRoleId("APPLICATION_USER");
        assertNotNull("result must not be null", memberships);
        assertTrue(!memberships.isEmpty());
        assertEquals("size", 2, memberships.size());
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        membershipRepository.delete("app1_userToDelete");

        Optional<Membership> optional = membershipRepository.findById("app1_userToDelete");
        assertFalse("There is no membership", optional.isPresent());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        Membership membership = new Membership("app1_userToDelete", "userToUpdate", MembershipMemberType.USER, "app1", MembershipReferenceType.APPLICATION, "APPLICATION_USER");
        membership.setId("app1_userToUpdate");
        membership.setCreatedAt(new Date(1000000000000L));

        Membership update = membershipRepository.update(membership);

        assertNotNull(update);
        assertTrue(compareDate(new Date(1000000000000L), update.getCreatedAt()));
    }

    @Test
    public void shouldFindByIds() throws TechnicalException {
        Set<Membership> memberships = membershipRepository
                .findByIds(new HashSet<>(Arrays.asList("api1_user_findByIds", "api2_user_findByIds", "unknown")));

        assertNotNull(memberships);
        assertFalse(memberships.isEmpty());
        assertEquals(2, memberships.size());
        assertTrue(memberships.
                stream().
                map(Membership::getId).
                collect(Collectors.toList()).
                containsAll(Arrays.asList("api1_user_findByIds", "api2_user_findByIds")));
    }

    @Test
    public void shouldFindByMemberIdAndMemberType() throws TechnicalException {
        Set<Membership> memberships = membershipRepository.findByMemberIdAndMemberType("user_findByIds", MembershipMemberType.USER);

        assertNotNull(memberships);
        assertFalse(memberships.isEmpty());
        assertEquals(2, memberships.size());
        assertTrue(memberships.
                stream().
                map(Membership::getId).
                collect(Collectors.toList()).
                containsAll(Arrays.asList("api1_user_findByIds", "api2_user_findByIds")));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownMembership() throws Exception {
        Membership unknownMembership = new Membership();
        unknownMembership.setId("unknown");
        unknownMembership.setMemberId("unknown");
        unknownMembership.setReferenceId("unknown");
        unknownMembership.setReferenceType(MembershipReferenceType.ENVIRONMENT);
        membershipRepository.update(unknownMembership);
        fail("An unknown membership should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        membershipRepository.update(null);
        fail("A null membership should not be updated");
    }

    @Test
    public void shouldDeleteMembers() throws TechnicalException {

        int m1 = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId("user_deleteRef_1", MembershipMemberType.USER,  API, "api_deleteRef").size();
        int m2 = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId("user_deleteRef_2", MembershipMemberType.USER, API, "api_deleteRef").size();

        assertEquals("m1 exists", 1, m1);
        assertEquals("m2 exists", 1, m2);

        membershipRepository.deleteMembers(API, "api_deleteRef");

        m1 = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId("user_deleteRef_1", MembershipMemberType.USER,  API, "api_deleteRef").size();
        m2 = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId("user_deleteRef_2", MembershipMemberType.USER, API, "api_deleteRef").size();

        assertEquals("m1 doesn't exist", 0, m1);
        assertEquals("m2 doesn't exist", 0, m2);
    }
}
