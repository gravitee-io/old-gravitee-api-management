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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class EnvironmentServiceImpl extends TransactionalService implements EnvironmentService {

    private final Logger LOGGER = LoggerFactory.getLogger(EnvironmentServiceImpl.class);

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private ApiHeaderService apiHeaderService;

    @Autowired
    private PageService pageService;

    @Autowired
    private MembershipService membershipService;

    @Override
    public EnvironmentEntity findById(String environmentId) {
        try {
            LOGGER.debug("Find environment by ID: {}", environmentId);
            Optional<Environment> optEnvironment = environmentRepository.findById(environmentId);

            if (!optEnvironment.isPresent()) {
                throw new EnvironmentNotFoundException(environmentId);
            }

            return convert(optEnvironment.get());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find environment by ID", ex);
            throw new TechnicalManagementException("An error occurs while trying to find environment by ID", ex);
        }
    }

    @Override
    public List<EnvironmentEntity> findByUser(String userId) {
        return findByUserAndIdOrHrid(userId, null);
    }

    @Override
    public List<EnvironmentEntity> findByUserAndIdOrHrid(String userId, String idOrHrid) {
        try {
            LOGGER.debug("Find all environments by user");

            Stream<Environment> envStream = environmentRepository.findByOrganization(GraviteeContext.getCurrentOrganization()).stream();

            if (userId != null) {
                final List<String> stringStream = membershipService
                    .getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.ENVIRONMENT)
                    .stream()
                    .map(MembershipEntity::getReferenceId)
                    .collect(Collectors.toList());

                envStream = envStream.filter(env -> stringStream.contains(env.getId()));
            }

            if (!StringUtils.isEmpty(idOrHrid)) {
                envStream =
                    envStream.filter(
                        env -> idOrHrid.equals(env.getId()) || !CollectionUtils.isEmpty(env.getHrids()) && env.getHrids().contains(idOrHrid)
                    );
            }

            return envStream.map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all environments", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all environments", ex);
        }
    }

    @Override
    public EnvironmentEntity createOrUpdate(String organizationId, String environmentId, final UpdateEnvironmentEntity environmentEntity) {
        try {
            // First we check that organization exists
            this.organizationService.findById(organizationId);

            Optional<Environment> environmentOptional = environmentRepository.findById(environmentId);
            Environment environment = convert(environmentEntity);
            environment.setId(environmentId);
            environment.setOrganizationId(organizationId);

            if (environmentOptional.isPresent()) {
                return convert(environmentRepository.update(environment));
            } else {
                EnvironmentEntity createdEnvironment = convert(environmentRepository.create(environment));

                //create Default items for environment
                apiHeaderService.initialize(createdEnvironment.getId());
                pageService.initialize(createdEnvironment.getId());

                return createdEnvironment;
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update environment {}", environmentEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to update environment " + environmentEntity.getName(), ex);
        }
    }

    @Override
    public void delete(final String environmentId) {
        try {
            Optional<Environment> environmentOptional = environmentRepository.findById(environmentId);
            if (environmentOptional.isPresent()) {
                environmentRepository.delete(environmentId);
            } else {
                throw new EnvironmentNotFoundException(environmentId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete environment {}", environmentId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete environment " + environmentId, ex);
        }
    }

    @Override
    public void initialize() {
        Environment defaultEnvironment = new Environment();
        defaultEnvironment.setId(GraviteeContext.getDefaultEnvironment());
        defaultEnvironment.setName("Default environment");
        defaultEnvironment.setDescription("Default environment");
        defaultEnvironment.setOrganizationId(GraviteeContext.getDefaultOrganization());
        try {
            environmentRepository.create(defaultEnvironment);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create default environment", ex);
            throw new TechnicalManagementException("An error occurs while trying to create default environment", ex);
        }
    }

    @Override
    public EnvironmentEntity findByCockpitId(String cockpitId) {
        try {
            LOGGER.debug("Find environment by cockpit id");
            return environmentRepository
                .findByCockpit(cockpitId)
                .map(this::convert)
                .orElseThrow(() -> new EnvironmentNotFoundException(cockpitId));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find environment by cockpit id {}", cockpitId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find environment by cockpit id " + cockpitId, ex);
        }
    }

    @Override
    public List<EnvironmentEntity> findByOrganization(String organizationId) {
        try {
            LOGGER.debug("Find all environments by organization");
            return environmentRepository.findByOrganization(organizationId).stream().map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all environments by organization {}", organizationId, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to find all environments by organization " + organizationId,
                ex
            );
        }
    }

    private Environment convert(final UpdateEnvironmentEntity environmentEntity) {
        final Environment environment = new Environment();
        environment.setCockpitId(environmentEntity.getCockpitId());
        environment.setHrids(environmentEntity.getHrids());
        environment.setName(environmentEntity.getName());
        environment.setDescription(environmentEntity.getDescription());
        environment.setDomainRestrictions(environmentEntity.getDomainRestrictions());
        return environment;
    }

    private EnvironmentEntity convert(final Environment environment) {
        final EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(environment.getId());
        environmentEntity.setCockpitId(environment.getCockpitId());
        environmentEntity.setHrids(environment.getHrids());
        environmentEntity.setName(environment.getName());
        environmentEntity.setDescription(environment.getDescription());
        environmentEntity.setOrganizationId(environment.getOrganizationId());
        environmentEntity.setDomainRestrictions(environment.getDomainRestrictions());
        return environmentEntity;
    }
}
