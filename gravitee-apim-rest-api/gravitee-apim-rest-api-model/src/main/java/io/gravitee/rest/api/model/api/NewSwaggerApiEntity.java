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
package io.gravitee.rest.api.model.api;

import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewSwaggerApiEntity {

    @NotNull
    private String name;

    @NotNull
    private String version;

    @NotNull
    private String description;

    @NotNull
    private String contextPath;

    @NotNull
    private List<String> endpoint;

    private Set<String> groups;
    private List<SwaggerPath> paths;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public List<String> getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(List<String> endpoint) {
        this.endpoint = endpoint;
    }

    public List<SwaggerPath> getPaths() {
        return paths;
    }

    public void setPaths(List<SwaggerPath> paths) {
        this.paths = paths;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    @Override
    public String toString() {
        return (
            "NewSwaggerApiEntity{" +
            "name='" +
            name +
            '\'' +
            ", version='" +
            version +
            '\'' +
            ", description='" +
            description +
            '\'' +
            ", contextPath='" +
            contextPath +
            '\'' +
            ", endpoint='" +
            endpoint +
            '\'' +
            ", groups=" +
            groups +
            ", paths=" +
            paths +
            '}'
        );
    }
}
