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
package io.gravitee.rest.api.model.alert;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.rest.api.model.AlertEventRuleEntity;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertTriggerEntity extends Trigger {

    private String description;

    @JsonProperty("reference_type")
    private AlertReferenceType referenceType;

    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    private String type;

    @JsonProperty("last_alert_at")
    private Date lastAlertAt;

    @JsonProperty("last_alert_message")
    private String lastAlertMessage;

    @JsonProperty("counters")
    private Map<String, Integer> counters;

    private boolean template;

    @JsonProperty("event_rules")
    private List<AlertEventRuleEntity> eventRules;

    @JsonProperty("parent_id")
    private String parentId;

    protected AlertTriggerEntity(String id, String name, String source, Severity severity, boolean enabled) {
        super(id, name, severity, source, enabled);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AlertReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(AlertReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getLastAlertAt() {
        return lastAlertAt;
    }

    public void setLastAlertAt(Date lastAlertAt) {
        this.lastAlertAt = lastAlertAt;
    }

    public String getLastAlertMessage() {
        return lastAlertMessage;
    }

    public void setLastAlertMessage(String lastAlertMessage) {
        this.lastAlertMessage = lastAlertMessage;
    }

    public Map<String, Integer> getCounters() {
        return counters;
    }

    public void setCounters(Map<String, Integer> counters) {
        this.counters = counters;
    }

    public boolean isTemplate() {
        return template;
    }

    public void setTemplate(boolean template) {
        this.template = template;
    }

    public List<AlertEventRuleEntity> getEventRules() {
        return eventRules;
    }

    public void setEventRules(List<AlertEventRuleEntity> eventRules) {
        this.eventRules = eventRules;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public String toString() {
        return (
            "AlertTriggerEntity{" +
            "description='" +
            description +
            '\'' +
            ", referenceType=" +
            referenceType +
            ", referenceId='" +
            referenceId +
            '\'' +
            ", createdAt=" +
            createdAt +
            ", updatedAt=" +
            updatedAt +
            '}'
        );
    }
}
