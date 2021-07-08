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
package io.gravitee.repository.jdbc.management;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static java.lang.String.format;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.repository.management.model.GroupEventRule;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcGroupRepository extends JdbcAbstractCrudRepository<Group, String> implements GroupRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcGroupRepository.class);

    private static final String SELECT_ESCAPED_GROUP_TABLE_NAME = "select * from " + escapeReservedWord("groups");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final JdbcObjectMapper ORM = JdbcObjectMapper
        .builder(Group.class, "groups", "id")
        .addColumn("id", Types.NVARCHAR, String.class)
        .addColumn("environment_id", Types.NVARCHAR, String.class)
        .addColumn("name", Types.NVARCHAR, String.class)
        .addColumn("created_at", Types.TIMESTAMP, Date.class)
        .addColumn("updated_at", Types.TIMESTAMP, Date.class)
        .addColumn("max_invitation", Types.INTEGER, Integer.class)
        .addColumn("lock_api_role", Types.BIT, boolean.class)
        .addColumn("lock_application_role", Types.BIT, boolean.class)
        .addColumn("system_invitation", Types.BIT, boolean.class)
        .addColumn("email_invitation", Types.BIT, boolean.class)
        .addColumn("disable_membership_notifications", Types.BIT, boolean.class)
        .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Group item) {
        return item.getId();
    }

    public Optional<Group> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcGroupRepository.findById({})", id);
        try {
            Optional<Group> group = jdbcTemplate
                .query(SELECT_ESCAPED_GROUP_TABLE_NAME + " g where id = ?", ORM.getRowMapper(), id)
                .stream()
                .findFirst();
            if (group.isPresent()) {
                addGroupEvents(group.get());
            }
            return group;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find group by id", ex);
            throw new TechnicalException("Failed to find group by id", ex);
        }
    }

    @Override
    public Group create(final Group group) throws TechnicalException {
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(group));
            storeGroupEvents(group, false);
            return findById(group.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create group", ex);
            throw new TechnicalException("Failed to create group", ex);
        }
    }

    @Override
    public Group update(final Group group) throws TechnicalException {
        if (group == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(group, group.getId()));
            storeGroupEvents(group, true);
            return findById(group.getId())
                .orElseThrow(() -> new IllegalStateException(format("No group found with id [%s]", group.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update group:", ex);
            throw new TechnicalException("Failed to update group", ex);
        }
    }

    @Override
    public void delete(final String id) throws TechnicalException {
        jdbcTemplate.update("delete from group_event_rules where group_id = ?", id);
        jdbcTemplate.update(ORM.getDeleteSql(), id);
    }

    private void addGroupEvents(Group parent) {
        List<GroupEventRule> groupEvents = getEvents(parent.getId());
        parent.setEventRules(groupEvents);
    }

    private List<GroupEventRule> getEvents(String groupId) {
        List<GroupEvent> groupEvents = jdbcTemplate.query(
            "select group_event from group_event_rules where group_id = ?",
            (ResultSet rs, int rowNum) -> {
                String value = rs.getString(1);
                try {
                    return GroupEvent.valueOf(value);
                } catch (IllegalArgumentException ex) {
                    LOGGER.error("Failed to parse {} as group_event:", value, ex);
                    return null;
                }
            },
            groupId
        );

        List<GroupEventRule> groupEventRules = new ArrayList<>(groupEvents.size());
        for (GroupEvent groupEvent : groupEvents) {
            if (groupEvent != null) {
                groupEventRules.add(new GroupEventRule(groupEvent));
            }
        }

        return groupEventRules;
    }

    private void storeGroupEvents(Group group, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from group_event_rules where group_id = ?", group.getId());
        }
        List<String> events = new ArrayList<>();
        if (group.getEventRules() != null) {
            for (GroupEventRule groupEventRule : group.getEventRules()) {
                events.add(groupEventRule.getEvent().name());
            }
        }
        if (!events.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into group_event_rules ( group_id, group_event ) values ( ?, ? )",
                ORM.getBatchStringSetter(group.getId(), events)
            );
        }
    }

    @Override
    public Set<Group> findAll() throws TechnicalException {
        LOGGER.debug("JdbcGroupRepository.findAll()");
        try {
            List<Group> rows = jdbcTemplate.query(SELECT_ESCAPED_GROUP_TABLE_NAME, ORM.getRowMapper());
            Set<Group> groups = new HashSet<>();
            for (Group group : rows) {
                addGroupEvents(group);
                groups.add(group);
            }
            return groups;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all groups:", ex);
            throw new TechnicalException("Failed to find all groups", ex);
        }
    }

    @Override
    public Set<Group> findByIds(Set<String> ids) throws TechnicalException {
        LOGGER.debug("JdbcGroupRepository.findByIds({})", ids);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        final StringBuilder query = new StringBuilder(SELECT_ESCAPED_GROUP_TABLE_NAME);
        ORM.buildInCondition(true, query, "id", ids);
        try {
            List<Group> rows = jdbcTemplate.query(
                query.toString(),
                (PreparedStatement ps) -> ORM.setArguments(ps, ids, 1),
                ORM.getRowMapper()
            );
            Set<Group> groups = new HashSet<>();
            for (Group group : rows) {
                addGroupEvents(group);
                groups.add(group);
            }
            return groups;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find group by ids", ex);
            throw new TechnicalException("Failed to find group by ids", ex);
        }
    }

    @Override
    public Set<Group> findAllByEnvironment(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcGroupRepository.findAllByEnvironment({})", environmentId);
        try {
            List<Group> rows = jdbcTemplate.query(
                SELECT_ESCAPED_GROUP_TABLE_NAME + " where environment_id = ?",
                ORM.getRowMapper(),
                environmentId
            );
            Set<Group> groups = new HashSet<>();
            for (Group group : rows) {
                addGroupEvents(group);
                groups.add(group);
            }
            return groups;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all groups by environment : {}", environmentId, ex);
            throw new TechnicalException("Failed to find all groups by environment : " + environmentId, ex);
        }
    }
}
