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

import io.gravitee.rest.api.model.ApiKeyEntity;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiKeyService {
    ApiKeyEntity generate(String subscription);

    ApiKeyEntity renew(String subscription);

    void revoke(String apiKey, boolean notify);

    ApiKeyEntity reactivate(String apiKey);

    List<ApiKeyEntity> findBySubscription(String subscription);

    ApiKeyEntity findByKey(String apiKey);

    void delete(String apiKey);

    ApiKeyEntity update(ApiKeyEntity apiKeyEntity);
}
