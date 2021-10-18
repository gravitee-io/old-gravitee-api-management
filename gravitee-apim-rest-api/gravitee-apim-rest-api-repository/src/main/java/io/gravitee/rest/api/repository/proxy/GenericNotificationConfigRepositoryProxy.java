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
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class GenericNotificationConfigRepositoryProxy
    extends AbstractProxy<GenericNotificationConfigRepository>
    implements GenericNotificationConfigRepository {

    @Override
    public GenericNotificationConfig create(GenericNotificationConfig genericNotificationConfig) throws TechnicalException {
        return target.create(genericNotificationConfig);
    }

    @Override
    public GenericNotificationConfig update(GenericNotificationConfig genericNotificationConfig) throws TechnicalException {
        return target.update(genericNotificationConfig);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        target.delete(id);
    }

    @Override
    public Optional<GenericNotificationConfig> findById(String id) throws TechnicalException {
        return target.findById(id);
    }

    @Override
    public List<GenericNotificationConfig> findByReferenceAndHook(String hook, NotificationReferenceType referenceType, String referenceId)
        throws TechnicalException {
        return target.findByReferenceAndHook(hook, referenceType, referenceId);
    }

    @Override
    public List<GenericNotificationConfig> findByReference(NotificationReferenceType referenceType, String referenceId)
        throws TechnicalException {
        return target.findByReference(referenceType, referenceId);
    }

    @Override
    public void deleteByConfig(String config) throws TechnicalException {
        target.deleteByConfig(config);
    }

    @Override
    public Set<GenericNotificationConfig> findAll() throws TechnicalException {
        return target.findAll();
    }
}
