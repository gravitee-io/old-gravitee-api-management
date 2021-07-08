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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.Objects;

public class Promotion {

    private String id;
    private String apiDefinition;
    private String apiId;

    private PromotionStatus status;

    private String targetEnvCockpitId;
    private String targetEnvName;

    private String sourceEnvCockpitId;
    private String sourceEnvName;

    private Date createdAt;
    private Date updatedAt;

    private PromotionAuthor author = new PromotionAuthor();
    private String targetApiId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTargetEnvCockpitId() {
        return targetEnvCockpitId;
    }

    public void setTargetEnvCockpitId(String targetEnvCockpitId) {
        this.targetEnvCockpitId = targetEnvCockpitId;
    }

    public String getSourceEnvCockpitId() {
        return sourceEnvCockpitId;
    }

    public void setSourceEnvCockpitId(String sourceEnvCockpitId) {
        this.sourceEnvCockpitId = sourceEnvCockpitId;
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

    public void setApiDefinition(String apiDefinition) {
        this.apiDefinition = apiDefinition;
    }

    public String getApiDefinition() {
        return apiDefinition;
    }

    public void setStatus(PromotionStatus status) {
        this.status = status;
    }

    public PromotionStatus getStatus() {
        return status;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getTargetEnvName() {
        return targetEnvName;
    }

    public void setTargetEnvName(String targetEnvName) {
        this.targetEnvName = targetEnvName;
    }

    public String getSourceEnvName() {
        return sourceEnvName;
    }

    public void setSourceEnvName(String sourceEnvName) {
        this.sourceEnvName = sourceEnvName;
    }

    public PromotionAuthor getAuthor() {
        return author;
    }

    public void setAuthor(PromotionAuthor author) {
        this.author = author;
    }

    /*
     * ⚠️ We define getter/setter for author's properties to be able to map these fields to db columns in JDBC world
     * instead of dealing with another table (or even an associative table 🤯)
     */
    public String getAuthorUserId() {
        return this.author.getUserId();
    }

    public void setAuthorUserId(String userId) {
        this.author.setUserId(userId);
    }

    public String getAuthorDisplayName() {
        return this.author.getDisplayName();
    }

    public void setAuthorDisplayName(String displayName) {
        this.author.setDisplayName(displayName);
    }

    public String getAuthorEmail() {
        return this.author.getEmail();
    }

    public void setAuthorEmail(String email) {
        this.author.setEmail(email);
    }

    public String getAuthorPicture() {
        return this.author.getPicture();
    }

    public void setAuthorPicture(String picture) {
        this.author.setPicture(picture);
    }

    public String getAuthorSource() {
        return this.author.getSource();
    }

    public void setAuthorSource(String source) {
        this.author.setSource(source);
    }

    public String getAuthorSourceId() {
        return this.author.getSourceId();
    }

    public void setAuthorSourceId(String sourceId) {
        this.author.setSourceId(sourceId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Promotion promotion = (Promotion) o;
        return Objects.equals(id, promotion.id) && Objects.equals(apiDefinition, promotion.apiDefinition) && Objects.equals(apiId, promotion.apiId) && status == promotion.status && Objects.equals(targetEnvName, promotion.targetEnvName) && Objects.equals(targetEnvCockpitId, promotion.targetEnvCockpitId) && Objects.equals(sourceEnvName, promotion.sourceEnvName) && Objects.equals(sourceEnvCockpitId, promotion.sourceEnvCockpitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, apiDefinition, apiId, status, targetEnvName, targetEnvCockpitId, sourceEnvName, sourceEnvCockpitId);
    }

    @Override
    public String toString() {
        return "Promotion{" +
            "id='" + id + '\'' +
            ", apiDefinition='" + apiDefinition + '\'' +
            ", apiId='" + apiId + '\'' +
            ", status=" + status +
            ", targetEnvironmentId='" + targetEnvCockpitId + '\'' +
            ", targetEnvironmentName='" + targetEnvName + '\'' +
            ", sourceEnvironmentId='" + sourceEnvCockpitId + '\'' +
            ", sourceEnvironmentName='" + sourceEnvName + '\'' +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            ", author=" + author +
            ", targetApiId=" + targetApiId +
            '}';
    }

    public String getTargetApiId() {
        return targetApiId;
    }

    public void setTargetApiId(String targetApiId) {
        this.targetApiId = targetApiId;
    }
}
