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
package io.gravitee.gateway.reactor.handler;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.Request;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VirtualHostTest {

    @Mock
    private Request request;

    @Mock
    private HttpHeaders httpHeaders;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(request.headers()).thenReturn(httpHeaders);
    }

    @Test
    public void shouldHaveWeightEqualsTo1() {
        VirtualHost vHost = new VirtualHost("/");

        Assert.assertEquals(1, vHost.priority());
    }

    @Test
    public void shouldHaveWeightEqualsTo5() {
        VirtualHost vHost = new VirtualHost("/path/to/my/api");

        Assert.assertEquals(5, vHost.priority());
    }

    @Test
    public void shouldHaveWeightEqualsTo5_duplicatedPathSeparator() {
        VirtualHost vHost = new VirtualHost("/path//to///my/api");

        Assert.assertEquals(5, vHost.priority());
    }

    @Test
    public void shouldHaveWeightEqualsTo1001() {
        VirtualHost vHost = new VirtualHost("api.gravitee.io", "/");

        Assert.assertEquals(1001, vHost.priority());
    }

    @Test
    public void shouldAccept_contextPath() {
        VirtualHost vHost = new VirtualHost("/");

        when(request.path()).thenReturn("/");
        boolean accept = vHost.accept(request);

        Assert.assertTrue(accept);
    }

    @Test
    public void shouldNotAccept_contextPath() {
        VirtualHost vHost = new VirtualHost("/products");

        when(request.path()).thenReturn("/");
        boolean accept = vHost.accept(request);

        Assert.assertFalse(accept);
    }

    @Test
    public void shouldAccept_contextPath_subPath() {
        VirtualHost vHost = new VirtualHost("/products");

        when(request.path()).thenReturn("/products");
        boolean accept = vHost.accept(request);

        Assert.assertTrue(accept);
    }

    @Test
    public void shouldAccept_contextPath_trailingSlash() {
        VirtualHost vHost = new VirtualHost("/products");

        when(request.path()).thenReturn("/products/");
        boolean accept = vHost.accept(request);

        Assert.assertTrue(accept);
    }

    @Test
    public void shouldNotAccept_contextPath_subPath() {
        VirtualHost vHost = new VirtualHost("/products");

        when(request.path()).thenReturn("/products2");
        boolean accept = vHost.accept(request);

        Assert.assertFalse(accept);
    }

    @Test
    public void shouldNotAccept_withHost() {
        VirtualHost vHost = new VirtualHost("api.gravitee.io", "/");

        // The request does not contain a HOST header
        when(request.path()).thenReturn("/");
        boolean accept = vHost.accept(request);

        Assert.assertFalse(accept);
    }

    @Test
    public void shouldAccept_withHost() {
        VirtualHost vHost = new VirtualHost("api.gravitee.io", "/");

        when(request.path()).thenReturn("/");
        when(httpHeaders.getFirst(HttpHeaders.HOST)).thenReturn("api.gravitee.io");
        boolean accept = vHost.accept(request);

        Assert.assertTrue(accept);
    }
}
