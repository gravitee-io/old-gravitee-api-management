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
package io.gravitee.gateway.security.core;

import io.gravitee.gateway.api.ExecutionContext;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface AuthenticationHandler {

    /**
     * The provider name.
     *
     * @return The provider name.
     */
    String name();

    /**
     * Prevalence order between each authentication system.
     *
     * @return The order value.
     */
    int order();

    /**
     * Check that the incoming HTTP request can be handle by the underlying authentication system.
     *
     * @param context context data upon which incoming HTTP request can be handled.
     * @return Flag indicating that the current context can be handled by the authentication system.
     */
    boolean canHandle(AuthenticationContext context);

    /**
     * Policies which will be run for each request after authentication method selection
     * The "Security policy chain" may be composed of
     * * {@link PluginAuthenticationPolicy}: a policy based on a plugin
     * * {@link HookAuthenticationPolicy}: a policy based on a class defining the behavior.
     *
     * @return A chain of {@link AuthenticationPolicy}
     */
    List<AuthenticationPolicy> handle(ExecutionContext executionContext);
}
