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

import static java.util.Arrays.asList;

import io.gravitee.repository.management.model.ApplicationStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationCriteria {

    private List<String> ids;
    private String name;
    private List<String> environmentIds;
    private ApplicationStatus status;

    ApplicationCriteria(ApplicationCriteria.Builder builder) {
        this.ids = builder.ids;
        this.name = builder.name;
        this.environmentIds = builder.environmentIds;
        this.status = builder.status;
    }

    public List<String> getIds() {
        return ids;
    }

    public String getName() {
        return name;
    }

    public List<String> getEnvironmentIds() {
        return environmentIds;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplicationCriteria)) return false;
        ApplicationCriteria that = (ApplicationCriteria) o;
        return (
            Objects.equals(ids, that.ids) &&
            Objects.equals(name, that.name) &&
            Objects.equals(environmentIds, that.environmentIds) &&
            Objects.equals(status, that.status)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(ids, name, environmentIds, status);
    }

    public static class Builder {

        private List<String> ids;
        private String name;
        private List<String> environmentIds;
        private ApplicationStatus status;

        public ApplicationCriteria.Builder ids(final String... id) {
            this.ids = asList(id);
            return this;
        }

        public ApplicationCriteria.Builder name(final String name) {
            this.name = name;
            return this;
        }

        public ApplicationCriteria.Builder environmentIds(final List<String> environmentIds) {
            this.environmentIds = environmentIds;
            return this;
        }

        public ApplicationCriteria.Builder status(final ApplicationStatus status) {
            this.status = status;
            return this;
        }

        public ApplicationCriteria build() {
            return new ApplicationCriteria(this);
        }
    }
}
