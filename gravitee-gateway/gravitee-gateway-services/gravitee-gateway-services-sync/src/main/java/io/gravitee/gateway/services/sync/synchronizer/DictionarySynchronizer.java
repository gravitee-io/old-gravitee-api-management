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
package io.gravitee.gateway.services.sync.synchronizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

import static io.gravitee.repository.management.model.Event.EventProperties.DICTIONARY_ID;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DictionarySynchronizer extends AbstractSynchronizer {

    private static final int PARALLELISM = Runtime.getRuntime().availableProcessors() * 2;
    private final Logger logger = LoggerFactory.getLogger(DictionarySynchronizer.class);

    @Autowired
    private DictionaryManager dictionaryManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${services.sync.bulk_items:100}")
    protected int bulkItems = 100;

    public void synchronize(long lastRefreshAt, long nextLastRefreshAt) {
        final long start = System.currentTimeMillis();
        final Long count;

        if (lastRefreshAt == -1) {
            count = initialSynchronizeDictionaries(nextLastRefreshAt);
        } else {
            count = this.searchLatestEvents(bulkItems, lastRefreshAt, nextLastRefreshAt, DICTIONARY_ID, EventType.PUBLISH_DICTIONARY, EventType.START_DICTIONARY, EventType.UNPUBLISH_DICTIONARY, EventType.STOP_DICTIONARY)
                    .compose(this::processDictionaryEvents)
                    .count()
                    .blockingGet();
        }

        if (lastRefreshAt == -1) {
            logger.info("{} dictionary(ies) synchronized in {}ms.", count, (System.currentTimeMillis() - start));
        } else {
            logger.debug("{} dictionary(ies) synchronized in {}ms.", count, (System.currentTimeMillis() - start));
        }
    }

    private long initialSynchronizeDictionaries(long nextLastRefreshAt) {
        return this.searchLatestEvents(bulkItems, null, nextLastRefreshAt, DICTIONARY_ID, EventType.PUBLISH_DICTIONARY, EventType.START_DICTIONARY)
                .compose(this::processDictionaryDeployEvents)
                .count()
                .blockingGet();
    }

    @NonNull
    private Flowable<String> processDictionaryEvents(Flowable<Event> upstream) {
        return upstream.groupBy(Event::getType)
                .flatMap(eventsByType -> {
                    if (eventsByType.getKey() == EventType.PUBLISH_DICTIONARY || eventsByType.getKey() == EventType.START_DICTIONARY) {
                        return eventsByType.compose(this::processDictionaryDeployEvents);
                    } else if (eventsByType.getKey() == EventType.UNPUBLISH_DICTIONARY || eventsByType.getKey() == EventType.STOP_DICTIONARY) {
                        return eventsByType.compose(this::processDictionaryUndeployEvents);
                    } else {
                        return Flowable.empty();
                    }
                });
    }

    @NonNull
    private Flowable<String> processDictionaryDeployEvents(Flowable<Event> upstream) {
        return upstream.flatMapMaybe(this::toDictionary)
                .compose(this::deployDictionary);
    }

    @NonNull
    private Flowable<String> processDictionaryUndeployEvents(Flowable<Event> upstream) {
        return upstream.flatMapMaybe(this::toDictionaryId)
                .compose(this::undeployDictionary);
    }

    @NonNull
    private Flowable<String> deployDictionary(Flowable<io.gravitee.gateway.dictionary.model.Dictionary> upstream) {
        return upstream.parallel(PARALLELISM)
                .runOn(Schedulers.from(executor))
                .doOnNext(dictionary -> {
                    try {
                        dictionaryManager.deploy(dictionary);
                    } catch (Exception e) {
                        logger.error("An error occurred when trying to deploy dictionary {} [{}].", dictionary.getName(), dictionary.getId());
                    }
                })
                .sequential()
                .map(io.gravitee.gateway.dictionary.model.Dictionary::getId);
    }

    @NonNull
    private Flowable<String> undeployDictionary(Flowable<String> upstream) {
        return upstream.parallel(PARALLELISM)
                .runOn(Schedulers.from(executor))
                .doOnNext(dictionaryId -> {
                    try {
                        dictionaryManager.undeploy(dictionaryId);
                    } catch (Exception e) {
                        logger.error("An error occurred when trying to undeploy dictionary [{}].", dictionaryId);
                    }
                })
                .sequential();
    }

    private Maybe<io.gravitee.gateway.dictionary.model.Dictionary> toDictionary(Event event) {
        try {
            // Read dictionary definition from event
            return Maybe.just(objectMapper.readValue(event.getPayload(), io.gravitee.gateway.dictionary.model.Dictionary.class));
        } catch (Exception ex) {
            logger.error("Error while determining deployed dictionaries into events payload", ex);
        }

        return Maybe.empty();
    }

    private Maybe<String> toDictionaryId(Event dictionaryEvent) {
        final String dictionaryId = dictionaryEvent.getProperties().get(DICTIONARY_ID.getValue());

        if (dictionaryId == null) {
            logger.error("Unable to extract dictionary info from event [{}].", dictionaryEvent.getId());
            return Maybe.empty();
        }
        return Maybe.just(dictionaryId);
    }
}
