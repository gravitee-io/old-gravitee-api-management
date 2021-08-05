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
package io.gravitee.rest.api.service.cockpit.command.handler;

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandHandler;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.goodbye.GoodbyeCommand;
import io.gravitee.cockpit.api.command.goodbye.GoodbyeReply;
import io.gravitee.rest.api.model.promotion.PromotionEntityStatus;
import io.gravitee.rest.api.model.promotion.PromotionQuery;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class GoodbyeCommandHandler implements CommandHandler<GoodbyeCommand, GoodbyeReply> {

    static final String DELETED_STATUS = "DELETED";
    private final Logger logger = LoggerFactory.getLogger(GoodbyeCommandHandler.class);

    private final InstallationService installationService;
    private final PromotionService promotionService;

    public GoodbyeCommandHandler(final InstallationService installationService, PromotionService promotionService) {
        this.installationService = installationService;
        this.promotionService = promotionService;
    }

    @Override
    public Command.Type handleType() {
        return Command.Type.GOODBYE_COMMAND;
    }

    @Override
    public Single<GoodbyeReply> handle(GoodbyeCommand command) {
        final Map<String, String> additionalInformation = this.installationService.getOrInitialize().getAdditionalInformation();
        additionalInformation.put(InstallationService.COCKPIT_INSTALLATION_STATUS, DELETED_STATUS);

        rejectAllPromotionToValidate();

        try {
            this.installationService.setAdditionalInformation(additionalInformation);
            logger.info("Installation status is [{}].", DELETED_STATUS);
            return Single.just(new GoodbyeReply(command.getId(), CommandStatus.SUCCEEDED));
        } catch (Exception ex) {
            logger.info("Error occurred when deleting installation.", ex);
            return Single.just(new GoodbyeReply(command.getId(), CommandStatus.ERROR));
        }
    }

    private void rejectAllPromotionToValidate() {
        PromotionQuery promotionQuery = new PromotionQuery();
        promotionQuery.setStatuses(List.of(PromotionEntityStatus.TO_BE_VALIDATED));

        promotionService
            .search(promotionQuery, null, null)
            .getContent()
            .forEach(
                promotionEntity -> {
                    promotionEntity.setStatus(PromotionEntityStatus.REJECTED);
                    promotionService.createOrUpdate(promotionEntity);
                }
            );
    }
}
