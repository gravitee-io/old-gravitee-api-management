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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.mongodb.management.internal.group.GroupMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.GroupMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class MongoGroupRepository implements GroupRepository {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private GroupMongoRepository internalRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Group> findById(String s) throws TechnicalException {
        logger.debug("Find group by id [{}]", s);
        Group group = map(internalRepository.findById(s).orElse(null));
        logger.debug("Find group by id [{}] - DONE", s);
        return Optional.ofNullable(group);
    }

    @Override
    public Set<Group> findByIds(Set<String> ids) throws TechnicalException {
        logger.debug("Find groups by ids");
        Set<Group> groups = internalRepository.findByIds(ids)
                .stream()
                .map(this::map)
                .collect(Collectors.toSet());
        logger.debug("Find groups by ids - Found {}", groups);
        return groups;
    }

    @Override
    public Group create(Group group) throws TechnicalException {
        logger.debug("Create group [{}]", group.getName());
        Group createdGroup = map(internalRepository.insert(map(group)));
        logger.debug("Create group [{}] - Done", createdGroup.getName());
        return createdGroup;
    }

    @Override
    public Group update(Group group) throws TechnicalException {
        if (group == null) {
            throw new IllegalStateException("Group must not be null");
        }

        final GroupMongo groupMongo = internalRepository.findById(group.getId()).orElse(null);
        if (groupMongo == null) {
            throw new IllegalStateException(String.format("No group found with id [%s]", group.getId()));
        }
        
        logger.debug("Update group [{}]", group.getName());
        Group updatedGroup = map(internalRepository.save(map(group)));
        logger.debug("Update group [{}] - Done", updatedGroup.getName());
        return updatedGroup;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        logger.debug("Delete group [{}]", id);
        internalRepository.deleteById(id);
        logger.debug("Delete group [{}] - Done", id);
    }

    @Override
    public Set<Group> findAll() throws TechnicalException {
        logger.debug("Find all groups");
        Set<Group> all = internalRepository.findAll().
                stream().
                map(this::map).
                collect(Collectors.toSet());
        logger.debug("Find all groups - Found {}", all);
        return all;
    }

    private GroupMongo map(Group group) {
        return mapper.map(group, GroupMongo.class);
    }

    private Group map(GroupMongo groupMongo) {
        return mapper.map(groupMongo, Group.class);
    }

    @Override
    public Set<Group> findAllByEnvironment(String environmentId) throws TechnicalException {
        logger.debug("Find all groups by environment");
        Set<Group> all = internalRepository.findByEnvironmentId(environmentId).
                stream().
                map(this::map).
                collect(Collectors.toSet());
        logger.debug("Find all groups by environment - Found {}", all);
        return all;
    }

}
