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
package io.gravitee.rest.api.model.promotion;

/**
 * @author Yann Tavernier (yann.tavernier at graviteesource.com)
 * @author Gaetan Maisse (gaetan.maisse at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PromotionRequestEntity {

    private String targetEnvCockpitId;
    private String targetEnvName;

    public String getTargetEnvCockpitId() {
        return targetEnvCockpitId;
    }

    public void setTargetEnvCockpitId(String targetEnvCockpitId) {
        this.targetEnvCockpitId = targetEnvCockpitId;
    }

    public String getTargetEnvName() {
        return targetEnvName;
    }

    public void setTargetEnvName(String targetEnvName) {
        this.targetEnvName = targetEnvName;
    }
}
