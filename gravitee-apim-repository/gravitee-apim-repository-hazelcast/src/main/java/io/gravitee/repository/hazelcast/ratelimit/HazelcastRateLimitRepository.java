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
package io.gravitee.repository.hazelcast.ratelimit;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import io.gravitee.repository.hazelcast.ratelimit.configuration.HazelcastRateLimitConfiguration;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.reactivex.*;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HazelcastRateLimitRepository implements RateLimitRepository<RateLimit> {

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private HazelcastRateLimitConfiguration configuration;

    private IMap<String, RateLimit> counters;

    @PostConstruct
    public void afterPropertiesSet() {
        counters = hazelcastInstance.getMap(configuration.getRateLimitMap());
    }

    @Override
    public Single<RateLimit> incrementAndGet(String key, long weight, Supplier<RateLimit> supplier) {
        final long now = System.currentTimeMillis();

        Lock lock = hazelcastInstance.getLock("lock-rl-" + key);

        return Completable
            .create(
                emitter -> {
                    lock.lock();
                    emitter.onComplete();
                }
            )
            .subscribeOn(Schedulers.computation())
            .andThen(
                Single.defer(
                    () ->
                        Maybe
                            .fromFuture(counters.getAsync(key))
                            .switchIfEmpty((SingleSource<RateLimit>) observer -> observer.onSuccess(supplier.get()))
                            .flatMap(
                                (Function<RateLimit, SingleSource<RateLimit>>) rateLimit -> {
                                    if (rateLimit.getResetTime() < now) {
                                        rateLimit = supplier.get();
                                    }

                                    rateLimit.setCounter(rateLimit.getCounter() + weight);

                                    final RateLimit finalRateLimit = rateLimit;

                                    return Completable
                                        .fromFuture(
                                            counters.setAsync(
                                                rateLimit.getKey(),
                                                rateLimit,
                                                now - rateLimit.getResetTime(),
                                                TimeUnit.MILLISECONDS
                                            )
                                        )
                                        .andThen(Single.defer(() -> Single.just(finalRateLimit)))
                                        .doFinally(lock::unlock);
                                }
                            )
                )
            );
    }
}
