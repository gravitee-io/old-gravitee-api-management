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

import static io.gravitee.repository.management.model.Audit.AuditProperties.METADATA;
import static io.gravitee.repository.management.model.Metadata.AuditEvent.*;
import static java.util.stream.Collectors.toMap;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.exceptions.*;
import java.util.*;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractReferenceMetadataService {

    private final Logger LOGGER = LoggerFactory.getLogger(io.gravitee.rest.api.service.impl.AbstractReferenceMetadataService.class);

    @Autowired
    private MetadataRepository metadataRepository;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ApplicationService applicationService;

    protected List<ReferenceMetadataEntity> findAllByReference(
        final MetadataReferenceType referenceType,
        final String referenceId,
        final boolean withDefaults
    ) {
        try {
            LOGGER.debug("Find all metadata by reference {} / {}", referenceType, referenceId);

            final List<Metadata> referenceMetadataList = metadataRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId);

            Map<String, ReferenceMetadataEntity> referenceMetadataMap = referenceMetadataList
                .stream()
                .map(this::convert)
                .peek(m -> m.setValue(m.getValue() == null ? "" : m.getValue()))
                .collect(toMap(ReferenceMetadataEntity::getKey, Function.identity()));

            final List<ReferenceMetadataEntity> allMetadata = new ArrayList<>();
            if (withDefaults) {
                final List<MetadataEntity> defaultMetadataList = metadataService.findAllDefault();
                defaultMetadataList.forEach(
                    defaultMetadata -> {
                        ReferenceMetadataEntity referenceMetadataEntity = referenceMetadataMap.get(defaultMetadata.getKey());
                        if (referenceMetadataEntity != null) {
                            //update the reference metadata in the map
                            referenceMetadataEntity.setDefaultValue(defaultMetadata.getValue());
                        } else {
                            final Optional<Metadata> optReferenceMetadata = referenceMetadataList
                                .stream()
                                .filter(referenceMetadata -> defaultMetadata.getKey().equals(referenceMetadata.getKey()))
                                .findAny();
                            allMetadata.add(convert(optReferenceMetadata, defaultMetadata));
                        }
                    }
                );
            }
            //add all reference metadata
            allMetadata.addAll(referenceMetadataMap.values());

            return allMetadata;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to find all metadata by REFERENCE", ex);
            throw new TechnicalManagementException("An error occurred while trying to find all metadata by REFERENCE", ex);
        }
    }

    protected ReferenceMetadataEntity findByIdAndReference(
        final String metadataId,
        final MetadataReferenceType referenceType,
        final String referenceId,
        final boolean withDefaults
    ) {
        LOGGER.debug("Find metadata by id {} and reference {} / {}", metadataId, referenceType, referenceId);
        final List<ReferenceMetadataEntity> allMetadata = findAllByReference(referenceType, referenceId, withDefaults);
        final Optional<ReferenceMetadataEntity> optMetadata = allMetadata.stream().filter(m -> metadataId.equals(m.getKey())).findAny();
        if (optMetadata.isPresent()) {
            final ReferenceMetadataEntity metadata = optMetadata.get();
            if (metadata.getValue() == null) {
                metadata.setValue(metadata.getDefaultValue());
            }
            return metadata;
        } else {
            if (referenceType.equals(MetadataReferenceType.APPLICATION)) {
                throw new ApplicationMetadataNotFoundException(referenceId, metadataId);
            } else {
                throw new ApiMetadataNotFoundException(referenceId, metadataId);
            }
        }
    }

    protected void delete(final String metadataId, final MetadataReferenceType referenceType, final String referenceId) {
        LOGGER.debug("Delete metadata by id {} and reference {} / {}", metadataId, referenceType, referenceId);
        try {
            // prevent deletion of a metadata not in the given reference
            final Optional<Metadata> optMetadata = metadataRepository.findById(metadataId, referenceId, referenceType);
            if (optMetadata.isPresent()) {
                metadataRepository.delete(metadataId, referenceId, referenceType);
                // Audit
                createReferenceAuditLog(referenceType, referenceId, optMetadata.get(), null, METADATA_DELETED);
            } else {
                if (referenceType.equals(MetadataReferenceType.APPLICATION)) {
                    throw new ApplicationMetadataNotFoundException(referenceId, metadataId);
                } else if (referenceType.equals(MetadataReferenceType.API)) {
                    throw new ApiMetadataNotFoundException(referenceId, metadataId);
                } else {
                    throw new MetadataNotFoundException(metadataId);
                }
            }
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to delete metadata " + metadataId;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    protected ReferenceMetadataEntity create(
        final NewReferenceMetadataEntity metadataEntity,
        final MetadataReferenceType referenceType,
        final String referenceId,
        final boolean withDefaults
    ) {
        // if no format defined, we just set String format
        if (metadataEntity.getFormat() == null) {
            metadataEntity.setFormat(MetadataFormat.STRING);
        }
        checkReferenceMetadataFormat(metadataEntity.getFormat(), metadataEntity.getValue(), referenceType, referenceId);
        // First we prevent the duplicate metadata name
        final Optional<ReferenceMetadataEntity> optionalMetadata = findAllByReference(referenceType, referenceId, withDefaults)
            .stream()
            .filter(metadata -> metadataEntity.getName().equalsIgnoreCase(metadata.getName()))
            .findAny();

        if (optionalMetadata.isPresent()) {
            throw new DuplicateMetadataNameException(optionalMetadata.get().getName());
        }

        try {
            final Metadata metadata = convertForReference(metadataEntity, referenceType, referenceId);
            final Date now = new Date();
            metadata.setCreatedAt(now);
            metadata.setUpdatedAt(now);
            metadataRepository.create(metadata);
            // Audit
            createReferenceAuditLog(referenceType, referenceId, null, metadata, METADATA_CREATED);
            return convert(metadata);
        } catch (TechnicalException ex) {
            final String message =
                "An error occurred while trying to create metadata " + metadataEntity.getName() + " on reference " + referenceId;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    private void checkReferenceMetadataFormat(
        MetadataFormat format,
        String value,
        MetadataReferenceType referenceType,
        String referenceId
    ) {
        switch (referenceType) {
            case API:
                final ApiEntity apiEntity = apiService.findById(referenceId);
                metadataService.checkMetadataFormat(format, value, referenceType, apiEntity);
                break;
            case APPLICATION:
                final ApplicationEntity applicationEntity = applicationService.findById(referenceId);
                metadataService.checkMetadataFormat(format, value, referenceType, applicationEntity);
                break;
        }
    }

    private void createReferenceAuditLog(
        MetadataReferenceType referenceType,
        String referenceId,
        Metadata oldMetadata,
        Metadata metadata,
        Metadata.AuditEvent auditEvent
    ) {
        final String key = metadata == null ? oldMetadata.getKey() : metadata.getKey();
        final Date updatedAt = metadata == null ? oldMetadata.getUpdatedAt() : metadata.getUpdatedAt();
        switch (referenceType) {
            case API:
                auditService.createApiAuditLog(
                    referenceId,
                    Collections.singletonMap(METADATA, key),
                    auditEvent,
                    updatedAt,
                    oldMetadata,
                    metadata
                );
                break;
            case APPLICATION:
                auditService.createApplicationAuditLog(
                    referenceId,
                    Collections.singletonMap(METADATA, key),
                    auditEvent,
                    updatedAt,
                    oldMetadata,
                    metadata
                );
                break;
        }
    }

    protected ReferenceMetadataEntity update(
        final UpdateReferenceMetadataEntity metadataEntity,
        final MetadataReferenceType referenceType,
        final String referenceId,
        final boolean withDefaults
    ) {
        checkReferenceMetadataFormat(metadataEntity.getFormat(), metadataEntity.getValue(), referenceType, referenceId);
        try {
            final Optional<Metadata> referenceMetadata = metadataRepository.findById(metadataEntity.getKey(), referenceId, referenceType);

            final Metadata savedMetadata;
            final Metadata metadata = convertForReference(metadataEntity, referenceType, referenceId);
            final Date now = new Date();
            if (referenceMetadata.isPresent()) {
                metadata.setUpdatedAt(now);
                savedMetadata = metadataRepository.update(metadata);
                // Audit
                createReferenceAuditLog(referenceType, referenceId, referenceMetadata.get(), metadata, METADATA_UPDATED);
            } else {
                metadata.setCreatedAt(now);
                metadata.setUpdatedAt(now);
                savedMetadata = metadataRepository.create(metadata);
                // Audit
                createReferenceAuditLog(referenceType, referenceId, null, metadata, METADATA_CREATED);
            }
            final ReferenceMetadataEntity referenceMetadataEntity = convert(savedMetadata);
            if (withDefaults) {
                final List<MetadataEntity> defaultMetatata = metadataService.findAllDefault();
                final Optional<MetadataEntity> optDefaultMetadata = defaultMetatata
                    .stream()
                    .filter(m -> m.getKey().equals(metadataEntity.getKey()))
                    .findAny();
                optDefaultMetadata.ifPresent(defaultMetadata -> referenceMetadataEntity.setDefaultValue(defaultMetadata.getValue()));
            }
            return referenceMetadataEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to update metadata {} on REFERENCE {}", metadataEntity.getName(), referenceId, ex);
            throw new TechnicalManagementException(
                "An error occurred while trying to update metadata " + metadataEntity.getName() + " on REFERENCE " + referenceId,
                ex
            );
        }
    }

    private ReferenceMetadataEntity convert(final Optional<Metadata> optMetadata, final MetadataEntity defaultMetadata) {
        final Metadata metadata;
        if (optMetadata.isPresent()) {
            metadata = optMetadata.get();
        } else {
            metadata = convert(defaultMetadata);
            metadata.setValue(null);
        }
        final ReferenceMetadataEntity referenceMetadataEntity = convert(metadata);

        referenceMetadataEntity.setDefaultValue(defaultMetadata.getValue());

        return referenceMetadataEntity;
    }

    private ReferenceMetadataEntity convert(final Metadata metadata) {
        final ReferenceMetadataEntity referenceMetadataEntity = new ReferenceMetadataEntity();
        referenceMetadataEntity.setValue(metadata.getValue());
        referenceMetadataEntity.setKey(metadata.getKey());
        referenceMetadataEntity.setName(metadata.getName());
        referenceMetadataEntity.setFormat(MetadataFormat.valueOf(metadata.getFormat().name()));
        return referenceMetadataEntity;
    }

    private Metadata convert(final MetadataEntity metadataEntity) {
        final Metadata metadata = new Metadata();
        metadata.setKey(metadataEntity.getKey());
        metadata.setName(metadataEntity.getName());
        metadata.setFormat(io.gravitee.repository.management.model.MetadataFormat.valueOf(metadataEntity.getFormat().name()));

        if (metadataEntity.getValue() != null) {
            if (MetadataFormat.DATE.equals(metadataEntity.getFormat())) {
                metadata.setValue(metadataEntity.getValue().substring(0, 10));
            } else {
                metadata.setValue(metadataEntity.getValue());
            }
        }
        return metadata;
    }

    private Metadata convertForReference(
        final NewReferenceMetadataEntity metadataEntity,
        final MetadataReferenceType referenceType,
        final String referenceId
    ) {
        final Metadata metadata = new Metadata();
        metadata.setKey(IdGenerator.generate(metadataEntity.getName()));
        metadata.setName(metadataEntity.getName());
        metadata.setFormat(io.gravitee.repository.management.model.MetadataFormat.valueOf(metadataEntity.getFormat().name()));

        if (metadataEntity.getValue() != null) {
            if (MetadataFormat.DATE.equals(metadataEntity.getFormat())) {
                metadata.setValue(metadataEntity.getValue().substring(0, 10));
            } else {
                metadata.setValue(metadataEntity.getValue());
            }
        }

        metadata.setReferenceId(referenceId);
        metadata.setReferenceType(referenceType);

        return metadata;
    }

    private Metadata convertForReference(
        final UpdateReferenceMetadataEntity metadataEntity,
        final MetadataReferenceType referenceType,
        final String referenceId
    ) {
        final Metadata metadata = new Metadata();
        metadata.setKey(metadataEntity.getKey());
        metadata.setName(metadataEntity.getName());
        metadata.setFormat(io.gravitee.repository.management.model.MetadataFormat.valueOf(metadataEntity.getFormat().name()));

        if (metadataEntity.getValue() != null) {
            if (MetadataFormat.DATE.equals(metadataEntity.getFormat())) {
                metadata.setValue(metadataEntity.getValue().substring(0, 10));
            } else {
                metadata.setValue(metadataEntity.getValue());
            }
        }

        metadata.setReferenceId(referenceId);
        metadata.setReferenceType(referenceType);

        return metadata;
    }
}
