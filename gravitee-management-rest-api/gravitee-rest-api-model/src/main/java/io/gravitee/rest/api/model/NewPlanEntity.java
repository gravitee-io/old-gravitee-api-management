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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewPlanEntity {

    @NotNull
    private String name;

    @NotNull
    private String description;

    @NotNull
    private PlanValidationType validation = PlanValidationType.MANUAL;

    @NotNull
    private PlanSecurityType security = PlanSecurityType.API_KEY;

    private String securityDefinition;

    @NotNull
    private PlanType type = PlanType.API;

    @NotNull
    private PlanStatus status = PlanStatus.STAGING;

    private String api;

    @JsonProperty(value = "paths", required = true)
    private Map<String, Path> paths = new HashMap<>();

    private List<String> characteristics;

    @JsonProperty("excluded_groups")
    private List<String> excludedGroups;

    @JsonProperty("comment_required")
    private boolean commentRequired;

    @JsonProperty("comment_message")
    private String commentMessage;

    private Set<String> tags;

    @JsonProperty("selection_rule")
    private String selectionRule;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PlanValidationType getValidation() {
        return validation;
    }

    public void setValidation(PlanValidationType validation) {
        this.validation = validation;
    }

    public PlanType getType() {
        return type;
    }

    public void setType(PlanType type) {
        this.type = type;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public Map<String, Path> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, Path> paths) {
        this.paths = paths;
    }

    public List<String> getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(List<String> characteristics) {
        this.characteristics = characteristics;
    }

    public PlanSecurityType getSecurity() {
        return security;
    }

    public void setSecurity(PlanSecurityType security) {
        this.security = security;
    }

    public List<String> getExcludedGroups() {
        return excludedGroups;
    }

    public void setExcludedGroups(List<String> excludedGroups) {
        this.excludedGroups = excludedGroups;
    }

    public String getSecurityDefinition() {
        return securityDefinition;
    }

    public void setSecurityDefinition(String securityDefinition) {
        this.securityDefinition = securityDefinition;
    }

    public boolean isCommentRequired() {
        return commentRequired;
    }

    public void setCommentRequired(boolean commentRequired) {
        this.commentRequired = commentRequired;
    }

    public String getCommentMessage() {
        return commentMessage;
    }

    public void setCommentMessage(String commentMessage) {
        this.commentMessage = commentMessage;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getSelectionRule() {
        return selectionRule;
    }

    public void setSelectionRule(String selectionRule) {
        this.selectionRule = selectionRule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NewPlanEntity that = (NewPlanEntity) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
