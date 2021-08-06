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
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RoleRepositoryProxy extends AbstractProxy<RoleRepository> implements RoleRepository {

    @Override
    public Optional<Role> findById(String roleId) throws TechnicalException {
        return target.findById(roleId);
    }

    @Override
    public Role create(Role item) throws TechnicalException {
        return target.create(item);
    }

    @Override
    public Role update(Role item) throws TechnicalException {
        return target.update(item);
    }

    @Override
    public Set<Role> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public void delete(String roleId) throws TechnicalException {
        target.delete(roleId);
    }

    @Override
    public Set<Role> findAllByReferenceIdAndReferenceType(String referenceId, RoleReferenceType referenceType) throws TechnicalException {
        return target.findAllByReferenceIdAndReferenceType(referenceId, referenceType);
    }

    @Override
    public Set<Role> findByScopeAndReferenceIdAndReferenceType(RoleScope scope, String referenceId, RoleReferenceType referenceType)
        throws TechnicalException {
        return target.findByScopeAndReferenceIdAndReferenceType(scope, referenceId, referenceType);
    }

    @Override
    public Optional<Role> findByScopeAndNameAndReferenceIdAndReferenceType(
        RoleScope scope,
        String name,
        String referenceId,
        RoleReferenceType referenceType
    ) throws TechnicalException {
        return target.findByScopeAndNameAndReferenceIdAndReferenceType(scope, name, referenceId, referenceType);
    }
}
