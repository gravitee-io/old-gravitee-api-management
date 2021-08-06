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
package io.gravitee.rest.api.service.impl.configuration.identity;

import static io.gravitee.repository.management.model.Audit.AuditProperties.IDENTITY_PROVIDER;
import static java.util.Collections.singletonMap;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IdentityProviderRepository;
import io.gravitee.repository.management.model.IdentityProvider;
import io.gravitee.repository.management.model.IdentityProviderReferenceType;
import io.gravitee.repository.management.model.IdentityProviderType;
import io.gravitee.rest.api.model.configuration.identity.*;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class IdentityProviderServiceImpl extends AbstractService implements IdentityProviderService {

    private final Logger LOGGER = LoggerFactory.getLogger(IdentityProviderServiceImpl.class);

    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";

    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private RoleService roleService;

    @Override
    public IdentityProviderEntity create(NewIdentityProviderEntity newIdentityProviderEntity) {
        try {
            LOGGER.debug("Create identity provider {}", newIdentityProviderEntity);

            Optional<IdentityProvider> optIdentityProvider = identityProviderRepository.findById(
                IdGenerator.generate(newIdentityProviderEntity.getName())
            );
            if (optIdentityProvider.isPresent()) {
                throw new IdentityProviderAlreadyExistsException(newIdentityProviderEntity.getName());
            }

            IdentityProvider identityProvider = convert(newIdentityProviderEntity);
            identityProvider.setReferenceId(GraviteeContext.getCurrentEnvironment());
            identityProvider.setReferenceType(IdentityProviderReferenceType.ENVIRONMENT);

            // If provider is a social type, we must ensure required parameters
            if (identityProvider.getType() == IdentityProviderType.GOOGLE || identityProvider.getType() == IdentityProviderType.GITHUB) {
                checkSocialProvider(identityProvider);
            }

            // Set date fields
            identityProvider.setCreatedAt(new Date());
            identityProvider.setUpdatedAt(identityProvider.getCreatedAt());

            IdentityProvider createdIdentityProvider = identityProviderRepository.create(identityProvider);

            auditService.createPortalAuditLog(
                singletonMap(IDENTITY_PROVIDER, createdIdentityProvider.getId()),
                IdentityProvider.AuditEvent.IDENTITY_PROVIDER_CREATED,
                createdIdentityProvider.getUpdatedAt(),
                null,
                createdIdentityProvider
            );

            return convert(createdIdentityProvider);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create identity provider {}", newIdentityProviderEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying to create " + newIdentityProviderEntity, ex);
        }
    }

    @Override
    public IdentityProviderEntity update(String id, UpdateIdentityProviderEntity updateIdentityProvider) {
        try {
            LOGGER.debug("Update identity provider {}", updateIdentityProvider);

            Optional<IdentityProvider> optIdentityProvider = identityProviderRepository.findById(id);

            if (!optIdentityProvider.isPresent()) {
                throw new IdentityProviderNotFoundException(updateIdentityProvider.getName());
            }

            //TODO: Find a way to validate mapping expression
            IdentityProvider identityProvider = convert(updateIdentityProvider);

            final IdentityProvider idpToUpdate = optIdentityProvider.get();
            identityProvider.setId(id);
            identityProvider.setType(idpToUpdate.getType());
            identityProvider.setCreatedAt(idpToUpdate.getCreatedAt());
            identityProvider.setUpdatedAt(new Date());
            identityProvider.setReferenceId(optIdentityProvider.get().getReferenceId());
            identityProvider.setReferenceType(optIdentityProvider.get().getReferenceType());
            IdentityProvider updatedIdentityProvider = identityProviderRepository.update(identityProvider);

            // Audit
            auditService.createPortalAuditLog(
                singletonMap(IDENTITY_PROVIDER, id),
                IdentityProvider.AuditEvent.IDENTITY_PROVIDER_UPDATED,
                identityProvider.getUpdatedAt(),
                idpToUpdate,
                updatedIdentityProvider
            );

            return convert(updatedIdentityProvider);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update identity provider {}", updateIdentityProvider, ex);
            throw new TechnicalManagementException("An error occurs while trying to update " + updateIdentityProvider, ex);
        }
    }

    @Override
    public IdentityProviderEntity findById(String id) {
        try {
            LOGGER.debug("Find identity provider by ID: {}", id);

            Optional<IdentityProvider> identityProvider = identityProviderRepository.findById(id);

            if (identityProvider.isPresent()) {
                return convert(identityProvider.get());
            }

            throw new IdentityProviderNotFoundException(id);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an identity provider using its ID {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete an identity provider using its ID " + id, ex);
        }
    }

    @Override
    public void delete(String id) {
        try {
            LOGGER.debug("Delete identity provider: {}", id);

            Optional<IdentityProvider> identityProvider = identityProviderRepository.findById(id);

            if (!identityProvider.isPresent()) {
                throw new IdentityProviderNotFoundException(id);
            }

            identityProviderRepository.delete(id);

            auditService.createPortalAuditLog(
                Collections.singletonMap(IDENTITY_PROVIDER, id),
                IdentityProvider.AuditEvent.IDENTITY_PROVIDER_DELETED,
                new Date(),
                identityProvider.get(),
                null
            );
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete an identity provider using its ID {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete an identity provider using its ID " + id, ex);
        }
    }

    private void checkSocialProvider(IdentityProvider identityProvider) {
        // For social provider, we need at least a clientId and a clientSecret
        Map<String, Object> providerConfiguration = identityProvider.getConfiguration();

        String clientId = (String) providerConfiguration.get(CLIENT_ID);
        if (clientId == null || clientId.isEmpty()) {
            throw new ClientIdRequiredException(identityProvider.getName());
        }

        String clientSecret = (String) providerConfiguration.get(CLIENT_SECRET);
        if (clientSecret == null || clientSecret.isEmpty()) {
            throw new ClientSecretRequiredException(identityProvider.getName());
        }
    }

    @Override
    public Set<IdentityProviderEntity> findAll() {
        try {
            return identityProviderRepository
                .findAllByReferenceIdAndReferenceType(GraviteeContext.getCurrentEnvironment(), IdentityProviderReferenceType.ENVIRONMENT)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to retrieve identity providers", ex);
            throw new TechnicalManagementException("An error occurs while trying to retrieve identity providers", ex);
        }
    }

    private IdentityProvider convert(NewIdentityProviderEntity newIdentityProviderEntity) {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setId(IdGenerator.generate(newIdentityProviderEntity.getName()));
        identityProvider.setName(newIdentityProviderEntity.getName());
        identityProvider.setDescription(newIdentityProviderEntity.getDescription());
        identityProvider.setConfiguration(newIdentityProviderEntity.getConfiguration());
        identityProvider.setType(IdentityProviderType.valueOf(newIdentityProviderEntity.getType().name().toUpperCase()));
        identityProvider.setEnabled(newIdentityProviderEntity.isEnabled());
        identityProvider.setUserProfileMapping(newIdentityProviderEntity.getUserProfileMapping());
        identityProvider.setEmailRequired(newIdentityProviderEntity.isEmailRequired());
        identityProvider.setSyncMappings(newIdentityProviderEntity.isSyncMappings());

        return identityProvider;
    }

    private IdentityProviderEntity convert(IdentityProvider identityProvider) {
        IdentityProviderEntity identityProviderEntity = new IdentityProviderEntity();

        identityProviderEntity.setId(identityProvider.getId());
        identityProviderEntity.setName(identityProvider.getName());
        identityProviderEntity.setDescription(identityProvider.getDescription());
        identityProviderEntity.setEnabled(identityProvider.isEnabled());
        identityProviderEntity.setType(
            io.gravitee.rest.api.model.configuration.identity.IdentityProviderType.valueOf(identityProvider.getType().name().toUpperCase())
        );
        identityProviderEntity.setConfiguration(identityProvider.getConfiguration());

        if (identityProvider.getGroupMappings() != null && !identityProvider.getGroupMappings().isEmpty()) {
            identityProviderEntity.setGroupMappings(
                identityProvider
                    .getGroupMappings()
                    .entrySet()
                    .stream()
                    .map(
                        entry -> {
                            GroupMappingEntity groupMapping = new GroupMappingEntity();
                            groupMapping.setCondition(entry.getKey());
                            if (entry.getValue() != null) {
                                groupMapping.setGroups(Arrays.asList(entry.getValue()));
                            }
                            return groupMapping;
                        }
                    )
                    .collect(Collectors.toList())
            );
        }

        if (identityProvider.getRoleMappings() != null && !identityProvider.getRoleMappings().isEmpty()) {
            identityProviderEntity.setRoleMappings(
                identityProvider
                    .getRoleMappings()
                    .entrySet()
                    .stream()
                    .map(
                        new Function<Map.Entry<String, String[]>, RoleMappingEntity>() {
                            @Override
                            public RoleMappingEntity apply(Map.Entry<String, String[]> entry) {
                                RoleMappingEntity roleMapping = new RoleMappingEntity();
                                roleMapping.setCondition(entry.getKey());
                                if (entry.getValue() != null) {
                                    List<String> organizationsRoles = new ArrayList<>();
                                    List<String> environmentsRoles = new ArrayList<>();

                                    for (String role : entry.getValue()) {
                                        if (role.startsWith(io.gravitee.repository.management.model.RoleScope.ORGANIZATION.name() + ":")) {
                                            organizationsRoles.add(role.split(":")[1]);
                                        }
                                        if (role.startsWith(io.gravitee.repository.management.model.RoleScope.ENVIRONMENT.name() + ":")) {
                                            environmentsRoles.add(role.split(":")[1]);
                                        }
                                    }
                                    roleMapping.setOrganizations(organizationsRoles);
                                    roleMapping.setEnvironments(environmentsRoles);
                                }

                                return roleMapping;
                            }
                        }
                    )
                    .collect(Collectors.toList())
            );
        }

        identityProviderEntity.setConfiguration(identityProvider.getConfiguration());
        identityProviderEntity.setCreatedAt(identityProvider.getCreatedAt());
        identityProviderEntity.setUpdatedAt(identityProvider.getUpdatedAt());
        identityProviderEntity.setUserProfileMapping(identityProvider.getUserProfileMapping());
        if (identityProvider.getEmailRequired() == null) {
            identityProviderEntity.setEmailRequired(true);
        } else {
            identityProviderEntity.setEmailRequired(identityProvider.getEmailRequired());
        }

        identityProviderEntity.setSyncMappings(identityProvider.getSyncMappings() == null ? false : identityProvider.getSyncMappings());

        return identityProviderEntity;
    }

    private IdentityProvider convert(UpdateIdentityProviderEntity updateIdentityProvider) {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setName(updateIdentityProvider.getName());
        identityProvider.setDescription(updateIdentityProvider.getDescription());
        identityProvider.setEnabled(updateIdentityProvider.isEnabled());
        identityProvider.setConfiguration(updateIdentityProvider.getConfiguration());
        identityProvider.setUserProfileMapping(updateIdentityProvider.getUserProfileMapping());
        identityProvider.setEmailRequired(updateIdentityProvider.isEmailRequired());
        identityProvider.setSyncMappings(updateIdentityProvider.isSyncMappings());

        if (updateIdentityProvider.getGroupMappings() != null && !updateIdentityProvider.getGroupMappings().isEmpty()) {
            identityProvider.setGroupMappings(
                updateIdentityProvider
                    .getGroupMappings()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            GroupMappingEntity::getCondition,
                            groupMappingEntity -> {
                                String[] groups = new String[groupMappingEntity.getGroups().size()];
                                return groupMappingEntity.getGroups().toArray(groups);
                            }
                        )
                    )
            );
        }

        if (updateIdentityProvider.getRoleMappings() != null && !updateIdentityProvider.getRoleMappings().isEmpty()) {
            identityProvider.setRoleMappings(
                updateIdentityProvider
                    .getRoleMappings()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            RoleMappingEntity::getCondition,
                            roleMapping -> {
                                List<String> lstRoles = new ArrayList<>();
                                if (roleMapping.getOrganizations() != null && !roleMapping.getOrganizations().isEmpty()) {
                                    roleMapping
                                        .getOrganizations()
                                        .forEach(
                                            organizationRoleName -> {
                                                // Ensure that the role is existing
                                                roleService.findByScopeAndName(RoleScope.ORGANIZATION, organizationRoleName);
                                                lstRoles.add(
                                                    io.gravitee.repository.management.model.RoleScope.ORGANIZATION.name() +
                                                    ":" +
                                                    organizationRoleName
                                                );
                                            }
                                        );
                                }
                                if (roleMapping.getEnvironments() != null && !roleMapping.getEnvironments().isEmpty()) {
                                    roleMapping
                                        .getEnvironments()
                                        .forEach(
                                            environmentRoleName -> {
                                                // Ensure that the role is existing
                                                roleService.findByScopeAndName(RoleScope.ENVIRONMENT, environmentRoleName);
                                                lstRoles.add(
                                                    io.gravitee.repository.management.model.RoleScope.ENVIRONMENT.name() +
                                                    ":" +
                                                    environmentRoleName
                                                );
                                            }
                                        );
                                }

                                String[] roles = new String[lstRoles.size()];
                                return lstRoles.toArray(roles);
                            }
                        )
                    )
            );
        }

        return identityProvider;
    }
}
