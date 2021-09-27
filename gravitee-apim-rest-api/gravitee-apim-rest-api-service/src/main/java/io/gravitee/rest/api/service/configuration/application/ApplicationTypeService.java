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

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
package io.gravitee.rest.api.service.configuration.application;

import io.gravitee.rest.api.model.configuration.application.ApplicationTypeEntity;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypesEntity;

public interface ApplicationTypeService {
    ApplicationTypesEntity getEnabledApplicationTypes();

    ApplicationTypeEntity getApplicationType(String typeId);
}