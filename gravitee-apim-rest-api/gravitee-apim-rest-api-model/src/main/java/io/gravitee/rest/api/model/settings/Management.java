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
package io.gravitee.rest.api.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Management {

    @ParameterKey(Key.CONSOLE_SUPPORT_ENABLED)
    private Enabled support;

    @ParameterKey(Key.MANAGEMENT_TITLE)
    private String title;

    @ParameterKey(Key.MANAGEMENT_URL)
    private String url;

    @ParameterKey(Key.CONSOLE_USERCREATION_ENABLED)
    private Enabled userCreation;

    @ParameterKey(Key.CONSOLE_USERCREATION_AUTOMATICVALIDATION_ENABLED)
    private Enabled automaticValidation;

    public Enabled getSupport() {
        return support;
    }

    public void setSupport(Enabled support) {
        this.support = support;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Enabled getUserCreation() {
        return userCreation;
    }

    public void setUserCreation(Enabled userCreation) {
        this.userCreation = userCreation;
    }

    public Enabled getAutomaticValidation() {
        return automaticValidation;
    }

    public void setAutomaticValidation(Enabled automaticValidation) {
        this.automaticValidation = automaticValidation;
    }
}
