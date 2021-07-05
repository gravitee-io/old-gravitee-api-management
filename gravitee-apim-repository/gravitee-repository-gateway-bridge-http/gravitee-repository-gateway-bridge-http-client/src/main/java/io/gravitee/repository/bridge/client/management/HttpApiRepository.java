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
import io.gravitee.repository.bridge.client.utils.BodyCodecs;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Api;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpApiRepository extends AbstractRepository implements ApiRepository {

    @Override
    public Optional<Api> findById(String apiId) throws TechnicalException {
        return blockingGet(get("/apis/" + apiId, BodyCodecs.optional(Api.class))
                .send());
    }

    @Override
    public Api create(Api item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Api update(Api item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void delete(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Page<Api> search(ApiCriteria apiCriteria, Pageable pageable) {
        throw new IllegalStateException();
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria) {
        throw new IllegalStateException();
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria, ApiFieldExclusionFilter apiFieldExclusionFilter) {
        try {
            return blockingGet(get("/apis", BodyCodecs.list(Api.class))
                    .addQueryParam("excludeDefinition", Boolean.toString(apiFieldExclusionFilter.isDefinition()))
                    .send());
        } catch (TechnicalException te) {
            // Ensure that an exception is thrown and managed by the caller
            throw new IllegalStateException(te);
        }
    }
}
