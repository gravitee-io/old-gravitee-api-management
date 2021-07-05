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
package io.gravitee.repository.bridge.client.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.management.model.UserReferenceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpUserRepository extends AbstractRepository implements UserRepository {

    @Override
    public Optional<User> findById(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public User create(User item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public User update(User item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void delete(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<User> findByIds(List<String> ids) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Page<User> search(UserCriteria criteria, Pageable pageable) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Optional<User> findBySource(String source, String sourceId, String referenceId, UserReferenceType referenceType) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Optional<User> findByEmail(String email, String referenceId, UserReferenceType referenceType) throws TechnicalException {
        throw new IllegalStateException();
    }
}
