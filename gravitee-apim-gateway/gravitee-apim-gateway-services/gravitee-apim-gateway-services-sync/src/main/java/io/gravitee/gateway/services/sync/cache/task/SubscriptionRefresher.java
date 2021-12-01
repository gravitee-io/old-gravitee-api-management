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
package io.gravitee.gateway.services.sync.cache.task;

import static io.gravitee.repository.management.model.Subscription.Status.*;

import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.resource.cache.api.Cache;
import io.gravitee.resource.cache.api.Element;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class SubscriptionRefresher implements Callable<Result<Boolean>> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private SubscriptionRepository subscriptionRepository;

    private Cache<String, Object> cache;

    protected Result<Boolean> doRefresh(SubscriptionCriteria criteria) {
        logger.debug("Refresh api-keys");

        try {
            subscriptionRepository.search(criteria).forEach(this::saveOrUpdate);

            return Result.success(true);
        } catch (Exception ex) {
            return Result.failure(ex);
        }
    }

    private void saveOrUpdate(Subscription subscription) {
        String key = subscription.getApi() + '-' + subscription.getClientId();

        Element<String, Object> element = cache.get(subscription.getId());

        if ((CLOSED.equals(subscription.getStatus()) || PAUSED.equals(subscription.getStatus())) && element != null) {
            cache.evict(subscription.getId());
            String oldKey = (String) element.getValue();
            Subscription eltSubscription = cache.get(oldKey) != null ? ((Subscription) cache.get(oldKey).getValue()) : null;
            if (eltSubscription != null && eltSubscription.getId().equals(subscription.getId())) {
                cache.evict(oldKey);
            }
        } else if (ACCEPTED.equals(subscription.getStatus())) {
            logger.debug(
                "Cache a subscription: plan[{}] application[{}] client_id[{}]",
                subscription.getPlan(),
                subscription.getApplication(),
                subscription.getClientId()
            );
            cache.put(new Element<>((subscription.getId()), key));

            // Delete useless information to preserve memory
            subscription.setGeneralConditionsContentPageId(null);
            subscription.setRequest(null);
            subscription.setReason(null);
            subscription.setSubscribedBy(null);
            subscription.setProcessedBy(null);

            cache.put(new Element<>(key, subscription));

            if (element != null) {
                final String oldKey = (String) element.getValue();
                if (!oldKey.equals(key)) {
                    cache.evict(oldKey);
                }
            }
        }
    }

    public void setSubscriptionRepository(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public void setCache(Cache<String, Object> cache) {
        this.cache = cache;
    }
}
