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
import io.gravitee.repository.management.api.EntrypointRepository;
import io.gravitee.repository.management.model.Entrypoint;
import io.gravitee.repository.management.model.EntrypointReferenceType;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class EntrypointRepositoryProxy extends AbstractProxy<EntrypointRepository> implements EntrypointRepository {

    @Override
    public Optional<Entrypoint> findById(String s) throws TechnicalException {
        return target.findById(s);
    }

    @Override
    public Optional<Entrypoint> findByIdAndReference(String s, String referenceId, EntrypointReferenceType referenceType)
        throws TechnicalException {
        return target.findByIdAndReference(s, referenceId, referenceType);
    }

    @Override
    public Entrypoint create(Entrypoint item) throws TechnicalException {
        return target.create(item);
    }

    @Override
    public Entrypoint update(Entrypoint item) throws TechnicalException {
        return target.update(item);
    }

    @Override
    public void delete(String s) throws TechnicalException {
        target.delete(s);
    }

    @Override
    public Set<Entrypoint> findByReference(String referenceId, EntrypointReferenceType referenceType) throws TechnicalException {
        return target.findByReference(referenceId, referenceType);
    }

    @Override
    public Set<Entrypoint> findAll() throws TechnicalException {
        return target.findAll();
    }
}
