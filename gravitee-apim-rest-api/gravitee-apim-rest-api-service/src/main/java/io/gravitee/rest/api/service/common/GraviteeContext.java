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
package io.gravitee.rest.api.service.common;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeContext {

    private static final String DEFAULT_ENVIRONMENT = "DEFAULT";
    private static final String DEFAULT_ORGANIZATION = "DEFAULT";

    private static final String CURRENT_ENVIRONMENT_CONTEXT_KEY = "currentEnvironment";
    private static final String CURRENT_ORGANIZATION_CONTEXT_KEY = "currentOrganization";

    private static final ThreadLocal<Map<String, Object>> contextThread = ThreadLocal.withInitial(
        () -> {
            Map<String, Object> propertiesMap = new HashMap<>();
            propertiesMap.put(CURRENT_ENVIRONMENT_CONTEXT_KEY, DEFAULT_ENVIRONMENT);
            propertiesMap.put(CURRENT_ORGANIZATION_CONTEXT_KEY, DEFAULT_ORGANIZATION);
            return propertiesMap;
        }
    );

    public static void cleanContext() {
        contextThread.remove();
    }

    public static String getCurrentEnvironment() {
        return (String) contextThread.get().get(CURRENT_ENVIRONMENT_CONTEXT_KEY);
    }

    public static void setCurrentEnvironment(String currentEnvironment) {
        contextThread.get().put(CURRENT_ENVIRONMENT_CONTEXT_KEY, currentEnvironment);
    }

    public static String getDefaultEnvironment() {
        return DEFAULT_ENVIRONMENT;
    }

    public static String getCurrentOrganization() {
        return (String) contextThread.get().get(CURRENT_ORGANIZATION_CONTEXT_KEY);
    }

    public static void setCurrentOrganization(String currentOrganization) {
        contextThread.get().put(CURRENT_ORGANIZATION_CONTEXT_KEY, currentOrganization);
    }

    public static String getDefaultOrganization() {
        return DEFAULT_ORGANIZATION;
    }
}
