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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Group;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface GroupRepository extends CrudRepository<Group, String> {
    /**
     * List all Groups
     * @return all groups
     * @throws TechnicalException if something goes wrong
     */
    Set<Group> findAll() throws TechnicalException;

    Set<Group> findAllByEnvironment(String environmentId) throws TechnicalException;

    Set<Group> findByIds(Set<String> ids) throws TechnicalException;

    Set<Group> findAllByOrganization(String organizationId) throws TechnicalException;
}
