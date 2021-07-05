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
package io.gravitee.repository.management.api.search;

import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;

import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiCriteria {

    private List<String> ids;
    private List<String> groups;
    private String category;
    private String label;
    private LifecycleState state;
    private Visibility visibility;
    private String version;
    private String name;
    private List<ApiLifecycleState> lifecycleStates;
    private String environmentId;

    ApiCriteria(ApiCriteria.Builder builder) {
        this.ids = builder.ids;
        this.groups = builder.groups;
        this.category = builder.category;
        this.label = builder.label;
        this.state = builder.state;
        this.visibility = builder.visibility;
        this.version = builder.version;
        this.name = builder.name;
        this.lifecycleStates = builder.lifecycleStates;
        this.environmentId = builder.environmentId;
    }

    public List<String> getIds() {
        return ids;
    }

    public List<String> getGroups() {
        return groups;
    }

    public String getCategory() {
        return category;
    }

    public String getLabel() {
        return label;
    }

    public LifecycleState getState() {
        return state;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public List<ApiLifecycleState> getLifecycleStates() {
        return lifecycleStates;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApiCriteria)) return false;
        ApiCriteria that = (ApiCriteria) o;
        return Objects.equals(ids, that.ids) &&
                Objects.equals(groups, that.groups) &&
                Objects.equals(category, that.category) &&
                Objects.equals(label, that.label) &&
                Objects.equals(state, that.state) &&
                Objects.equals(visibility, that.visibility) &&
                Objects.equals(version, that.version) &&
                Objects.equals(name, that.name) &&
                Objects.equals(lifecycleStates, that.lifecycleStates) &&
                Objects.equals(environmentId, that.environmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ids, groups, category, label, state, visibility, version, name, lifecycleStates, environmentId);
    }

    public static class Builder {
        private List<String> ids;
        private List<String> groups;
        private String category;
        private String label;
        private LifecycleState state;
        private Visibility visibility;
        private String version;
        private String name;
        private List<ApiLifecycleState> lifecycleStates;
        private String environmentId;

        public ApiCriteria.Builder ids(final String... id) {
            this.ids = asList(id);
            return this;
        }

        public ApiCriteria.Builder groups(final String... group) {
            this.groups = asList(group);
            return this;
        }

        public ApiCriteria.Builder category(final String category) {
            this.category = category;
            return this;
        }

        public ApiCriteria.Builder label(final String label) {
            this.label = label;
            return this;
        }

        public ApiCriteria.Builder state(final LifecycleState state) {
            this.state = state;
            return this;
        }

        public ApiCriteria.Builder visibility(final Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public ApiCriteria.Builder version(final String version) {
            this.version = version;
            return this;
        }

        public ApiCriteria.Builder name(final String name) {
            this.name = name;
            return this;
        }

        public ApiCriteria.Builder lifecycleStates(final List<ApiLifecycleState> lifecycleStates) {
            this.lifecycleStates = lifecycleStates;
            return this;
        }

        public ApiCriteria.Builder environmentId(final String environmentId) {
            this.environmentId = environmentId;
            return this;
        }
        
        public ApiCriteria build() {
            return new ApiCriteria(this);
        }
    }
}
