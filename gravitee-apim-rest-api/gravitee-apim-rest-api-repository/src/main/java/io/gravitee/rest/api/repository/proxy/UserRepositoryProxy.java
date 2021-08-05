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
package io.gravitee.rest.api.repository.proxy;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UserRepositoryProxy extends AbstractProxy<UserRepository> implements UserRepository {

    @Override
    public Optional<User> findById(String id) throws TechnicalException {
        return target.findById(id);
    }

    @Override
    public User create(User user) throws TechnicalException {
        return target.create(user);
    }

    @Override
    public User update(User user) throws TechnicalException {
        return target.update(user);
    }

    @Override
    public Page<User> search(UserCriteria criteria, Pageable pageable) throws TechnicalException {
        return target.search(criteria, pageable);
    }

    @Override
    public Optional<User> findBySource(String sourceId, String userId, String organizationId) throws TechnicalException {
        return target.findBySource(sourceId, userId, organizationId);
    }

    @Override
    public Optional<User> findByEmail(String email, String organizationId) throws TechnicalException {
        return target.findByEmail(email, organizationId);
    }

    @Override
    public Set<User> findByIds(List<String> ids) throws TechnicalException {
        return target.findByIds(ids);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        target.delete(id);
    }
}
