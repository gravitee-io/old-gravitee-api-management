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

import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApplicationService {
    ApplicationEntity findById(String applicationId);
    Set<ApplicationListItem> findByIds(List<String> applicationIds);

    Set<ApplicationListItem> findByUser(String username);

    Set<ApplicationListItem> findByNameAndStatus(String username, String name, String status);

    Set<ApplicationListItem> findByOrganization(String organizationId);

    Set<ApplicationListItem> findByGroups(List<String> groupId);
    Set<ApplicationListItem> findByGroupsAndStatus(List<String> groupId, String status);

    Set<ApplicationListItem> findAll();

    Set<ApplicationListItem> findByStatus(String status);

    ApplicationEntity create(NewApplicationEntity application, String username);

    ApplicationEntity create(NewApplicationEntity application, String username, String environmentId);

    ApplicationEntity update(String applicationId, UpdateApplicationEntity application);

    ApplicationEntity renewClientSecret(String applicationId);

    ApplicationEntity restore(String applicationId);

    void archive(String applicationId);

    InlinePictureEntity getPicture(String apiId);

    InlinePictureEntity getBackground(String application);
}
