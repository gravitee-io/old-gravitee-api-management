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
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UpdateInvitationEntity {

    private String id;

    @NotNull
    @JsonProperty("reference_type")
    private InvitationReferenceType referenceType;

    @NotNull
    @JsonProperty("reference_id")
    private String referenceId;

    @NotNull
    private String email;

    @JsonProperty("api_role")
    private String apiRole;

    @JsonProperty("application_role")
    private String applicationRole;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public InvitationReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(InvitationReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getApiRole() {
        return apiRole;
    }

    public void setApiRole(String apiRole) {
        this.apiRole = apiRole;
    }

    public String getApplicationRole() {
        return applicationRole;
    }

    public void setApplicationRole(String applicationRole) {
        this.applicationRole = applicationRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateInvitationEntity)) return false;
        UpdateInvitationEntity that = (UpdateInvitationEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "UpdateInvitationEntity{" +
            "id='" +
            id +
            '\'' +
            ", referenceType=" +
            referenceType +
            ", referenceId='" +
            referenceId +
            '\'' +
            ", email='" +
            email +
            '\'' +
            ", apiRole='" +
            apiRole +
            '\'' +
            ", applicationRole='" +
            applicationRole +
            '\'' +
            '}'
        );
    }
}
