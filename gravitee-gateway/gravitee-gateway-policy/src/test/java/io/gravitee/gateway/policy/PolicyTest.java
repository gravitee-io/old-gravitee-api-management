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

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.policy.impl.PolicyFactoryImpl;
import io.gravitee.gateway.policy.impl.PolicyImpl;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.reflections.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyTest {

    private PolicyFactory policyFactory = new PolicyFactoryImpl();

    @Before
    public void setUp() {
        policyFactory = spy(policyFactory);
    }

    @Test
    public void onRequest() throws Exception {
        PolicyMetadata policyDefinition = mock(PolicyMetadata.class);
        when(policyDefinition.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);

        DummyPolicy policyInst = Mockito.spy((DummyPolicy) policyFactory.create(policyDefinition, null));
        Method onRequestMethod = resolvePolicyMethod(DummyPolicy.class, OnRequest.class);

        when(policyDefinition.method(OnRequest.class)).thenReturn(onRequestMethod);

        Policy policy = PolicyImpl.target(policyInst).definition(policyDefinition).build();
        policy.onRequest();

        verify(policyInst, atLeastOnce()).onRequest(any(), any(), any());
        verify(policyInst, never()).onResponse(any(), any(), any());
    }

    @Test
    public void onResponse() throws Exception {
        PolicyMetadata policyDefinition = mock(PolicyMetadata.class);
        when(policyDefinition.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);

        DummyPolicy policyInst = Mockito.spy((DummyPolicy) policyFactory.create(policyDefinition, null));
        Method onResponseMethod = resolvePolicyMethod(DummyPolicy.class, OnResponse.class);

        when(policyDefinition.method(OnResponse.class)).thenReturn(onResponseMethod);

        Policy policy = PolicyImpl.target(policyInst).definition(policyDefinition).build();
        policy.onResponse();

        verify(policyInst, never()).onRequest(any(), any(), any());
        verify(policyInst, atLeastOnce()).onResponse(any(), any(), any());
    }

    @Test
    public void onRequest_emptyParameters() throws Exception {
        PolicyMetadata policyDefinition = mock(PolicyMetadata.class);
        when(policyDefinition.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);

        DummyPolicy policyInst = Mockito.spy((DummyPolicy) policyFactory.create(policyDefinition, null));
        Method onRequestMethod = resolvePolicyMethod(DummyPolicy.class, OnRequest.class);

        when(policyDefinition.method(OnRequest.class)).thenReturn(onRequestMethod);

        Policy policy = PolicyImpl.target(policyInst).definition(policyDefinition).build();
        policy.onRequest();

        verify(policyInst, atLeastOnce()).onRequest(any(), any(), any());
    }

    @Test
    public void onResponse_emptyParameters() throws Exception {
        PolicyMetadata policyDefinition = mock(PolicyMetadata.class);
        when(policyDefinition.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);

        DummyPolicy policyInst = Mockito.spy((DummyPolicy) policyFactory.create(policyDefinition, null));
        Method onResponseMethod = resolvePolicyMethod(DummyPolicy.class, OnResponse.class);

        when(policyDefinition.method(OnResponse.class)).thenReturn(onResponseMethod);

        Policy policy = PolicyImpl.target(policyInst).definition(policyDefinition).build();
        policy.onResponse();

        verify(policyInst, atLeastOnce()).onResponse(any(), any(), any());
    }

    @Test
    public void onRequest_mockParameters() throws Exception {
        PolicyMetadata policyDefinition = mock(PolicyMetadata.class);
        when(policyDefinition.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);

        DummyPolicy policyInst = Mockito.spy((DummyPolicy) policyFactory.create(policyDefinition, null));
        Method onRequestMethod = resolvePolicyMethod(DummyPolicy.class, OnRequest.class);

        when(policyDefinition.method(OnRequest.class)).thenReturn(onRequestMethod);

        Policy policy = PolicyImpl.target(policyInst).definition(policyDefinition).build();
        Request mockRequest = mock(Request.class);
        Response mockResponse = mock(Response.class);

        policy.onRequest(mockRequest, mockResponse);

        verify(policyInst, atLeastOnce()).onRequest(nullable(PolicyChain.class), eq(mockRequest), eq(mockResponse));
    }

    @Test
    public void onResponse_mockParameters() throws Exception {
        PolicyMetadata policyDefinition = mock(PolicyMetadata.class);
        when(policyDefinition.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);

        DummyPolicy policyInst = Mockito.spy((DummyPolicy) policyFactory.create(policyDefinition, null));
        Method onResponseMethod = resolvePolicyMethod(DummyPolicy.class, OnResponse.class);

        when(policyDefinition.method(OnResponse.class)).thenReturn(onResponseMethod);

        Policy policy = PolicyImpl.target(policyInst).definition(policyDefinition).build();

        Request mockRequest = mock(Request.class);
        Response mockResponse = mock(Response.class);

        policy.onResponse(mockRequest, mockResponse);

        verify(policyInst, atLeastOnce()).onResponse(eq(mockRequest), eq(mockResponse), nullable(PolicyChain.class));
    }

    private Method resolvePolicyMethod(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        Set<Method> methods = ReflectionUtils.getMethods(
                clazz,
                withModifier(Modifier.PUBLIC),
                withAnnotation(annotationClass));

        if (methods.isEmpty()) {
            return null;
        }

        return methods.iterator().next();
    }
}
