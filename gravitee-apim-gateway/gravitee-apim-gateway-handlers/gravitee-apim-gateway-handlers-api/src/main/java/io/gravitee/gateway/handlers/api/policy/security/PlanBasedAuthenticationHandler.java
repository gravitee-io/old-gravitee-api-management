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
package io.gravitee.gateway.handlers.api.policy.security;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import io.gravitee.gateway.security.core.PluginAuthenticationPolicy;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanBasedAuthenticationHandler implements AuthenticationHandler {

    private static final String CONTEXT_ATTRIBUTE_PLAN_SELECTION_RULE_BASED =
        ExecutionContext.ATTR_PREFIX + ExecutionContext.ATTR_PLAN + ".selection.rule.based";
    protected final AuthenticationHandler handler;
    protected final Plan plan;

    public PlanBasedAuthenticationHandler(final AuthenticationHandler handler, final Plan plan) {
        this.handler = handler;
        this.plan = plan;
    }

    @Override
    public boolean canHandle(AuthenticationContext authenticationContext) {
        return handler.canHandle(authenticationContext);
    }

    @Override
    public String name() {
        return handler.name();
    }

    @Override
    public int order() {
        return handler.order();
    }

    @Override
    public List<AuthenticationPolicy> handle(ExecutionContext executionContext) {
        executionContext.setAttribute(ExecutionContext.ATTR_PLAN, plan.getId());
        executionContext.setAttribute(
            CONTEXT_ATTRIBUTE_PLAN_SELECTION_RULE_BASED,
            plan.getSelectionRule() != null && !plan.getSelectionRule().isEmpty()
        );

        return handler
            .handle(executionContext)
            .stream()
            .map(
                new Function<AuthenticationPolicy, AuthenticationPolicy>() {
                    @Override
                    public AuthenticationPolicy apply(AuthenticationPolicy securityPolicy) {
                        // Override the configuration of the policy with the one provided by the plan
                        if (securityPolicy instanceof PluginAuthenticationPolicy) {
                            return new PluginAuthenticationPolicy() {
                                @Override
                                public String name() {
                                    return ((PluginAuthenticationPolicy) securityPolicy).name();
                                }

                                @Override
                                public String configuration() {
                                    return plan.getSecurityDefinition();
                                }
                            };
                        }

                        return securityPolicy;
                    }
                }
            )
            .collect(Collectors.toList());
    }
}
