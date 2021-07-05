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
package io.gravitee.repository.config.mock;

import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Subscription;

import java.util.Collections;
import java.util.Date;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionRepositoryMock extends AbstractRepositoryMock<SubscriptionRepository> {

    public SubscriptionRepositoryMock() {
        super(SubscriptionRepository.class);
    }

    @Override
    void prepare(SubscriptionRepository subscriptionRepository) throws Exception {
        final Subscription sub1 = new Subscription();
        sub1.setId("sub1");
        sub1.setPlan("plan1");
        sub1.setApplication("app1");
        sub1.setApi("api1");
        sub1.setRequest("request");
        sub1.setReason("reason");
        sub1.setStatus(Subscription.Status.PENDING);
        sub1.setProcessedBy("user1");
        sub1.setSubscribedBy("user2");
        sub1.setStartingAt(new Date(1439022010883L));
        sub1.setEndingAt(new Date(1449022010883L));
        sub1.setCreatedAt(new Date(1459022010883L));
        sub1.setUpdatedAt(new Date(1469022010883L));
        sub1.setProcessedAt(new Date(1479022010883L));
        sub1.setPausedAt(new Date(1479022010883L));
        sub1.setClientId("my-client-id");

        final Subscription sub3 = new Subscription();
        sub3.setId("sub3");

        final Subscription sub4 = new Subscription();
        sub4.setId("sub4");

        when(subscriptionRepository.search(
                new SubscriptionCriteria.Builder()
                        .plans(singleton("plan1"))
                        .build()))
                .thenReturn(singletonList(sub1));
        when(subscriptionRepository.search(
                new SubscriptionCriteria.Builder()
                        .plans(singleton("unknown-plan"))
                        .build()))
                .thenReturn(Collections.emptyList());

        when(subscriptionRepository.search(
                new SubscriptionCriteria.Builder()
                        .applications(singleton("app1"))
                        .build()))
                .thenReturn(asList(sub3, sub4, sub1));
        when(subscriptionRepository.search(
                new SubscriptionCriteria.Builder()
                        .applications(singleton("unknown-app"))
                        .build()))
                .thenReturn(Collections.emptyList());

        when(subscriptionRepository.findById("sub1")).thenReturn(of(sub1));
        when(subscriptionRepository.findById("unknown-sub")).thenReturn(empty());
        when(subscriptionRepository.findById("sub2")).thenReturn(empty());
        when(subscriptionRepository.update(sub1)).thenReturn(sub1);

        when(subscriptionRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());

        when(subscriptionRepository.search(
                new SubscriptionCriteria.Builder()
                        .from(1469022010883L)
                        .to(1569022010883L)
                        .build()))
                .thenReturn(singletonList(sub1));

        when(subscriptionRepository.search(any(), eq(new PageableBuilder().pageNumber(0).pageSize(2).build())))
                .thenReturn(new io.gravitee.common.data.domain.Page<>(asList(sub3, sub1), 0, 2, 2));
        when(subscriptionRepository.search(any(), eq(new PageableBuilder().pageNumber(1).pageSize(2).build())))
                .thenReturn(new io.gravitee.common.data.domain.Page<>(emptyList(), 1, 0, 2));
    }
}
