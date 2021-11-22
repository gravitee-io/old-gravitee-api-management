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
package io.gravitee.gateway.policy.impl;

import io.gravitee.policy.api.PolicyConfiguration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CachedPolicyConfigurationFactory extends PolicyConfigurationFactoryImpl {

    private final ConcurrentMap<Integer, PolicyConfiguration> cachedPolicyConfiguration =
            new ConcurrentHashMap<>();

    @Override
    public <T extends PolicyConfiguration> T create(Class<T> policyConfigurationClass, String configuration) {
        if (policyConfigurationClass == null || configuration == null) {
            return null;
        }

        Integer hash = configuration.hashCode();
        PolicyConfiguration config = cachedPolicyConfiguration.get(hash);
        if (config == null) {
            config = super.create(policyConfigurationClass, configuration);
            if (config != null) {
                cachedPolicyConfiguration.put(hash, config);
            }
        }

        return (T) config;
    }
}
