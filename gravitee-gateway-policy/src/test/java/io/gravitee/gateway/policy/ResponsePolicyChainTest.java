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
package io.gravitee.gateway.policy;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.BufferedReadWriteStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.policy.impl.PolicyChain;
import io.gravitee.gateway.policy.impl.ResponsePolicyChain;
import io.gravitee.reporter.api.http.Metrics;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponsePolicyChainTest {

    @Spy
    private Policy policy = new SuccessPolicy();

    @Spy
    private Policy policy2 = new SuccessPolicy();

    @Spy
    private Policy policy3 = new FailurePolicy();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test(expected = NullPointerException.class)
    public void buildPolicyChain_withNullPolicies() {
        ResponsePolicyChain.create(null, mock(ExecutionContext.class));
    }

    @Test
    public void buildPolicyChain_withEmptyPolicies() {
        io.gravitee.policy.api.PolicyChain chain = ResponsePolicyChain.create(new ArrayList<>(), mock(ExecutionContext.class));

        Assert.assertNotNull(chain);
    }

    @Test
    public void doNext_emptyPolicies() throws Exception {
        PolicyChain chain = ResponsePolicyChain.create(Collections.emptyList(), mock(ExecutionContext.class));
        chain.handler(result -> {});
        chain.doNext(null, null);

        verify(policy, never()).onRequest();
        verify(policy, never()).onResponse();
    }

    @Test
    public void doNext_singlePolicy() throws Exception {
        PolicyChain chain = ResponsePolicyChain.create(
                Collections.singletonList(policy), mock(ExecutionContext.class));
        chain.handler(result -> {});
        chain.doNext(null, null);

        verify(policy, never()).onRequest(any());
        verify(policy, atLeastOnce()).onResponse(any());
    }

    @Test
    public void doNext_multiplePolicy() throws Exception {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        PolicyChain chain = ResponsePolicyChain.create(
                Arrays.asList(policy, policy2), executionContext);
        chain.handler(result -> {});

        chain.doNext(null, null);

        verify(policy, atLeastOnce()).onResponse(chain, null, null, executionContext);
        verify(policy2, atLeastOnce()).onResponse(chain, null, null, executionContext);
    }

    @Test
    public void doNext_multiplePolicyOrder() throws Exception {
        PolicyChain chain = ResponsePolicyChain.create(
                Arrays.asList(policy, policy2), mock(ExecutionContext.class));
        chain.handler(result -> {});

        InOrder inOrder = inOrder(policy, policy2);

        chain.doNext(null, null);

        inOrder.verify(policy2).onResponse(any());
        inOrder.verify(policy).onResponse(any());
    }

    @Test
    public void doNext_multiplePolicy_throwError() throws Exception {
        Request request = mock(Request.class);
        Metrics metrics = Metrics.on(System.currentTimeMillis()).build();
        when(request.metrics()).thenReturn(metrics);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.request()).thenReturn(request);

        PolicyChain chain = ResponsePolicyChain.create(
                Arrays.asList(policy2, policy3), executionContext);
        chain.handler(result -> {});
        chain.doNext(request, null);

        verify(request, atLeastOnce()).metrics();
        verify(policy3, atLeastOnce()).onResponse(chain, request, null, executionContext);
        verify(policy2, never()).onResponse(chain, request, null, executionContext);
    }

    @Test
    public void doNext_streamablePolicy() throws Exception {
        StreamablePolicy policy4 = spy(new StreamablePolicy());

        ExecutionContext executionContext = mock(ExecutionContext.class);

        ReadWriteStream stream = spy(new BufferedReadWriteStream());
        when(policy4.onResponseContent(
                nullable(Request.class), nullable(Response.class), any(io.gravitee.policy.api.PolicyChain.class), eq(executionContext)
        )).thenReturn(stream);

        PolicyChain chain = ResponsePolicyChain.create(
                Collections.singletonList(policy4), executionContext);
        chain.handler(result -> {});
        chain.doNext(null, null);

        verify(stream, atLeastOnce()).bodyHandler(any(Handler.class));
        verify(stream, atLeastOnce()).endHandler(any(Handler.class));
        verify(policy4, atLeastOnce()).onResponse(chain, null, null, executionContext);
    }

    @Test
    public void doNext_streamablePolicies() throws Exception {
        StreamablePolicy policy4 = spy(new StreamablePolicy());
        StreamablePolicy policy5 = spy(new StreamablePolicy());

        ExecutionContext executionContext = mock(ExecutionContext.class);

        ReadWriteStream streamPolicy4 = spy(new BufferedReadWriteStream());
        when(policy4.onResponseContent(
                nullable(Request.class), nullable(Response.class), any(io.gravitee.policy.api.PolicyChain.class), eq(executionContext)
        )).thenReturn(streamPolicy4);

        ReadWriteStream streamPolicy5 = spy(new BufferedReadWriteStream());
        when(policy5.onResponseContent(
                nullable(Request.class), nullable(Response.class), any(io.gravitee.policy.api.PolicyChain.class), eq(executionContext)
        )).thenReturn(streamPolicy5);

        InOrder inOrder = inOrder(streamPolicy4, streamPolicy5);

        PolicyChain chain = ResponsePolicyChain.create(
                Arrays.asList(policy4, policy5), executionContext);
        chain.handler(result -> {});
        chain.doNext(null, null);

        inOrder.verify(streamPolicy4, atLeastOnce()).bodyHandler(any(Handler.class));
        inOrder.verify(streamPolicy4, atLeastOnce()).endHandler(any(Handler.class));

        inOrder.verify(streamPolicy5, atLeastOnce()).bodyHandler(any(Handler.class));
        inOrder.verify(streamPolicy5, atLeastOnce()).endHandler(any(Handler.class));

        verify(policy4, atLeastOnce()).onResponse(chain, null, null, executionContext);
    }

    @Test
    public void doNext_streamablePolicies_streaming() throws Exception {
        StreamablePolicy policy4 = spy(new StreamablePolicy());
        StreamablePolicy policy5 = spy(new StreamablePolicy());

        ExecutionContext executionContext = mock(ExecutionContext.class);

        ReadWriteStream streamPolicy4 = spy(new BufferedReadWriteStream());
        when(policy4.onResponseContent(
                nullable(Request.class), nullable(Response.class), any(io.gravitee.policy.api.PolicyChain.class), eq(executionContext)
        )).thenReturn(streamPolicy4);

        ReadWriteStream streamPolicy5 = spy(new BufferedReadWriteStream());
        when(policy5.onResponseContent(
                nullable(Request.class), nullable(Response.class), any(io.gravitee.policy.api.PolicyChain.class), eq(executionContext)
        )).thenReturn(streamPolicy5);

        InOrder inOrder = inOrder(streamPolicy4, streamPolicy5);

        PolicyChain chain = ResponsePolicyChain.create(
                Arrays.asList(policy4, policy5), executionContext);
        chain.handler(result -> {});
        chain.bodyHandler(mock(Handler.class));
        chain.endHandler(mock(Handler.class));
        chain.doNext(null, null);

        chain.write(Buffer.buffer("TEST"));
        chain.write(Buffer.buffer("TEST"));
        chain.end();

        inOrder.verify(streamPolicy4, atLeastOnce()).bodyHandler(any(Handler.class));
        inOrder.verify(streamPolicy4, atLeastOnce()).endHandler(any(Handler.class));

        inOrder.verify(streamPolicy5, atLeastOnce()).bodyHandler(any(Handler.class));
        inOrder.verify(streamPolicy5, atLeastOnce()).endHandler(any(Handler.class));

        verify(policy4, atLeastOnce()).onResponse(chain, null, null, executionContext);
    }
}
