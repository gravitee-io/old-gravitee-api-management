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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IdentityProviderActivationRepository;
import io.gravitee.repository.management.model.IdentityProviderActivation;
import io.gravitee.repository.management.model.IdentityProviderActivationReferenceType;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpIdentityProviderActivationRepository extends AbstractRepository implements IdentityProviderActivationRepository {

    @Override
    public Optional<IdentityProviderActivation> findById(
        String identityProviderId,
        String referenceId,
        IdentityProviderActivationReferenceType referenceType
    ) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<IdentityProviderActivation> findAll() throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<IdentityProviderActivation> findAllByIdentityProviderId(String identityProviderId) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<IdentityProviderActivation> findAllByReferenceIdAndReferenceType(
        String referenceId,
        IdentityProviderActivationReferenceType referenceType
    ) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public IdentityProviderActivation create(IdentityProviderActivation identityProviderActivation) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void delete(String identityProviderId, String referenceId, IdentityProviderActivationReferenceType referenceType)
        throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void deleteByIdentityProviderId(String identityProviderId) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void deleteByReferenceIdAndReferenceType(String referenceId, IdentityProviderActivationReferenceType referenceType)
        throws TechnicalException {
        throw new IllegalStateException();
    }
}
