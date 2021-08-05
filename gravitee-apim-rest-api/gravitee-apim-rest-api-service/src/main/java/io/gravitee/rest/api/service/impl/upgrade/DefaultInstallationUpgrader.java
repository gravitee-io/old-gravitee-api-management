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

import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.Upgrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultInstallationUpgrader implements Upgrader, Ordered {

    private final Logger logger = LoggerFactory.getLogger(DefaultInstallationUpgrader.class);

    @Autowired
    private InstallationService installationService;

    @Override
    public boolean upgrade() {
        final InstallationEntity installation = installationService.getOrInitialize();
        logger.info("Current installation id is [{}]", installation.getId());
        return true;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
