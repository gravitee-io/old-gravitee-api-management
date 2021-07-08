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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.management.model.UserReferenceType;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface UserRepository extends CrudRepository<User, String> {
    /**
     * Find a {@link User} by its source and user ID.
     *
     * @param source The source identifier.
     * @param sourceId The user identifier (from the source).
     * @param referenceId The reference id of the item the user belongs to
     * @param referenceType The reference type of the item the user belongs to (ENVIRONMENT, ORGANIZATION)
     * @return Option user found
     */
    Optional<User> findBySource(String source, String sourceId, String referenceId, UserReferenceType referenceType)
        throws TechnicalException;

    /**
     * Find a {@link User} by its email.
     *
     * @param email The email to search
     * @param referenceId The reference id of the item the user belongs to
     * @param referenceType The reference type of the item the user belongs to (ENVIRONMENT, ORGANIZATION)
     * @return
     * @throws TechnicalException
     */
    Optional<User> findByEmail(String email, String referenceId, UserReferenceType referenceType) throws TechnicalException;

    /**
     * Find a list of {@link User} by IDs
     *
     * @param ids Identifier of the searched users
     * @return list of users found
     */
    Set<User> findByIds(List<String> ids) throws TechnicalException;

    /**
     * search {@link User}s
     *
     * @return Users found
     */
    Page<User> search(UserCriteria criteria, Pageable pageable) throws TechnicalException;
}
