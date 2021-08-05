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

import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.DuplicateApiEntity;

public interface ApiDuplicatorService {
    ApiEntity createWithImportedDefinition(String apiDefinitionOrURL, String userId, String organizationId, String environmentId);

    ApiEntity duplicate(ApiEntity apiEntity, DuplicateApiEntity duplicateApiEntity, String organizationId, String environmentId);

    String exportAsJson(String apiId, String exportVersion, String... filteredFields);

    ApiEntity updateWithImportedDefinition(
        String apiId,
        String apiDefinitionOrURL,
        String userId,
        String organizationId,
        String environmentId
    );
}
