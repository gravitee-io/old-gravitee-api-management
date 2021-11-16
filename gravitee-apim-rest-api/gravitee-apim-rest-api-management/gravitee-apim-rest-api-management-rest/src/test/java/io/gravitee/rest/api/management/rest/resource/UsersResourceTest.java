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
package io.gravitee.rest.api.management.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.NewPreRegisterUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UsersResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "users/";
    }

    @Before
    public void setUp() {
        reset(userService);
    }

    @Test
    public void shouldCreateServiceAccountWithEmail() {
        NewPreRegisterUserEntity newPreRegisterUserEntity = new NewPreRegisterUserEntity();
        newPreRegisterUserEntity.setEmail("mail@fake.fake");
        newPreRegisterUserEntity.setService(true);

        when(userService.create(any())).thenReturn(new UserEntity());

        final Response response = orgTarget().request().post(Entity.json(newPreRegisterUserEntity));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldCreateServiceAccountWithoutEmail() {
        NewPreRegisterUserEntity newPreRegisterUserEntity = new NewPreRegisterUserEntity();
        newPreRegisterUserEntity.setService(true);

        when(userService.create(any())).thenReturn(new UserEntity());

        final Response response = orgTarget().request().post(Entity.json(newPreRegisterUserEntity));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldCreateUser() {
        NewPreRegisterUserEntity newPreRegisterUserEntity = new NewPreRegisterUserEntity();
        newPreRegisterUserEntity.setEmail("mail@fake.fake");

        when(userService.create(any())).thenReturn(new UserEntity());

        final Response response = orgTarget().request().post(Entity.json(newPreRegisterUserEntity));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldNotCreateUserWhenNoEmailProvided() {
        NewPreRegisterUserEntity newPreRegisterUserEntity = new NewPreRegisterUserEntity();

        final Response response = orgTarget().request().post(Entity.json(newPreRegisterUserEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }
}
