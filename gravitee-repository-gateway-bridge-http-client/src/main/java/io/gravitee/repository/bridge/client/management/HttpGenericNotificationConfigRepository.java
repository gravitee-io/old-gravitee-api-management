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
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpGenericNotificationConfigRepository extends AbstractRepository implements GenericNotificationConfigRepository {

    @Override
    public GenericNotificationConfig create(GenericNotificationConfig genericNotificationConfig) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public GenericNotificationConfig update(GenericNotificationConfig genericNotificationConfig) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void delete(String id) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Optional<GenericNotificationConfig> findById(String id) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public List<GenericNotificationConfig> findByReferenceAndHook(String hook, NotificationReferenceType referenceType, String referenceId) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public List<GenericNotificationConfig> findByReference(NotificationReferenceType referenceType, String referenceId) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void deleteByConfig(String config) throws TechnicalException {
        throw new IllegalStateException();
    }
}
