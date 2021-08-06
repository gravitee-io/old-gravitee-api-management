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
package io.gravitee.rest.api.service.impl.upgrade;

import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.UserStatus;
import io.gravitee.rest.api.model.UpdateUserEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultUserStatusUpgrader implements Upgrader, Ordered {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(DefaultUserStatusUpgrader.class);

    @Autowired
    private UserService userService;

    @Override
    public boolean upgrade() {
        // Initialize default user status
        UpdateUserEntity updateUserEntity = new UpdateUserEntity();
        updateUserEntity.setStatus(UserStatus.ACTIVE.name());
        userService
            .search(new UserCriteria.Builder().noStatus().build(), new PageableImpl(1, Integer.MAX_VALUE))
            .getContent()
            .forEach(
                userEntity -> {
                    if (userEntity.getStatus() == null) {
                        userService.update(userEntity.getId(), updateUserEntity);
                    }
                }
            );

        return true;
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
