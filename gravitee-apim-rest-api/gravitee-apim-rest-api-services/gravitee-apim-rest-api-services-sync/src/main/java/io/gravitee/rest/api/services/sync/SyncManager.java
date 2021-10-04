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
package io.gravitee.rest.api.services.sync;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.event.EventManager;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SyncManager {

    private final Logger logger = LoggerFactory.getLogger(SyncManager.class);

    private static final int TIMEFRAME_BEFORE_DELAY = 10 * 60 * 1000;
    private static final int TIMEFRAME_AFTER_DELAY = 1 * 60 * 1000;

    @Autowired
    private DictionaryManager dictionaryManager;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ApiManager apiManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventManager eventManager;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private UserService userService;

    private final AtomicLong counter = new AtomicLong(0);

    private long lastRefreshAt = -1;

    public void refresh() {
        logger.debug("Synchronization #{} started at {}", counter.incrementAndGet(), Instant.now());
        logger.debug("Refreshing state...");

        long nextLastRefreshAt = System.currentTimeMillis();

        try {
            synchronizeApis(nextLastRefreshAt);
        } catch (Exception ex) {
            logger.error("An error occurs while synchronizing APIs", ex);
        }

        try {
            synchronizeDictionaries(nextLastRefreshAt);
        } catch (Exception ex) {
            logger.error("An error occurs while synchronizing dictionaries", ex);
        }

        lastRefreshAt = nextLastRefreshAt;
        logger.debug("Synchronization #{} ended at {}", counter.get(), Instant.now());
    }

    private void synchronizeApis(long nextLastRefreshAt) {
        final EventCriteria.Builder criteriaBuilder = new EventCriteria.Builder()
            .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
            .from(lastRefreshAt - TIMEFRAME_BEFORE_DELAY)
            .to(nextLastRefreshAt + TIMEFRAME_AFTER_DELAY);

        Map<String, Event> apiEvents = eventRepository
            .searchLatest(criteriaBuilder.build(), Event.EventProperties.API_ID, null, null)
            .stream()
            .collect(toMap(event -> event.getProperties().get(Event.EventProperties.API_ID.getValue()), event -> event));

        // Then, compute events
        computeApiEvents(apiEvents);
    }

    private void synchronizeDictionaries(long nextLastRefreshAt) throws Exception {
        final EventCriteria.Builder criteriaBuilder = new EventCriteria.Builder()
            .types(EventType.START_DICTIONARY, EventType.STOP_DICTIONARY)
            .from(lastRefreshAt - TIMEFRAME_BEFORE_DELAY)
            .to(nextLastRefreshAt + TIMEFRAME_AFTER_DELAY);

        Map<String, Event> dictionaryEvents = eventRepository
            .searchLatest(criteriaBuilder.build(), Event.EventProperties.DICTIONARY_ID, null, null)
            .stream()
            .collect(toMap(event -> event.getProperties().get(Event.EventProperties.DICTIONARY_ID.getValue()), event -> event));

        computeDictionaryEvents(dictionaryEvents);
    }

    private void computeDictionaryEvents(Map<String, Event> dictionaryEvents) {
        dictionaryEvents.forEach(
            (id, event) -> {
                switch (event.getType()) {
                    case START_DICTIONARY:
                        dictionaryManager.start(id);
                        break;
                    case STOP_DICTIONARY:
                        dictionaryManager.stop(id);
                        break;
                    default:
                        break;
                }
            }
        );
    }

    private void computeApiEvents(Map<String, Event> apiEvents) {
        final int parallelism = Runtime.getRuntime().availableProcessors() * 2;

        if (apiEvents.size() > parallelism) {
            final ForkJoinPool.ForkJoinWorkerThreadFactory factory = pool -> {
                final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                worker.setName("gio.sync-" + worker.getPoolIndex());
                return worker;
            };

            ForkJoinPool customThreadPool = new ForkJoinPool(parallelism, factory, null, false);
            customThreadPool.submit(() -> apiEvents.entrySet().parallelStream().forEach(e -> processApiEvent(e.getKey(), e.getValue())));
            customThreadPool.shutdown();
        } else {
            apiEvents.forEach(this::processApiEvent);
        }
    }

    private void processApiEvent(String apiId, Event apiEvent) {
        switch (apiEvent.getType()) {
            case UNPUBLISH_API:
            case STOP_API:
                apiManager.undeploy(apiId);
                break;
            case START_API:
            case PUBLISH_API:
                try {
                    // Read API definition from event
                    Api payloadApi = objectMapper.readValue(apiEvent.getPayload(), Api.class);

                    // API to deploy
                    ApiEntity apiToDeploy = convert(payloadApi);

                    if (apiToDeploy != null) {
                        // Get deployed API
                        ApiEntity deployedApi = apiManager.get(apiToDeploy.getId());

                        // API is not yet deployed, so let's do it !
                        if (deployedApi == null) {
                            apiManager.deploy(apiToDeploy);
                        } else {
                            if (deployedApi.getDeployedAt().before(apiToDeploy.getDeployedAt())) {
                                apiManager.update(apiToDeploy);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error while determining deployed APIs store into events payload", e);
                }
                break;
            default:
                break;
        }
    }

    private ApiEntity convert(Api api) {
        ApiEntity apiEntity = new ApiEntity();

        apiEntity.setId(api.getId());
        apiEntity.setName(api.getName());
        apiEntity.setDeployedAt(api.getDeployedAt());
        apiEntity.setCreatedAt(api.getCreatedAt());

        if (api.getDefinition() != null) {
            try {
                io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(
                    api.getDefinition(),
                    io.gravitee.definition.model.Api.class
                );

                apiEntity.setProxy(apiDefinition.getProxy());
                apiEntity.setPaths(apiDefinition.getPaths());
                apiEntity.setServices(apiDefinition.getServices());
                apiEntity.setResources(apiDefinition.getResources());
                apiEntity.setProperties(apiDefinition.getProperties());
                apiEntity.setTags(apiDefinition.getTags());
                if (apiDefinition.getPathMappings() != null) {
                    apiEntity.setPathMappings(new HashSet<>(apiDefinition.getPathMappings().keySet()));
                }
            } catch (IOException ioe) {
                logger.error("Unexpected error while generating API definition", ioe);
            }
        }
        apiEntity.setUpdatedAt(api.getUpdatedAt());
        apiEntity.setVersion(api.getVersion());
        apiEntity.setDescription(api.getDescription());
        apiEntity.setPicture(api.getPicture());
        apiEntity.setBackground(api.getBackground());
        apiEntity.setCategories(api.getCategories());

        final LifecycleState lifecycleState = api.getLifecycleState();
        if (lifecycleState != null) {
            apiEntity.setState(Lifecycle.State.valueOf(lifecycleState.name()));
        }
        final ApiLifecycleState apiLifecycleState = api.getApiLifecycleState();
        if (apiLifecycleState != null) {
            apiEntity.setLifecycleState(io.gravitee.rest.api.model.api.ApiLifecycleState.valueOf(apiLifecycleState.name()));
        }
        if (api.getVisibility() != null) {
            apiEntity.setVisibility(io.gravitee.rest.api.model.Visibility.valueOf(api.getVisibility().toString()));
        }

        MembershipEntity optPrimaryOwner = membershipService.getPrimaryOwner(MembershipReferenceType.API, api.getId());
        final UserEntity user = userService.findById(optPrimaryOwner.getMemberId());
        apiEntity.setPrimaryOwner(new PrimaryOwnerEntity(user));

        return apiEntity;
    }
}
