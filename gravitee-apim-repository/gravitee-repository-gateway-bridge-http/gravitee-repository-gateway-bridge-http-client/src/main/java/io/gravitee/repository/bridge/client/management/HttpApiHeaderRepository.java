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
import io.gravitee.repository.management.api.ApiHeaderRepository;
import io.gravitee.repository.management.model.ApiHeader;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpApiHeaderRepository extends AbstractRepository implements ApiHeaderRepository {
    @Override
    public Set<ApiHeader> findAll() throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Optional<ApiHeader> findById(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public ApiHeader create(ApiHeader apiHeader) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public ApiHeader update(ApiHeader apiHeader) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void delete(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<ApiHeader> findAllByEnvironment(String environmentId) throws TechnicalException {
        throw new IllegalStateException();
    }
}
