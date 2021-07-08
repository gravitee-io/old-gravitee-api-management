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

import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Tenant {

    public enum AuditEvent implements Audit.AuditEvent {
        TENANT_CREATED,
        TENANT_UPDATED,
        TENANT_DELETED,
    }

    private String id;

    private String name;

    private String description;

    private String referenceId;

    private TenantReferenceType referenceType;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public TenantReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(TenantReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tenant)) return false;
        Tenant view = (Tenant) o;
        return (
            Objects.equals(id, view.id) &&
            Objects.equals(name, view.name) &&
            Objects.equals(description, view.description) &&
            Objects.equals(referenceId, view.referenceId) &&
            Objects.equals(referenceType, view.referenceType)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, referenceId, referenceType);
    }

    @Override
    public String toString() {
        return (
            "Tenant{" +
            "id='" +
            id +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", description='" +
            description +
            '\'' +
            ", referenceId='" +
            referenceId +
            '\'' +
            ", referenceType='" +
            referenceType +
            '\'' +
            '}'
        );
    }
}
