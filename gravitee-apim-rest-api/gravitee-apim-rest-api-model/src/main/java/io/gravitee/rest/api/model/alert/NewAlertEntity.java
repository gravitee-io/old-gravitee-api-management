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
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewAlertEntity {

    @NotNull
    private String name;

    private String description;

    @JsonProperty("reference_type")
    private AlertReferenceType referenceType;

    @JsonProperty("reference_id")
    private String referenceId;

    private AlertType type;
    private boolean enabled;
    private MetricType metricType;
    private Metric metric;
    private ThresholdType thresholdType;
    private Double threshold;
    private String plan;

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

    public AlertType getType() {
        return type;
    }

    public void setType(AlertType type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public ThresholdType getThresholdType() {
        return thresholdType;
    }

    public void setThresholdType(ThresholdType thresholdType) {
        this.thresholdType = thresholdType;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NewAlertEntity)) return false;
        NewAlertEntity that = (NewAlertEntity) o;
        return (
            enabled == that.enabled &&
            Objects.equals(name, that.name) &&
            Objects.equals(description, that.description) &&
            referenceType == that.referenceType &&
            Objects.equals(referenceId, that.referenceId) &&
            type == that.type &&
            metricType == that.metricType &&
            Objects.equals(metric, that.metric) &&
            thresholdType == that.thresholdType &&
            Objects.equals(threshold, that.threshold) &&
            Objects.equals(plan, that.plan)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            name,
            description,
            referenceType,
            referenceId,
            type,
            enabled,
            metricType,
            metric,
            thresholdType,
            threshold,
            plan
        );
    }

    @Override
    public String toString() {
        return (
            "NewAlertEntity{" +
            "name='" +
            name +
            '\'' +
            ", description='" +
            description +
            '\'' +
            ", referenceType=" +
            referenceType +
            ", referenceId='" +
            referenceId +
            '\'' +
            ", type=" +
            type +
            ", enabled=" +
            enabled +
            ", metricType=" +
            metricType +
            ", metric=" +
            metric +
            ", thresholdType=" +
            thresholdType +
            ", threshold=" +
            threshold +
            ", plan='" +
            plan +
            '\'' +
            '}'
        );
    }
}
