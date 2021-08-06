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
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupEntity {

    private String id;
    private String name;

    @JsonProperty("event_rules")
    private List<GroupEventRuleEntity> eventRules;

    boolean manageable;
    private Map<RoleScope, String> roles;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    @JsonProperty("max_invitation")
    private Integer maxInvitation;

    @JsonProperty("lock_api_role")
    private boolean lockApiRole;

    @JsonProperty("lock_application_role")
    private boolean lockApplicationRole;

    @JsonProperty("system_invitation")
    private boolean systemInvitation;

    @JsonProperty("email_invitation")
    private boolean emailInvitation;

    @JsonProperty("disable_membership_notifications")
    private boolean disableMembershipNotifications;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GroupEventRuleEntity> getEventRules() {
        return eventRules;
    }

    public void setEventRules(List<GroupEventRuleEntity> eventRules) {
        this.eventRules = eventRules;
    }

    public boolean isManageable() {
        return manageable;
    }

    public void setManageable(boolean manageable) {
        this.manageable = manageable;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<RoleScope, String> getRoles() {
        return roles;
    }

    public void setRoles(Map<RoleScope, String> roles) {
        this.roles = roles;
    }

    public Integer getMaxInvitation() {
        return maxInvitation;
    }

    public void setMaxInvitation(Integer maxInvitation) {
        this.maxInvitation = maxInvitation;
    }

    public boolean isLockApiRole() {
        return lockApiRole;
    }

    public void setLockApiRole(boolean lockApiRole) {
        this.lockApiRole = lockApiRole;
    }

    public boolean isLockApplicationRole() {
        return lockApplicationRole;
    }

    public void setLockApplicationRole(boolean lockApplicationRole) {
        this.lockApplicationRole = lockApplicationRole;
    }

    public boolean isSystemInvitation() {
        return systemInvitation;
    }

    public void setSystemInvitation(boolean systemInvitation) {
        this.systemInvitation = systemInvitation;
    }

    public boolean isEmailInvitation() {
        return emailInvitation;
    }

    public void setEmailInvitation(boolean emailInvitation) {
        this.emailInvitation = emailInvitation;
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
        GroupEntity group = (GroupEntity) o;
        return Objects.equals(id, group.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "GroupEntity{" +
            "id='" +
            id +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", eventRules=" +
            eventRules +
            ", manageable=" +
            manageable +
            ", roles=" +
            roles +
            ", createdAt=" +
            createdAt +
            ", updatedAt=" +
            updatedAt +
            ", maxInvitation=" +
            maxInvitation +
            ", lockApiRole=" +
            lockApiRole +
            ", lockApplicationRole=" +
            lockApplicationRole +
            ", systemInvitation=" +
            systemInvitation +
            ", emailInvitation=" +
            emailInvitation +
            ", disableMembershipNotifications=" +
            disableMembershipNotifications +
            '}'
        );
    }
}
