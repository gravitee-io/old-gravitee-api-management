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
package io.gravitee.gateway.handlers.api.processor.error.templates;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.ResponseTemplates;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.core.processor.ProcessorFailure;
import io.gravitee.reporter.api.http.Metrics;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseTemplateBasedFailureProcessorTest {

    private ResponseTemplateBasedFailureProcessor processor;

    @Mock
    private ExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private HttpHeaders requestHeaders;

    @Mock
    private HttpHeaders responseHeaders;

    @Mock
    private Handler<ExecutionContext> next;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(request.metrics()).thenReturn(Metrics.on(System.currentTimeMillis()).build());
        when(request.headers()).thenReturn(requestHeaders);
        when(response.headers()).thenReturn(responseHeaders);
        when(context.request()).thenReturn(request);
        when(context.response()).thenReturn(response);
    }

    @Test
    public void shouldFallbackToDefaultHandler_noProcessorFailureKey() {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(ResponseTemplateBasedFailureProcessor.WILDCARD_CONTENT_TYPE, template);

        ResponseTemplates responseTemplates = new ResponseTemplates();
        responseTemplates.setTemplates(mapTemplates);

        Map<String, ResponseTemplates> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", responseTemplates);

        processor = new ResponseTemplateBasedFailureProcessor(templates);
        processor.handler(next);

        // Set failure
        DummyProcessorFailure failure = new DummyProcessorFailure();
        failure.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        when(context.getAttribute(ExecutionContext.ATTR_PREFIX + "failure")).thenReturn(failure);

        processor.handle(context);

        verify(response, times(1)).status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
    }

    @Test
    public void shouldFallbackToDefaultHandler_withProcessorFailureKey() {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(ResponseTemplateBasedFailureProcessor.WILDCARD_CONTENT_TYPE, template);

        ResponseTemplates responseTemplates = new ResponseTemplates();
        responseTemplates.setTemplates(mapTemplates);

        Map<String, ResponseTemplates> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", responseTemplates);

        processor = new ResponseTemplateBasedFailureProcessor(templates);
        processor.handler(next);

        // Set failure
        DummyProcessorFailure failure = new DummyProcessorFailure();
        failure.setKey("POLICY_ERROR_KEY");
        failure.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        when(context.getAttribute(ExecutionContext.ATTR_PREFIX + "failure")).thenReturn(failure);

        processor.handle(context);

        verify(response, times(1)).status(HttpStatusCode.BAD_REQUEST_400);
    }

    @Test
    public void shouldFallbackToDefaultHandler_withProcessorFailureKey_fallbackToDefault() {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(ResponseTemplateBasedFailureProcessor.WILDCARD_CONTENT_TYPE, template);

        ResponseTemplates responseTemplates = new ResponseTemplates();
        responseTemplates.setTemplates(mapTemplates);

        Map<String, ResponseTemplates> templates = new HashMap<>();
        templates.put("DEFAULT", responseTemplates);

        processor = new ResponseTemplateBasedFailureProcessor(templates);
        processor.handler(next);

        // Set failure
        DummyProcessorFailure failure = new DummyProcessorFailure();
        failure.setKey("POLICY_ERROR_KEY");
        failure.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);

        when(context.getAttribute(ExecutionContext.ATTR_PREFIX + "failure")).thenReturn(failure);

        processor.handle(context);

        verify(response, times(1)).status(HttpStatusCode.BAD_REQUEST_400);
    }

    @Test
    public void shouldFallbackToDefaultHandler_withProcessorFailureKey_unmappedAcceptHeader() {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(MediaType.APPLICATION_JSON, template);

        ResponseTemplates responseTemplates = new ResponseTemplates();
        responseTemplates.setTemplates(mapTemplates);

        Map<String, ResponseTemplates> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", responseTemplates);

        processor = new ResponseTemplateBasedFailureProcessor(templates);
        processor.handler(next);

        // Set failure
        DummyProcessorFailure failure = new DummyProcessorFailure();
        failure.setKey("POLICY_ERROR_KEY");
        failure.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);

        when(requestHeaders.getAccept()).thenReturn(Collections.singletonList(MediaType.MEDIA_APPLICATION_XML));
        when(context.getAttribute(ExecutionContext.ATTR_PREFIX + "failure")).thenReturn(failure);

        processor.handle(context);

        verify(response, times(1)).status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
    }

    @Test
    public void shouldFallbackToDefaultHandler_withProcessorFailureKey_unmappedAcceptHeader_fallbackToWildcard() {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        ResponseTemplate wildcardTemplate = new ResponseTemplate();
        wildcardTemplate.setStatusCode(HttpStatusCode.BAD_GATEWAY_502);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(MediaType.APPLICATION_JSON, template);
        mapTemplates.put(ResponseTemplateBasedFailureProcessor.WILDCARD_CONTENT_TYPE, wildcardTemplate);

        ResponseTemplates responseTemplates = new ResponseTemplates();
        responseTemplates.setTemplates(mapTemplates);

        Map<String, ResponseTemplates> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", responseTemplates);

        processor = new ResponseTemplateBasedFailureProcessor(templates);
        processor.handler(next);

        // Set failure
        DummyProcessorFailure failure = new DummyProcessorFailure();
        failure.setKey("POLICY_ERROR_KEY");
        failure.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);

        when(requestHeaders.getAccept()).thenReturn(Collections.singletonList(MediaType.MEDIA_APPLICATION_XML));
        when(context.getAttribute(ExecutionContext.ATTR_PREFIX + "failure")).thenReturn(failure);

        processor.handle(context);

        verify(response, times(1)).status(HttpStatusCode.BAD_GATEWAY_502);
    }

    @Test
    public void shouldFallbackToDefaultHandler_withProcessorFailureKey_acceptHeader() {
        ResponseTemplate jsonTemplate = new ResponseTemplate();
        jsonTemplate.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(MediaType.APPLICATION_JSON, jsonTemplate);

        ResponseTemplates responseTemplates = new ResponseTemplates();
        responseTemplates.setTemplates(mapTemplates);

        Map<String, ResponseTemplates> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", responseTemplates);

        processor = new ResponseTemplateBasedFailureProcessor(templates);
        processor.handler(next);

        // Set failure
        DummyProcessorFailure failure = new DummyProcessorFailure();
        failure.setKey("POLICY_ERROR_KEY");
        failure.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);

        when(requestHeaders.getAccept()).thenReturn(Collections.singletonList(MediaType.MEDIA_APPLICATION_JSON));
        when(context.getAttribute(ExecutionContext.ATTR_PREFIX + "failure")).thenReturn(failure);

        processor.handle(context);

        verify(response, times(1)).status(HttpStatusCode.BAD_REQUEST_400);
    }

    @Test
    public void shouldFallbackToDefaultHandler_withProcessorFailureKey_acceptHeader_fallbackToNextMediaType() {
        ResponseTemplate jsonTemplate = new ResponseTemplate();
        jsonTemplate.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(MediaType.APPLICATION_JSON, jsonTemplate);

        ResponseTemplates responseTemplates = new ResponseTemplates();
        responseTemplates.setTemplates(mapTemplates);

        Map<String, ResponseTemplates> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", responseTemplates);

        processor = new ResponseTemplateBasedFailureProcessor(templates);
        processor.handler(next);

        // Set failure
        DummyProcessorFailure failure = new DummyProcessorFailure();
        failure.setKey("POLICY_ERROR_KEY");
        failure.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "text/html, application/json, */*;q=0.8, application/xml;q=0.9");
        when(request.headers()).thenReturn(headers);
        when(context.getAttribute(ExecutionContext.ATTR_PREFIX + "failure")).thenReturn(failure);

        processor.handle(context);

        verify(response, times(1)).status(HttpStatusCode.BAD_REQUEST_400);
    }

    static class DummyProcessorFailure implements ProcessorFailure {

        private int statusCode;
        private String message;
        private String key;
        private Map<String, Object> parameters;
        private String contentType;

        @Override
        public int statusCode() {
            return this.statusCode;
        }

        @Override
        public String message() {
            return this.message;
        }

        @Override
        public String key() {
            return this.key;
        }

        @Override
        public Map<String, Object> parameters() {
            return this.parameters;
        }

        @Override
        public String contentType() {
            return this.contentType;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
    }
}
