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
package io.gravitee.repository.mongodb;

import io.gravitee.repository.Repository;
import io.gravitee.repository.Scope;
import io.gravitee.repository.mongodb.management.ManagementRepositoryConfiguration;
import io.gravitee.repository.mongodb.ratelimit.RateLimitRepositoryConfiguration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MongoRepository implements Repository {

    @Override
    public String type() {
        return "mongodb";
    }

    @Override
    public Scope[] scopes() {
        return new Scope [] {
                Scope.MANAGEMENT,
                Scope.RATE_LIMIT
        };
    }
    @Override
    public Class<?> configuration(Scope scope) {
        switch (scope) {
            case MANAGEMENT:
                return ManagementRepositoryConfiguration.class;
            case RATE_LIMIT:
                return RateLimitRepositoryConfiguration.class;

        }

        return null;
    }
}
