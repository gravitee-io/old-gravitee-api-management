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
package io.gravitee.gateway.handlers.api;

import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.support.AbstractApplicationContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiContextHandlerFactoryTest {

    private ApiContextHandlerFactory apiContextHandlerFactory;

    @Mock
    private Api api;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        apiContextHandlerFactory = new ApiContextHandlerFactory();
    }

    @Test
    public void shouldNotCreateContext() {
        when(api.isEnabled()).thenReturn(false);
        ReactorHandler handler = apiContextHandlerFactory.create(api);

        assertNull(handler);
    }

    @Test
    public void shouldCreateContext() {
        apiContextHandlerFactory = spy(apiContextHandlerFactory);

        AbstractApplicationContext ctx = mock(AbstractApplicationContext.class);
        when(api.isEnabled()).thenReturn(true);
        when(ctx.getBean(ApiReactorHandler.class)).thenReturn(mock(ApiReactorHandler.class));
        doReturn(ctx).when(apiContextHandlerFactory).createApplicationContext(api);

        ReactorHandler handler = apiContextHandlerFactory.create(api);

        assertNotNull(handler);
        assertTrue(ApiReactorHandler.class.isAssignableFrom(handler.getClass()));
    }
}
