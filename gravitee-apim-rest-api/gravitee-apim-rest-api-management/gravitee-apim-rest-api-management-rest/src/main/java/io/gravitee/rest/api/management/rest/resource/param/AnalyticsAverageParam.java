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
package io.gravitee.rest.api.management.rest.resource.param;

import io.gravitee.rest.api.management.rest.resource.param.AnalyticsAverageTypeParam.AnalyticsAverageType;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AnalyticsAverageParam {

    @Min(0)
    @QueryParam("from")
    private long from;

    @Min(0)
    @QueryParam("to")
    private long to;

    @Min(1_000)
    @Max(1_000_000_000)
    @QueryParam("interval")
    private long interval;

    @NotNull
    @QueryParam("type")
    private AnalyticsAverageType type;

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public AnalyticsAverageType getType() {
        return type;
    }

    public void setType(AnalyticsAverageType type) {
        this.type = type;
    }
}
