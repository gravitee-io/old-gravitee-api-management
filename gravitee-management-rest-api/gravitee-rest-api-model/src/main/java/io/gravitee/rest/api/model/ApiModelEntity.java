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
package io.gravitee.rest.api.model;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import java.util.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiModelEntity {

    private String id;
    private String name;
    private String version;
    private String description;
    private Set<String> groups;
    private ProxyModelEntity proxy;
    private Map<String, Path> paths = new HashMap<>();
    private Date deployedAt;
    private Date createdAt;
    private Date updatedAt;
    private Visibility visibility;
    private Lifecycle.State state;
    private PrimaryOwnerEntity primaryOwner;
    private Properties properties;
    private Services services;
    private Set<String> tags;
    private String picture;
    private List<Resource> resources = new ArrayList<>();
    private Set<String> categories;
    private Map<String, String> metadata;
    private ApiLifecycleState lifecycleState;
    private boolean disableMembershipNotifications;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
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

    public Lifecycle.State getState() {
        return state;
    }

    public void setState(Lifecycle.State state) {
        this.state = state;
    }

    public ProxyModelEntity getProxy() {
        return proxy;
    }

    public void setProxy(ProxyModelEntity proxy) {
        this.proxy = proxy;
    }

    public Map<String, Path> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, Path> paths) {
        this.paths = paths;
    }

    public PrimaryOwnerEntity getPrimaryOwner() {
        return primaryOwner;
    }

    public void setPrimaryOwner(PrimaryOwnerEntity primaryOwner) {
        this.primaryOwner = primaryOwner;
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Date getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(Date deployedAt) {
        this.deployedAt = deployedAt;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public ApiLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(ApiLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public boolean isDisableMembershipNotifications() {
        return disableMembershipNotifications;
    }

    public void setDisableMembershipNotifications(boolean disableMembershipNotifications) {
        this.disableMembershipNotifications = disableMembershipNotifications;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiModelEntity api = (ApiModelEntity) o;
        return Objects.equals(id, api.id) && Objects.equals(version, api.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    public String toString() {
        return (
            "ApiEntity{" +
            "id='" +
            id +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", version='" +
            version +
            '\'' +
            ", description='" +
            description +
            '\'' +
            ", proxy=" +
            proxy +
            ", paths=" +
            paths +
            ", createdAt=" +
            createdAt +
            ", updatedAt=" +
            updatedAt +
            ", visibility=" +
            visibility +
            ", state=" +
            state +
            ", primaryOwner=" +
            primaryOwner +
            ", tags=" +
            tags +
            ", category=" +
            categories +
            ", groups=" +
            groups +
            ", metadata=" +
            metadata +
            ", lifecycleState=" +
            lifecycleState +
            ", disableMembershipNotifications=" +
            disableMembershipNotifications +
            '}'
        );
    }
}
