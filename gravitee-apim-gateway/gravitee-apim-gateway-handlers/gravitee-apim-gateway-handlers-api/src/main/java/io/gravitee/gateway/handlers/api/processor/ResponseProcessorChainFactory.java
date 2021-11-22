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
package io.gravitee.gateway.handlers.api.processor;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.gateway.handlers.api.flow.BestMatchPolicyResolver;
import io.gravitee.gateway.handlers.api.flow.SimpleFlowPolicyChainProvider;
import io.gravitee.gateway.handlers.api.flow.SimpleFlowProvider;
import io.gravitee.gateway.handlers.api.flow.api.ApiFlowResolver;
import io.gravitee.gateway.handlers.api.flow.condition.CompositeConditionEvaluator;
import io.gravitee.gateway.handlers.api.flow.condition.ConditionEvaluator;
import io.gravitee.gateway.handlers.api.flow.condition.evaluation.HttpMethodConditionEvaluator;
import io.gravitee.gateway.handlers.api.flow.condition.evaluation.PathBasedConditionEvaluator;
import io.gravitee.gateway.handlers.api.flow.condition.evaluation.el.ExpressionLanguageBasedConditionEvaluator;
import io.gravitee.gateway.handlers.api.flow.plan.PlanFlowPolicyChainProvider;
import io.gravitee.gateway.handlers.api.flow.plan.PlanFlowResolver;
import io.gravitee.gateway.handlers.api.policy.api.ApiPolicyChainProvider;
import io.gravitee.gateway.handlers.api.policy.api.ApiPolicyResolver;
import io.gravitee.gateway.handlers.api.policy.plan.PlanPolicyChainProvider;
import io.gravitee.gateway.handlers.api.policy.plan.PlanPolicyResolver;
import io.gravitee.gateway.handlers.api.processor.cors.CorsSimpleRequestProcessor;
import io.gravitee.gateway.handlers.api.processor.pathmapping.PathMappingProcessor;
import io.gravitee.gateway.policy.StreamType;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseProcessorChainFactory extends ApiProcessorChainFactory {

    @Override
    public void afterPropertiesSet() {
        if (api.getDefinitionVersion() == DefinitionVersion.V1) {
            add(new ApiPolicyChainProvider(StreamType.ON_RESPONSE, new ApiPolicyResolver(), chainFactory));
            add(new PlanPolicyChainProvider(StreamType.ON_RESPONSE, new PlanPolicyResolver(api), chainFactory));
        } else if (api.getDefinitionVersion() == DefinitionVersion.V2) {
            final ConditionEvaluator evaluator = new CompositeConditionEvaluator(
                new HttpMethodConditionEvaluator(),
                new PathBasedConditionEvaluator(),
                new ExpressionLanguageBasedConditionEvaluator()
            );

            if (api.getFlowMode() == null || api.getFlowMode() == FlowMode.DEFAULT) {
                add(
                    new SimpleFlowPolicyChainProvider(
                        new SimpleFlowProvider(StreamType.ON_RESPONSE, new ApiFlowResolver(api, evaluator), chainFactory)
                    )
                );
                add(
                    new PlanFlowPolicyChainProvider(
                        new SimpleFlowProvider(StreamType.ON_RESPONSE, new PlanFlowResolver(api, evaluator), chainFactory)
                    )
                );
            } else {
                add(
                    new SimpleFlowPolicyChainProvider(
                        new SimpleFlowProvider(
                            StreamType.ON_RESPONSE,
                            new BestMatchPolicyResolver(new ApiFlowResolver(api, evaluator)),
                            chainFactory
                        )
                    )
                );
                add(
                    new PlanFlowPolicyChainProvider(
                        new SimpleFlowProvider(
                            StreamType.ON_RESPONSE,
                            new BestMatchPolicyResolver(new PlanFlowResolver(api, evaluator)),
                            chainFactory
                        )
                    )
                );
            }
        }

        if (api.getProxy().getCors() != null && api.getProxy().getCors().isEnabled()) {
            add(() -> new CorsSimpleRequestProcessor(api.getProxy().getCors()));
        }

        if (api.getPathMappings() != null && !api.getPathMappings().isEmpty()) {
            add(() -> new PathMappingProcessor(api.getPathMappings()));
        }
    }
}
