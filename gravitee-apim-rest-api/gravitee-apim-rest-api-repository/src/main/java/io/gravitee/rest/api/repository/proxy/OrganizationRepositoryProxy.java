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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class OrganizationRepositoryProxy extends AbstractProxy<OrganizationRepository> implements OrganizationRepository {

    @Override
    public Optional<Organization> findById(String s) throws TechnicalException {
        return target.findById(s);
    }

    @Override
    public Organization create(Organization item) throws TechnicalException {
        return target.create(item);
    }

    @Override
    public Organization update(Organization item) throws TechnicalException {
        return target.update(item);
    }

    @Override
    public void delete(String s) throws TechnicalException {
        target.delete(s);
    }

    @Override
    public Long count() throws TechnicalException {
        return target.count();
    }

    @Override
    public Set<Organization> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public Set<Organization> findByHrids(Set<String> hrids) throws TechnicalException {
        return target.findByHrids(hrids);
    }
}
