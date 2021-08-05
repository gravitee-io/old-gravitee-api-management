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
package io.gravitee.rest.api.service;

import io.gravitee.definition.model.VirtualHost;
import java.util.Collection;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface VirtualHostService {
    /**
     * This method sanitizes the virtual host definitions and verifies that the given virtual hosts are valid among all the existing
     * virtual hosts for all the APIs.
     *
     * @param virtualHosts
     * @return sanitized virtualHosts
     */
    default Collection<VirtualHost> sanitizeAndValidate(Collection<VirtualHost> virtualHosts) {
        return sanitizeAndValidate(virtualHosts, null);
    }

    /**
     * This method sanitized the virtual host definitions verifies that the given virtual hosts are valid among all the existing
     * virtual hosts for all the APIs.
     *
     * Unlike the first method, this one must be used when validating the virtual hosts for an existing API (in the case
     * of an update of the API).
     *
     * @param virtualHosts
     * @param api
     * @return sanitized virtualHosts
     */
    Collection<VirtualHost> sanitizeAndValidate(Collection<VirtualHost> virtualHosts, String api);

    /**
     * this method sanitizes the path of the virtual host.
     * @param virtualHost
     * @return virtualHost sanitized
     */
    VirtualHost sanitize(VirtualHost virtualHost);
}
