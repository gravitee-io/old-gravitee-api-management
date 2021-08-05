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

import static io.gravitee.rest.api.service.impl.MetadataServiceImpl.getDefaultReferenceId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.*;
import io.gravitee.repository.management.api.search.AuditCriteria.Builder;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.audit.AuditEntity;
import io.gravitee.rest.api.model.audit.AuditQuery;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AuditServiceImpl extends AbstractService implements AuditService {

    private final Logger LOGGER = LoggerFactory.getLogger(AuditServiceImpl.class);

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private MetadataRepository metadataRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    @Lazy
    private UserService userService;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public MetadataPage<AuditEntity> search(AuditQuery query) {
        Builder criteria = new Builder().from(query.getFrom()).to(query.getTo());

        if (query.isCurrentEnvironmentLogsOnly()) {
            criteria.references(Audit.AuditReferenceType.ENVIRONMENT, Collections.singletonList(GraviteeContext.getCurrentEnvironment()));
        } else if (query.isCurrentOrganizationLogsOnly()) {
            criteria.references(Audit.AuditReferenceType.ORGANIZATION, Collections.singletonList(GraviteeContext.getCurrentOrganization()));
        } else if (query.getApiIds() != null && !query.getApiIds().isEmpty()) {
            criteria.references(Audit.AuditReferenceType.API, query.getApiIds());
        } else if (query.getApplicationIds() != null && !query.getApplicationIds().isEmpty()) {
            criteria.references(Audit.AuditReferenceType.APPLICATION, query.getApplicationIds());
        }

        if (query.getEvents() != null && !query.getEvents().isEmpty()) {
            criteria.events(query.getEvents());
        }

        Page<Audit> auditPage = auditRepository.search(
            criteria.build(),
            new PageableBuilder().pageNumber(query.getPage() - 1).pageSize(query.getSize()).build()
        );

        List<AuditEntity> content = auditPage.getContent().stream().map(this::convert).collect(Collectors.toList());

        return new MetadataPage<>(content, query.getPage(), query.getSize(), auditPage.getTotalElements(), getMetadata(content));
    }

    private Map<String, String> getMetadata(List<AuditEntity> content) {
        Map<String, String> metadata = new HashMap<>();
        for (AuditEntity auditEntity : content) {
            //add user's display name
            String metadataKey = "USER:" + auditEntity.getUser() + ":name";
            try {
                UserEntity user = userService.findById(auditEntity.getUser());
                metadata.put(metadataKey, user.getDisplayName());
            } catch (TechnicalManagementException e) {
                LOGGER.error("Error finding metadata {}", auditEntity.getUser());
            } catch (UserNotFoundException unfe) {
                metadata.put(metadataKey, auditEntity.getUser());
            }

            if (Audit.AuditReferenceType.API.name().equals(auditEntity.getReferenceType())) {
                metadataKey = "API:" + auditEntity.getReferenceId() + ":name";
                if (!metadata.containsKey(metadataKey)) {
                    try {
                        Optional<Api> optApi = apiRepository.findById(auditEntity.getReferenceId());
                        if (optApi.isPresent()) {
                            metadata.put(metadataKey, optApi.get().getName());
                        }
                    } catch (TechnicalException e) {
                        LOGGER.error("Error finding metadata {}", metadataKey);
                        metadata.put(metadataKey, auditEntity.getReferenceId());
                    }
                }
            } else if (Audit.AuditReferenceType.APPLICATION.name().equals(auditEntity.getReferenceType())) {
                metadataKey = "APPLICATION:" + auditEntity.getReferenceId() + ":name";
                if (!metadata.containsKey(metadataKey)) {
                    try {
                        Optional<Application> optApp = applicationRepository.findById(auditEntity.getReferenceId());
                        if (optApp.isPresent()) {
                            metadata.put(metadataKey, optApp.get().getName());
                        }
                    } catch (TechnicalException e) {
                        LOGGER.error("Error finding metadata {}", metadataKey);
                        metadata.put(metadataKey, auditEntity.getReferenceId());
                    }
                }
            }

            //add property metadata
            String name;
            if (auditEntity.getProperties() != null) {
                for (Map.Entry<String, String> property : auditEntity.getProperties().entrySet()) {
                    metadataKey = new StringJoiner(":").add(property.getKey()).add(property.getValue()).add("name").toString();
                    if (!metadata.containsKey(metadataKey)) {
                        name = property.getValue();
                        try {
                            switch (Audit.AuditProperties.valueOf(property.getKey())) {
                                case API:
                                    Optional<Api> optApi = apiRepository.findById(property.getValue());
                                    if (optApi.isPresent()) {
                                        name = optApi.get().getName();
                                    }
                                    break;
                                case APPLICATION:
                                    Optional<Application> optApp = applicationRepository.findById(property.getValue());
                                    if (optApp.isPresent()) {
                                        name = optApp.get().getName();
                                    }
                                    break;
                                case PAGE:
                                    Optional<io.gravitee.repository.management.model.Page> optPage = pageRepository.findById(
                                        property.getValue()
                                    );
                                    if (optPage.isPresent()) {
                                        name = optPage.get().getName();
                                    }
                                    break;
                                case PLAN:
                                    Optional<Plan> optPlan = planRepository.findById(property.getValue());
                                    if (optPlan.isPresent()) {
                                        name = optPlan.get().getName();
                                    }
                                    break;
                                case METADATA:
                                    MetadataReferenceType refType = (
                                            Audit.AuditReferenceType.API.name().equals(auditEntity.getReferenceType())
                                        )
                                        ? MetadataReferenceType.API
                                        : (Audit.AuditReferenceType.APPLICATION.name().equals(auditEntity.getReferenceType()))
                                            ? MetadataReferenceType.APPLICATION
                                            : MetadataReferenceType.DEFAULT;
                                    String refId = refType.equals(MetadataReferenceType.DEFAULT)
                                        ? getDefaultReferenceId()
                                        : auditEntity.getReferenceId();

                                    Optional<Metadata> optMetadata = metadataRepository.findById(property.getValue(), refId, refType);
                                    if (optMetadata.isPresent()) {
                                        name = optMetadata.get().getName();
                                    }
                                    break;
                                case GROUP:
                                    Optional<Group> optGroup = groupRepository.findById(property.getValue());
                                    if (optGroup.isPresent()) {
                                        name = optGroup.get().getName();
                                    }
                                    break;
                                case USER:
                                    try {
                                        UserEntity user = userService.findById(property.getValue());
                                        name = user.getDisplayName();
                                    } catch (UserNotFoundException unfe) {
                                        name = property.getValue();
                                    }
                                default:
                                    break;
                            }
                        } catch (TechnicalException e) {
                            LOGGER.error("Error finding metadata {}", metadataKey);
                            name = property.getValue();
                        }
                        metadata.put(metadataKey, name);
                    }
                }
            }
        }
        return metadata;
    }

    @Override
    public void createApiAuditLog(
        String apiId,
        Map<Audit.AuditProperties, String> properties,
        Audit.AuditEvent event,
        Date createdAt,
        Object oldValue,
        Object newValue
    ) {
        createAuditLog(Audit.AuditReferenceType.API, apiId, properties, event, createdAt, oldValue, newValue);
    }

    @Override
    public void createApplicationAuditLog(
        String applicationId,
        Map<Audit.AuditProperties, String> properties,
        Audit.AuditEvent event,
        Date createdAt,
        Object oldValue,
        Object newValue
    ) {
        createAuditLog(Audit.AuditReferenceType.APPLICATION, applicationId, properties, event, createdAt, oldValue, newValue);
    }

    @Override
    public void createEnvironmentAuditLog(
        Map<Audit.AuditProperties, String> properties,
        Audit.AuditEvent event,
        Date createdAt,
        Object oldValue,
        Object newValue
    ) {
        createAuditLog(
            Audit.AuditReferenceType.ENVIRONMENT,
            GraviteeContext.getCurrentEnvironment(),
            properties,
            event,
            createdAt,
            oldValue,
            newValue
        );
    }

    @Override
    public void createOrganizationAuditLog(
        Map<Audit.AuditProperties, String> properties,
        Audit.AuditEvent event,
        Date createdAt,
        Object oldValue,
        Object newValue
    ) {
        createAuditLog(
            Audit.AuditReferenceType.ORGANIZATION,
            GraviteeContext.getCurrentOrganization(),
            properties,
            event,
            createdAt,
            oldValue,
            newValue
        );
    }

    @Async
    @Override
    public void createAuditLog(
        Audit.AuditReferenceType referenceType,
        String referenceId,
        Map<Audit.AuditProperties, String> properties,
        Audit.AuditEvent event,
        Date createdAt,
        Object oldValue,
        Object newValue
    ) {
        Audit audit = new Audit();
        audit.setId(UuidString.generateRandom());
        audit.setCreatedAt(createdAt == null ? new Date() : createdAt);

        final UserDetails authenticatedUser = getAuthenticatedUser();
        final String user;
        if (authenticatedUser != null && "token".equals(authenticatedUser.getSource())) {
            user =
                userService.findById(authenticatedUser.getUsername()).getDisplayName() +
                " - (using token \"" +
                authenticatedUser.getSourceId() +
                "\")";
        } else {
            user = getAuthenticatedUsernameOrSystem();
        }
        audit.setUser(user);

        if (properties != null) {
            Map<String, String> stringStringMap = new HashMap<>(properties.size());
            properties.forEach((auditProperties, s) -> stringStringMap.put(auditProperties.name(), s));
            audit.setProperties(stringStringMap);
        }

        audit.setReferenceType(referenceType);
        audit.setReferenceId(referenceId);
        audit.setEvent(event.name());

        ObjectNode oldNode = oldValue == null
            ? mapper.createObjectNode()
            : mapper.convertValue(oldValue, ObjectNode.class).remove(Arrays.asList("updatedAt", "createdAt"));
        ObjectNode newNode = newValue == null
            ? mapper.createObjectNode()
            : mapper.convertValue(newValue, ObjectNode.class).remove(Arrays.asList("updatedAt", "createdAt"));

        audit.setPatch(JsonDiff.asJson(oldNode, newNode).toString());

        try {
            auditRepository.create(audit);
        } catch (TechnicalException e) {
            LOGGER.error("Error occurs during the creation of an Audit Log {}.", e);
        }
    }

    private AuditEntity convert(Audit audit) {
        AuditEntity auditEntity = new AuditEntity();

        auditEntity.setReferenceType(audit.getReferenceType().name());
        auditEntity.setReferenceId(audit.getReferenceId());
        auditEntity.setEvent(audit.getEvent());
        auditEntity.setProperties(audit.getProperties());
        auditEntity.setUser(audit.getUser());
        auditEntity.setId(audit.getId());
        auditEntity.setPatch(audit.getPatch());
        auditEntity.setCreatedAt(audit.getCreatedAt());

        return auditEntity;
    }

    private String getAuthenticatedUsernameOrSystem() {
        return isAuthenticated() ? getAuthenticatedUsername() : "system";
    }
}
