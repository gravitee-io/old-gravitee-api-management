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
package io.gravitee.rest.api.service.quality;

import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.ParameterService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiQualityMetricDescription implements ApiQualityMetric {

    @Autowired
    ParameterService parameterService;

    @Override
    public Key getWeightKey() {
        return Key.API_QUALITY_METRICS_DESCRIPTION_WEIGHT;
    }

    @Override
    public boolean isValid(ApiEntity api) {
        int minLength = Integer.parseInt(Key.API_QUALITY_METRICS_DESCRIPTION_MIN_LENGTH.defaultValue());
        List<String> minLengthParam = parameterService.findAll(Key.API_QUALITY_METRICS_DESCRIPTION_MIN_LENGTH);
        if (!minLengthParam.isEmpty()) {
            minLength = Integer.parseInt(minLengthParam.get(0));
        }

        return api.getDescription() != null && api.getDescription().length() >= minLength;
    }
}
