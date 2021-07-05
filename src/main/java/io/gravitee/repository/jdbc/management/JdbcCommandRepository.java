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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.model.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static java.lang.String.format;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcCommandRepository extends JdbcAbstractCrudRepository<Command, String> implements CommandRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcCommandRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Command.class, "commands", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("from", Types.NVARCHAR, String.class)
            .addColumn("to", Types.NVARCHAR, String.class)
            .addColumn("content", Types.NVARCHAR, String.class)
            .addColumn("expired_at", Types.TIMESTAMP, Date.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();

    private static final JdbcHelper.ChildAdder<Command> CHILD_ADDER = (Command parent, ResultSet rs) -> {
        List<String> acknowledgments = parent.getAcknowledgments();
        if (acknowledgments == null) {
            acknowledgments = new ArrayList<>();
            parent.setAcknowledgments(acknowledgments);
        }
        String acknowledgment = rs.getString("acknowledgment");
        if (acknowledgment != null && !acknowledgments.contains(acknowledgment)) {
            acknowledgments.add(acknowledgment);
        }

        List<String> tags = parent.getTags();
        if (tags == null) {
            tags = new ArrayList<>();
            parent.setTags(tags);
        }
        String tag = rs.getString("tag");
        if (tag != null && !tags.contains(tag)) {
            tags.add(tag);
        }
    };

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Command item) {
        return item.getId();
    }

    @Override
    public Optional<Command> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcCommandRepository.findById({})", id);
        try {
            JdbcHelper.CollatingRowMapper<Command> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query("select * from " + escapeReservedWord("commands") + " c " +
                            "left join command_acknowledgments ca on c.id = ca.command_id " +
                            "left join command_tags ct on c.id = ct.command_id " +
                            "where c.id = ?"
                    , rowMapper
                    , id
            );
            return rowMapper.getRows().stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find command by id:", ex);
            throw new TechnicalException("Failed to find command by id", ex);
        }
    }

    @Override
    public Command create(Command item) throws TechnicalException {
        LOGGER.debug("JdbcCommandRepository.create({})", item);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            storeAcknowledgments(item, false);
            storeTags(item, false);
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create plan", ex);
            throw new TechnicalException("Failed to create plan", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        LOGGER.debug("JdbcCommandRepository.delete({})", id);
        try {
            jdbcTemplate.update("delete from command_acknowledgments where command_id = ?", id);
            jdbcTemplate.update("delete from command_tags where command_id = ?", id);
            jdbcTemplate.update(ORM.getDeleteSql(), id);
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete command:", ex);
            throw new TechnicalException("Failed to delete command", ex);
        }
    }

    @Override
    public Command update(Command item) throws TechnicalException {
        LOGGER.debug("JdbcCommandRepository.update({})", item);
        if (item == null) {
            throw new IllegalStateException();
        }
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(item, item.getId()));
            storeAcknowledgments(item, true);
            storeTags(item, true);
            return findById(item.getId()).orElseThrow(() ->
                    new IllegalStateException(format("No command found with id [%s]", item.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update command", ex);
            throw new TechnicalException("Failed to update command", ex);
        }
    }

    @Override
    public List<Command> search(CommandCriteria criteria) {
        LOGGER.debug("JdbcCommandRepository.search({})", criteria);
        JdbcHelper.CollatingRowMapper<Command> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "id");
        final StringBuilder query = new StringBuilder(
                "select * from " + escapeReservedWord("commands") + " c " +
                "left join command_acknowledgments ca on c.id = ca.command_id " +
                "left join command_tags ct on c.id = ct.command_id " +
                "where 1=1 ");

        if (criteria.getNotAckBy() != null) {
            query.append(" and not exists (")
                    .append("select 1 from command_acknowledgments cak ")
                    .append("where cak.command_id = c.id ")
                    .append("and cak.acknowledgment = ? ")
                    .append(")");
        }
        if (criteria.getNotFrom() != null) {
            query.append(" and c.").append(escapeReservedWord("from")).append(" != ? ");
        }
        if (criteria.getTo() != null) {
            query.append(" and c.").append(escapeReservedWord("to")).append(" = ? ");
        }
        if (criteria.getEnvironmentId() != null) {
            query.append(" and c.environment_id = ? ");
        }
        if (criteria.isNotExpired()) {
            query.append(" and c.expired_at >= ? ");
        }

        List<Command> commands;
        try {
            jdbcTemplate.query(query.toString(), (PreparedStatement ps) -> {
                int lastIndex = 1;
                if (criteria.getNotAckBy() != null) {
                    ps.setString(lastIndex++, criteria.getNotAckBy());
                }
                if (criteria.getNotFrom() != null) {
                    ps.setString(lastIndex++, criteria.getNotFrom());
                }
                if (criteria.getTo() != null) {
                    ps.setString(lastIndex++, criteria.getTo());
                }
                if (criteria.getEnvironmentId() != null) {
                    ps.setString(lastIndex++, criteria.getEnvironmentId());
                }
                if (criteria.isNotExpired()) {
                    ps.setDate(lastIndex++, new java.sql.Date(System.currentTimeMillis()));
                }
            }, rowMapper);
            commands = rowMapper.getRows();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find command records:", ex);
            throw new IllegalStateException("Failed to find command records", ex);
        }

        if (criteria.getTags() != null && criteria.getTags().length > 0) {
            commands = commands
                    .stream()
                    .filter(command -> command.getTags() != null && command.getTags().containsAll(Arrays.asList(criteria.getTags())))
                    .collect(Collectors.toList());
        }

        LOGGER.debug("command records found ({}): {}", commands.size(), commands);
        return commands;
    }

    private void storeAcknowledgments(Command command, boolean deleteFirst) {
        LOGGER.debug("JdbcCommandRepository.storeAcknowledgments({}, {})", command, deleteFirst);
        if (deleteFirst) {
            jdbcTemplate.update("delete from command_acknowledgments where command_id = ?", command.getId());
        }
        List<String> acknowledgments = ORM.filterStrings(command.getAcknowledgments());
        if (!acknowledgments.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into command_acknowledgments ( command_id, acknowledgment ) values ( ?, ? )"
                    , ORM.getBatchStringSetter(command.getId(), acknowledgments));
        }
    }

    private void storeTags(Command command, boolean deleteFirst) {
        LOGGER.debug("JdbcCommandRepository.storeTags({}, {})", command, deleteFirst);
        if (deleteFirst) {
            jdbcTemplate.update("delete from command_tags where command_id = ?", command.getId());
        }

        List<String> tags = ORM.filterStrings(command.getTags());
        if (!tags.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into command_tags ( command_id, tag ) values ( ?, ? )"
                    , ORM.getBatchStringSetter(command.getId(), tags));
        }
    }
}