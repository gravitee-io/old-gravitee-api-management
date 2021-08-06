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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.ApiHeader.AuditEvent.*;
import static io.gravitee.repository.management.model.Audit.AuditProperties.API_HEADER;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiHeaderRepository;
import io.gravitee.repository.management.model.ApiHeader;
import io.gravitee.rest.api.model.api.header.ApiHeaderEntity;
import io.gravitee.rest.api.model.api.header.NewApiHeaderEntity;
import io.gravitee.rest.api.model.api.header.UpdateApiHeaderEntity;
import io.gravitee.rest.api.service.ApiHeaderService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.service.exceptions.ApiHeaderNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiHeaderServiceImpl extends TransactionalService implements ApiHeaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiHeaderServiceImpl.class);

    @Autowired
    private ApiHeaderRepository apiHeaderRepository;

    @Autowired
    AuditService auditService;

    @Override
    public ApiHeaderEntity create(NewApiHeaderEntity newEntity) {
        return this.create(newEntity, GraviteeContext.getCurrentEnvironment());
    }

    private ApiHeaderEntity create(NewApiHeaderEntity newEntity, String environmentId) {
        try {
            int order = apiHeaderRepository.findAllByEnvironment(environmentId).size() + 1;

            ApiHeader apiHeader = new ApiHeader();
            apiHeader.setId(RandomString.generate());
            apiHeader.setEnvironmentId(environmentId);
            apiHeader.setName(newEntity.getName());
            apiHeader.setValue(newEntity.getValue());
            apiHeader.setOrder(order);
            apiHeader.setCreatedAt(new Date());
            apiHeader.setUpdatedAt(apiHeader.getCreatedAt());

            auditService.createPortalAuditLog(
                Collections.singletonMap(API_HEADER, apiHeader.getId()),
                API_HEADER_CREATED,
                apiHeader.getCreatedAt(),
                null,
                apiHeader
            );

            return convert(apiHeaderRepository.create(apiHeader));
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to create a header {}", newEntity, e);
            throw new TechnicalManagementException("An error occurs while trying to create a header " + newEntity, e);
        }
    }

    @Override
    public void delete(String apiHeaderId) {
        try {
            Optional<ApiHeader> optionalApiHeader = apiHeaderRepository.findById(apiHeaderId);
            if (!optionalApiHeader.isPresent()) {
                throw new ApiHeaderNotFoundException(apiHeaderId);
            }

            apiHeaderRepository.delete(apiHeaderId);

            auditService.createPortalAuditLog(
                Collections.singletonMap(API_HEADER, apiHeaderId),
                API_HEADER_DELETED,
                new Date(),
                optionalApiHeader.get(),
                null
            );

            //reorder headers
            int currentOrder = 1;
            for (ApiHeaderEntity apiHeaderEntity : this.findAll()) {
                if (apiHeaderEntity.getOrder() != currentOrder) {
                    UpdateApiHeaderEntity updateEntity = convert(apiHeaderEntity);
                    updateEntity.setOrder(currentOrder);
                    this.update(updateEntity);
                    break;
                }
                currentOrder++;
            }
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to delete a header {}", apiHeaderId, e);
            throw new TechnicalManagementException("An error occurs while trying to delete a header " + apiHeaderId, e);
        }
    }

    @Override
    public ApiHeaderEntity update(UpdateApiHeaderEntity updateEntity) {
        try {
            Optional<ApiHeader> optionalApiHeader = apiHeaderRepository.findById(updateEntity.getId());
            if (!optionalApiHeader.isPresent()) {
                throw new ApiHeaderNotFoundException(updateEntity.getId());
            }
            ApiHeader updatedHeader = new ApiHeader(optionalApiHeader.get());
            Date updatedAt = new Date();
            updatedHeader.setName(updateEntity.getName());
            updatedHeader.setValue(updateEntity.getValue());
            updatedHeader.setUpdatedAt(updatedAt);

            if (updatedHeader.getOrder() != updateEntity.getOrder()) {
                updatedHeader.setOrder(updateEntity.getOrder());
                reorderAndSave(updatedHeader);
                return convert(updatedHeader);
            } else {
                ApiHeader header = apiHeaderRepository.update(updatedHeader);
                auditService.createPortalAuditLog(
                    singletonMap(API_HEADER, header.getId()),
                    API_HEADER_UPDATED,
                    header.getUpdatedAt(),
                    optionalApiHeader.get(),
                    header
                );
                return convert(header);
            }
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to update header {}", updateEntity, e);
            throw new TechnicalManagementException("An error occurs while trying to update header " + updateEntity, e);
        }
    }

    @Override
    public List<ApiHeaderEntity> findAll() {
        try {
            return apiHeaderRepository
                .findAllByEnvironment(GraviteeContext.getCurrentEnvironment())
                .stream()
                .sorted(Comparator.comparingInt(ApiHeader::getOrder))
                .map(this::convert)
                .collect(toList());
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to find all header", e);
            throw new TechnicalManagementException("An error occurs while trying to find all header", e);
        }
    }

    private void reorderAndSave(final ApiHeader headerToReorder) throws TechnicalException {
        ApiHeader[] headers = apiHeaderRepository
            .findAllByEnvironment(GraviteeContext.getCurrentEnvironment())
            .stream()
            .filter(h -> !Objects.equals(h.getId(), headerToReorder.getId()))
            .sorted(Comparator.comparingInt(ApiHeader::getOrder))
            .toArray(ApiHeader[]::new);

        //the new header order must be between 1 and numbers of current headers
        if (headerToReorder.getOrder() < 1) {
            headerToReorder.setOrder(1);
        } else if (headerToReorder.getOrder() > headers.length + 1) { // +1 because we have filtered headers
            headerToReorder.setOrder(headers.length + 1);
        }

        for (int i = 0; i < headers.length; i++) {
            int newOrder = (i + 1) < headerToReorder.getOrder() ? (i + 1) : (i + 2);
            if (headers[i].getOrder() != newOrder) {
                headers[i].setOrder(newOrder);
                apiHeaderRepository.update(headers[i]);
            }
        }
        apiHeaderRepository.update(headerToReorder);
    }

    @Override
    public void initialize(String environmentId) {
        NewApiHeaderEntity h = new NewApiHeaderEntity();

        h.setName("api.version");
        h.setValue("${api.version}");
        this.create(h, environmentId);

        h.setName("api.owner");
        h.setValue("${api.primaryOwner.displayName}");
        this.create(h, environmentId);

        h.setName("api.publishedAt");
        h.setValue("${(api.deployedAt?date)!}");
        this.create(h, environmentId);
    }

    private ApiHeaderEntity convert(ApiHeader apiHeader) {
        ApiHeaderEntity entity = new ApiHeaderEntity();
        entity.setId(apiHeader.getId());
        entity.setName(apiHeader.getName());
        entity.setValue(apiHeader.getValue());
        entity.setOrder(apiHeader.getOrder());
        entity.setCreatedAt(apiHeader.getCreatedAt());
        entity.setUpdatedAt(apiHeader.getUpdatedAt());
        return entity;
    }

    private UpdateApiHeaderEntity convert(ApiHeaderEntity entity) {
        UpdateApiHeaderEntity updateApiHeaderEntity = new UpdateApiHeaderEntity();
        updateApiHeaderEntity.setId(entity.getId());
        updateApiHeaderEntity.setName(entity.getName());
        updateApiHeaderEntity.setOrder(entity.getOrder());
        updateApiHeaderEntity.setValue(entity.getValue());
        return updateApiHeaderEntity;
    }
}
