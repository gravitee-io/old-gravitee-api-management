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
package io.gravitee.repository.healthcheck.query.log;

import io.gravitee.repository.healthcheck.query.AbstractQuery;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogsQuery extends AbstractQuery<LogsResponse> {

    private int size = 20;

    private int page = 0;

    private Boolean transition;

    private long from;

    private long to;

    @Override
    public Class<LogsResponse> responseType() {
        return LogsResponse.class;
    }

    public int size() {
        return size;
    }

    void size(int size) {
        this.size = size;
    }

    public int page() {
        return page;
    }

    void page(int page) {
        this.page = page;
    }

    public Boolean transition() {
        return transition;
    }

    public void transition(Boolean transition) {
        this.transition = transition;
    }

    public long from() {
        return from;
    }

    void from(long from) {
        this.from = from;
    }

    public long to() {
        return to;
    }

    void to(long to) {
        this.to = to;
    }
}
