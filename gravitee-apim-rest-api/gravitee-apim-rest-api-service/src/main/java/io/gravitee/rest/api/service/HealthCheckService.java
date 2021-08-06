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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.analytics.Analytics;
import io.gravitee.rest.api.model.analytics.query.DateHistogramQuery;
import io.gravitee.rest.api.model.analytics.query.LogQuery;
import io.gravitee.rest.api.model.healthcheck.ApiMetrics;
import io.gravitee.rest.api.model.healthcheck.Log;
import io.gravitee.rest.api.model.healthcheck.SearchLogResponse;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface HealthCheckService {
    ApiMetrics getAvailability(String api, String field);

    ApiMetrics getResponseTime(String api, String field);

    SearchLogResponse findByApi(String api, LogQuery logQuery, Boolean transition);

    Log findLog(String id);

    Analytics query(DateHistogramQuery query);
}
