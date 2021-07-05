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
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.management.model.UserReferenceType;
import io.gravitee.repository.management.model.UserStatus;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class UserRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/user-tests/";
    }

    @Test
    public void createUserTest() throws Exception {
        String username = "createuser1";

        User user = new User();
        user.setId("createuser1");
        user.setReferenceId("DEFAULT");
        user.setReferenceType(UserReferenceType.ENVIRONMENT);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(user.getCreatedAt());
        user.setEmail(String.format("%s@gravitee.io", username));
        user.setStatus(UserStatus.ACTIVE);
        user.setSource("gravitee");
        user.setSourceId("createuser1");
        user.setLoginCount(123);
        User userCreated = userRepository.create(user);

        assertNotNull("User created is null", userCreated);

        Optional<User> optional = userRepository.findBySource("gravitee", "createuser1", "DEFAULT", UserReferenceType.ENVIRONMENT);

        assertTrue("Unable to find saved user", optional.isPresent());
        User userFound = optional.get();

        assertEquals("Invalid saved reference id.", user.getReferenceId(), userFound.getReferenceId());
        assertEquals("Invalid saved reference type.", user.getReferenceType(), userFound.getReferenceType());
        assertEquals("Invalid saved user name.", user.getId(), userFound.getId());
        assertEquals("Invalid saved user mail.", user.getEmail(), userFound.getEmail());
        assertEquals("Invalid saved user status.", user.getStatus(), userFound.getStatus());
        assertEquals("Invalid saved user login count.", user.getLoginCount(), userFound.getLoginCount());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<User> optional = userRepository.findById("id2update");
        assertTrue("userRepository to update not found", optional.isPresent());

        final User user = optional.get();
        user.setSource("sourceUpdated");
        user.setReferenceId("new_DEFAULT");
        user.setReferenceType(UserReferenceType.ENVIRONMENT);
        user.setSourceId("sourceIdUpdated");
        user.setPassword("passwordUpdated");
        user.setEmail("emailUpdated");
        user.setFirstname("firstnameUpdated");
        user.setLastname("lastnameUpdated");
        user.setPicture("pictureUpdated");
        user.setStatus(UserStatus.ARCHIVED);
        user.setCreatedAt(new Date(1439032010883L));
        user.setUpdatedAt(new Date(1439042010883L));
        user.setLastConnectionAt(new Date(1439052010883L));
        user.setLoginCount(123);

        long nbUsersBeforeUpdate = userRepository.search(null,
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
        ).getTotalElements();
        userRepository.update(user);
        long nbUsersAfterUpdate = userRepository.search(null,
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
        ).getTotalElements();

        assertEquals(nbUsersBeforeUpdate, nbUsersAfterUpdate);

        Optional<User> optionalUpdated = userRepository.findById("id2update");
        assertTrue("User to update not found", optionalUpdated.isPresent());

        final User userUpdated = optionalUpdated.get();
        assertEquals("Invalid saved reference id.", "new_DEFAULT", userUpdated.getReferenceId());
        assertEquals("Invalid saved reference type.", UserReferenceType.ENVIRONMENT, userUpdated.getReferenceType());
        assertEquals("Invalid saved source", "sourceUpdated", userUpdated.getSource());
        assertEquals("Invalid saved sourceId", "sourceIdUpdated", userUpdated.getSourceId());
        assertEquals("Invalid saved password", "passwordUpdated", userUpdated.getPassword());
        assertEquals("Invalid saved email", "emailUpdated", userUpdated.getEmail());
        assertEquals("Invalid saved firstname", "firstnameUpdated", userUpdated.getFirstname());
        assertEquals("Invalid saved lastname", "lastnameUpdated", userUpdated.getLastname());
        assertEquals("Invalid saved picture", "pictureUpdated", userUpdated.getPicture());
        assertTrue("Invalid saved createDate", compareDate(new Date(1439032010883L), userUpdated.getCreatedAt()));
        assertTrue("Invalid saved updateDate", compareDate(new Date(1439042010883L), userUpdated.getUpdatedAt()));
        assertTrue("Invalid saved lastConnection", compareDate(new Date(1439052010883L), userUpdated.getLastConnectionAt()));
        assertEquals("Invalid status", UserStatus.ARCHIVED, userUpdated.getStatus());
        assertEquals("Invalid saved user login count.", 123, userUpdated.getLoginCount());
    }

    @Test
    public void shouldSearchAllWithEnvironment() throws Exception {
        List<User> users = userRepository.search(new UserCriteria.Builder().referenceId("DEFAULT").referenceType(UserReferenceType.ENVIRONMENT).build(),
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
        ).getContent();

        assertNotNull(users);
        assertEquals("Invalid user numbers in search", 1, users.size());
        assertEquals("user0", users.get(0).getId());
    }
    
    @Test
    public void shouldSearchAllWithNullCriteria() throws Exception {
        List<User> users = userRepository.search(null,
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
        ).getContent();

        assertNotNull(users);
        assertEquals("Invalid user numbers in search", 9, users.size());
        assertEquals("user0", users.get(0).getId());
        assertEquals("user1", users.get(1).getId());
        assertEquals("user3", users.get(2).getId());
        assertEquals("user5", users.get(3).getId());
        assertEquals("user2", users.get(4).getId());
        assertEquals("user4", users.get(5).getId());
        assertEquals("id2update", users.get(6).getId());
        assertEquals("idSpecialChar", users.get(7).getId());
        assertEquals("user2delete", users.get(8).getId());
    }

    @Test
    public void shouldSearchAllWithEmptyCriteria() throws Exception {
        List<User> users = userRepository.search(new UserCriteria.Builder().build(),
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
        ).getContent();

    }

    @Test
    public void shouldSearchArchivedUsers() throws Exception {
        List<User> users = userRepository.search(new UserCriteria.Builder().statuses(UserStatus.ARCHIVED).build(),
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
        ).getContent();

        Assert.assertNotNull(users);
        assertEquals("Invalid user numbers in find archived", 1, users.size());
    }

    @Test
    public void findUserBySourceCaseInsensitive() throws Exception {
        Optional<User> user1 = userRepository.findBySource("gravitee", "user1", "DEV", UserReferenceType.ENVIRONMENT);
        Optional<User> user1Upper = userRepository.findBySource("gravitee", "USER1", "DEV", UserReferenceType.ENVIRONMENT);
        assertTrue(user1.isPresent());
        assertTrue(user1Upper.isPresent());
        assertEquals(user1.get().getId(), user1Upper.get().getId());
    }

    @Test
    public void findUserByEmail() throws Exception {
        Optional<User> user1 = userRepository.findByEmail("user0@gravitee.io", "DEFAULT", UserReferenceType.ENVIRONMENT);
        Optional<User> user1Upper = userRepository.findByEmail("usER0@gravitee.io", "DEFAULT", UserReferenceType.ENVIRONMENT);
        assertTrue(user1.isPresent());
        assertTrue(user1Upper.isPresent());
        assertEquals(user1.get().getId(), user1Upper.get().getId());
    }

    @Test
    public void findUserBySourceSpecialCharacters() throws Exception {
        Optional<User> user = userRepository.findBySource("sourceSpecialChar", "sourceIdSpecialChar+test@me", "DEV", UserReferenceType.ENVIRONMENT);
        assertTrue(user.isPresent());
    }

    @Test
    public void shouldSearchUsersWithNoStatus() throws Exception {
        List<User> users = userRepository.search(new UserCriteria.Builder().noStatus().build(),
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
        ).getContent();

        Assert.assertNotNull(users);
        assertEquals("Invalid user numbers in find no status", 1, users.size());
    }

    @Test
    public void shouldSearchActiveUsers() throws Exception {
        List<User> users = userRepository.search(new UserCriteria.Builder().statuses(UserStatus.ACTIVE).build(),
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
        ).getContent();

        Assert.assertNotNull(users);
        assertEquals("Invalid user numbers in find active", 7, users.size());
    }

    @Test
    public void findUserBySourceTest() throws Exception {
        Optional<User> user = userRepository.findBySource("gravitee", "user1", "DEV", UserReferenceType.ENVIRONMENT);
        Assert.assertTrue(user.isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownUser() throws Exception {
        User unknownUser = new User();
        unknownUser.setId("unknown");
        userRepository.update(unknownUser);
        fail("An unknown user should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        userRepository.update(null);
        fail("A null user should not be updated");
    }

    @Test
    public void shouldFindById() throws Exception {
        final Optional<User> optionalUser = userRepository.findById("user1");

        assertTrue(optionalUser.isPresent());
        assertEquals("User not found by its id", "user1", optionalUser.get().getId());
    }

    @Test
    public void shouldFindByIds() throws Exception {
        final Set<User> users = userRepository.findByIds(asList("user1", "user5"));

        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.stream().map(User::getId).collect(toList()).containsAll(asList("user1", "user5")));
    }

    @Test
    public void shouldDelete() throws Exception {
        assertTrue("user2delete exists", userRepository.findById("user2delete").isPresent());
        userRepository.delete("user2delete");
        assertFalse("user2delete not exists", userRepository.findById("user2delete").isPresent());
    }
}
