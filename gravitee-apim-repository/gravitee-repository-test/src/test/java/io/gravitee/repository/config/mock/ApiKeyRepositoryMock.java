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

import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;

import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyRepositoryMock extends AbstractRepositoryMock<ApiKeyRepository> {

    public ApiKeyRepositoryMock() {
        super(ApiKeyRepository.class);
    }

    @Override
    void prepare(ApiKeyRepository apiKeyRepository) throws Exception {
        final ApiKey apiKey = mock(ApiKey.class);
        when(apiKey.getKey()).thenReturn("apiKey");
        when(apiKey.getExpireAt()).thenReturn(parse("11/02/2016"));
        when(apiKey.getSubscription()).thenReturn("subscription1");
        when(apiKey.isRevoked()).thenReturn(true);
        when(apiKey.isPaused()).thenReturn(true);
        when(apiKeyRepository.findById(anyString())).thenReturn(empty());
        when(apiKeyRepository.findById("d449098d-8c31-4275-ad59-8dd707865a33")).thenReturn(of(apiKey));
        when(apiKeyRepository.findById("apiKey")).thenReturn(of(apiKey));
        when(apiKeyRepository.findBySubscription("subscription1")).thenReturn(newSet(apiKey, mock(ApiKey.class)));

        when(apiKeyRepository.update(argThat(o -> o == null || o.getKey().equals("unknown")))).thenThrow(new IllegalStateException());

        ApiKey mockCriteria1 = mock(ApiKey.class);
        ApiKey mockCriteria1Revoked = mock(ApiKey.class);
        ApiKey mockCriteria2 = mock(ApiKey.class);
        when(mockCriteria1.getKey()).thenReturn("findByCriteria1");
        when(mockCriteria1Revoked.getKey()).thenReturn("findByCriteria1Revoked");
        when(mockCriteria2.getKey()).thenReturn("findByCriteria2");
        when(apiKeyRepository.findByCriteria(argThat(o -> o == null || o.getFrom() == 0))).thenReturn(asList(mockCriteria1, mockCriteria2));
        when(apiKeyRepository.findByCriteria(argThat(o -> o == null || o.getTo() == 1486771400000L))).thenReturn(singletonList(mockCriteria1));
        when(apiKeyRepository.findByCriteria(argThat(o -> o == null || o.isIncludeRevoked()))).thenReturn(asList(mockCriteria2,mockCriteria1Revoked,mockCriteria1));
    }
}
