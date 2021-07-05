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
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 *
 * @author njt
 */
@Repository
public class JdbcApplicationRepository extends JdbcAbstractCrudRepository<Application, String> implements ApplicationRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcApplicationRepository.class);

    private static final String TYPE_FIELD = "type";
    private static final String STATUS_FIELD = "status";

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Application.class, "applications", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn(TYPE_FIELD, Types.NVARCHAR, ApplicationType.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("picture", Types.NVARCHAR, String.class)
            .addColumn(STATUS_FIELD, Types.NVARCHAR, ApplicationStatus.class)
            .addColumn("disable_membership_notifications", Types.BIT, boolean.class)
            .build();

    private static final JdbcHelper.ChildAdder<Application> CHILD_ADDER = (Application parent, ResultSet rs) -> {
        Map<String, String> metadata = parent.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
            parent.setMetadata(metadata);
        }
        if (rs.getString("am_k") != null) {
            metadata.put(rs.getString("am_k"), rs.getString("am_v"));
        }
    };

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Application item) {
        return item.getId();
    }
    
    private void addGroups(Application parent) {        
        List<String> groups = getGroups(parent.getId());
        parent.setGroups(new HashSet<>(groups));
    }
    
    private List<String> getGroups(String apiId) {
        return jdbcTemplate.queryForList("select group_id from application_groups where application_id = ?", String.class, apiId);
    }
    
    private void storeGroups(Application application, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from application_groups where application_id = ?", application.getId());
        }
        List<String> filteredGroups = ORM.filterStrings(application.getGroups());
        if (! filteredGroups.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into application_groups ( application_id, group_id ) values ( ?, ? )"
                    , ORM.getBatchStringSetter(application.getId(), filteredGroups));
        }
    }

    private void storeMetadata(Application application, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from application_metadata where application_id = ?", application.getId());
        }
        if (application.getMetadata() != null && !application.getMetadata().isEmpty()) {
            List<Map.Entry<String, String>> entries = new ArrayList<>(application.getMetadata().entrySet());
            jdbcTemplate.batchUpdate("insert into application_metadata ( application_id, k, v ) values ( ?, ?, ? )"
                    , new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            ps.setString(1, application.getId());
                            ps.setString(2, entries.get(i).getKey());
                            ps.setString(3, entries.get(i).getValue());
                        }

                        @Override
                        public int getBatchSize() {
                            return entries.size();
                        }
                    });
        }
    }

    @Override
    public Application create(Application item) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.create({})", item);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            storeGroups(item, false);
            storeMetadata(item, false);
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create application", ex);
            throw new TechnicalException("Failed to create application", ex);
        }
    }

    @Override
    public Application update(final Application application) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.update({})", application);
        if (application == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(application, application.getId()));
            storeGroups(application, true);
            storeMetadata(application, true);
            return findById(application.getId()).orElseThrow(() -> new IllegalStateException(format("No application found with id [%s]", application.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update application", ex);
            throw new TechnicalException("Failed to update application", ex);
        }
    }

    private class Rm implements RowMapper<Application> {
        @Override
        public Application mapRow(ResultSet rs, int i) throws SQLException {
            Application application = new Application();
            ORM.setFromResultSet(application, rs);
            addGroups(application);
            return application;
        }
    }

    private final Rm mapper = new Rm();

    @Override
    public Optional<Application> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findById({})", id);
        try {
            JdbcHelper.CollatingRowMapper<Application> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            jdbcTemplate.query("select a.*, am.k as am_k, am.v as am_v from applications a left join application_metadata am on a.id = am.application_id where a.id = ?"
                    , rowMapper
                    , id
            );
            Optional<Application> result = rowMapper.getRows().stream().findFirst();
            result.ifPresent(this::addGroups);
            LOGGER.debug("JdbcApplicationRepository.findById({}) = {}", id, result);
            return result;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find application by id:", ex);
            throw new TechnicalException("Failed to find application by id", ex);
        }
    }

    @Override
    public Set<Application> findByIds(List<String> ids) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findByIds({})", ids);
        try {
            if (isEmpty(ids)) {
                return emptySet();
            }
            JdbcHelper.CollatingRowMapper<Application> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            jdbcTemplate.query("select a.*, am.k as am_k, am.v as am_v from applications a left join application_metadata am on a.id = am.application_id where a.id in ( "
                    + ORM.buildInClause(ids) + " )"
                    , (PreparedStatement ps) -> ORM.setArguments(ps, ids, 1)
                    , rowMapper
            );
            for (Application application : rowMapper.getRows()) {
                addGroups(application);
            }
            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find applications by ids:", ex);
            throw new TechnicalException("Failed to find  applications by ids", ex);
        }
    }

    @Override
    public Set<Application> findAll(ApplicationStatus... ass) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findAll({})", (Object[])ass);

        try {
            List<ApplicationStatus> statuses = Arrays.asList(ass);
            
            StringBuilder query = new StringBuilder("select a.*, am.k as am_k, am.v as am_v from applications a left join application_metadata am on a.id = am.application_id");
            boolean first = true;
            ORM.buildInCondition(first, query, STATUS_FIELD, statuses);

            JdbcHelper.CollatingRowMapper<Application> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> ORM.setArguments(ps, statuses, 1)
                    , rowMapper
            );
            for (Application application : rowMapper.getRows()) {
                addGroups(application);
            }
            LOGGER.debug("Found {} applications: {}", rowMapper.getRows().size(), rowMapper.getRows());
            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find applications:", ex);
            throw new TechnicalException("Failed to find applications", ex);
        }
    }

    @Override
    public Set<Application> findByGroups(List<String> groupIds, ApplicationStatus... ass) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findByGroups({}, {})", groupIds, ass);
        if (isEmpty(groupIds)) {
            return emptySet();
        }
        try {
            final List<ApplicationStatus> statuses = Arrays.asList(ass);
            final StringBuilder query = new StringBuilder("select a.*, am.k as am_k, am.v as am_v from applications a left join application_metadata am on a.id = am.application_id join application_groups ag on ag.application_id = a.id ");
            boolean first = true;
            first = ORM.buildInCondition(first, query, "group_id", groupIds);
            ORM.buildInCondition(first, query, STATUS_FIELD, statuses);

            JdbcHelper.CollatingRowMapper<Application> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> {
                        int idx = ORM.setArguments(ps, groupIds, 1);
                        ORM.setArguments(ps, statuses, idx);
                    }
                    , rowMapper
            );
            for (Application application : rowMapper.getRows()) {
                addGroups(application);
            }
            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find applications by groups", ex);
            throw new TechnicalException("Failed to find applications by groups", ex);
        }
    }

    @Override
    public Set<Application> findByName(String partialName) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findByName({})", partialName);
        try {
            JdbcHelper.CollatingRowMapper<Application> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            jdbcTemplate.query("select a.*, am.k as am_k, am.v as am_v from applications a left join application_metadata am on a.id = am.application_id where lower(name) like ?"
                    , rowMapper, "%" + partialName.toLowerCase() + "%"
            );
            for (Application application : rowMapper.getRows()) {
                addGroups(application);
            }
            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find applications by name", ex);
            throw new TechnicalException("Failed to find applications by name", ex);
        }
    }

    @Override
    public Set<Application> findAllByEnvironment(String environmentId, ApplicationStatus... ass)
            throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findAllByEnvironment({}, {})", environmentId, (Object[])ass);

        try {
            List<ApplicationStatus> statuses = Arrays.asList(ass);
            
            StringBuilder query = new StringBuilder("select a.*, am.k as am_k, am.v as am_v from applications a left join application_metadata am on a.id = am.application_id where a.environment_id = ?");
            boolean first = false;
            ORM.buildInCondition(first, query, STATUS_FIELD, statuses);

            JdbcHelper.CollatingRowMapper<Application> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> {
                        ORM.setArguments(ps, Arrays.asList(environmentId), 1);
                        ORM.setArguments(ps, statuses, 2);
                    }
                    , rowMapper
            );
            for (Application application : rowMapper.getRows()) {
                addGroups(application);
            }
            LOGGER.debug("Found {} applications: {}", rowMapper.getRows().size(), rowMapper.getRows());
            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find applications by environment:", ex);
            throw new TechnicalException("Failed to find applications by environment", ex);
        }
    }
}
