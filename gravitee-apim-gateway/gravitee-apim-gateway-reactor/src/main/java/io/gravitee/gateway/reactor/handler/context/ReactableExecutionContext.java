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
package io.gravitee.gateway.reactor.handler.context;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.el.EvaluableRequest;
import io.gravitee.gateway.api.el.EvaluableResponse;
import io.gravitee.tracing.api.Tracer;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import org.springframework.context.ApplicationContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReactableExecutionContext implements MutableExecutionContext {

    private static final String TEMPLATE_ATTRIBUTE_REQUEST = "request";
    private static final String TEMPLATE_ATTRIBUTE_RESPONSE = "response";
    private static final String TEMPLATE_ATTRIBUTE_CONTEXT = "context";

    private final ApplicationContext applicationContext;

    private Collection<TemplateVariableProvider> providers;

    private TemplateEngine templateEngine;

    private final Tracer tracer;

    private final MutableExecutionContext context;

    ReactableExecutionContext(final MutableExecutionContext context, final Tracer tracer, ApplicationContext applicationContext) {
        this.context = context;
        this.tracer = tracer;
        this.applicationContext = applicationContext;

        setAttribute(ExecutionContext.ATTR_CONTEXT_PATH, context.request().contextPath());
    }

    @Override
    public ReactableExecutionContext request(Request request) {
        context.request(request);
        return this;
    }

    @Override
    public ReactableExecutionContext response(Response response) {
        context.response(response);
        return this;
    }

    @Override
    public Request request() {
        return context.request();
    }

    @Override
    public Response response() {
        return context.response();
    }

    @Override
    public void setAttribute(String name, Object value) {
        context.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        context.removeAttribute(name);
    }

    @Override
    public Object getAttribute(String name) {
        return context.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return context.getAttributeNames();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return context.getAttributes();
    }

    @Override
    public <T> T getComponent(Class<T> componentClass) {
        return applicationContext.getBean(componentClass);
    }

    @Override
    public TemplateEngine getTemplateEngine() {
        if (templateEngine == null) {
            templateEngine = TemplateEngine.templateEngine();

            TemplateContext templateContext = templateEngine.getTemplateContext();
            templateContext.setVariable(TEMPLATE_ATTRIBUTE_REQUEST, new EvaluableRequest(request()));
            templateContext.setVariable(TEMPLATE_ATTRIBUTE_RESPONSE, new EvaluableResponse(response()));
            templateContext.setVariable(TEMPLATE_ATTRIBUTE_CONTEXT, new EvaluableExecutionContext(this));

            if (providers != null) {
                providers.forEach(templateVariableProvider -> templateVariableProvider.provide(templateContext));
            }
        }

        return templateEngine;
    }

    @Override
    public Tracer getTracer() {
        return tracer;
    }

    void setProviders(Collection<TemplateVariableProvider> providers) {
        this.providers = providers;
    }
}
