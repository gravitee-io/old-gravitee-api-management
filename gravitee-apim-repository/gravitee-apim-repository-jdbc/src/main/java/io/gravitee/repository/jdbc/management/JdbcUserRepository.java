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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.management.model.UserReferenceType;
import io.gravitee.repository.management.model.UserStatus;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 */
@Repository
public class JdbcUserRepository extends JdbcAbstractCrudRepository<User, String> implements UserRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcUserRepository.class);

    private static final String SELECT_ESCAPED_USER_TABLE_NAME = "select * from " + escapeReservedWord("users");

    private static final String STATUS_FIELD = "status";

    private static final JdbcObjectMapper ORM = JdbcObjectMapper
        .builder(User.class, "users", "id")
        .addColumn("id", Types.NVARCHAR, String.class)
        .addColumn("reference_id", Types.NVARCHAR, String.class)
        .addColumn("reference_type", Types.NVARCHAR, UserReferenceType.class)
        .addColumn("created_at", Types.TIMESTAMP, Date.class)
        .addColumn("email", Types.NVARCHAR, String.class)
        .addColumn("firstname", Types.NVARCHAR, String.class)
        .addColumn("last_connection_at", Types.TIMESTAMP, Date.class)
        .addColumn("lastname", Types.NVARCHAR, String.class)
        .addColumn("password", Types.NVARCHAR, String.class)
        .addColumn("picture", Types.NVARCHAR, String.class)
        .addColumn("source", Types.NVARCHAR, String.class)
        .addColumn("source_id", Types.NVARCHAR, String.class)
        .addColumn("updated_at", Types.TIMESTAMP, Date.class)
        .addColumn(STATUS_FIELD, Types.NVARCHAR, UserStatus.class)
        .addColumn("login_count", Types.BIGINT, long.class)
        .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(User item) {
        return item.getId();
    }

    @Override
    public Optional<User> findBySource(String source, String sourceId, String referenceId, UserReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcUserRepository.findBySource({}, {})", source, sourceId);
        try {
            List<User> users = jdbcTemplate.query(
                SELECT_ESCAPED_USER_TABLE_NAME +
                " u where u.source = ? and UPPER(u.source_id) = UPPER(?) and reference_id = ? and reference_type = ?",
                ORM.getRowMapper(),
                source,
                sourceId,
                referenceId,
                referenceType.name()
            );
            return users.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find user by source", ex);
            throw new TechnicalException("Failed to find user by source", ex);
        }
    }

    @Override
    public Optional<User> findByEmail(String email, String referenceId, UserReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcUserRepository.findByEmail({})", email);
        try {
            List<User> users = jdbcTemplate.query(
                SELECT_ESCAPED_USER_TABLE_NAME + " u where UPPER(u.email) = UPPER(?) and reference_id = ? and reference_type = ?",
                ORM.getRowMapper(),
                email,
                referenceId,
                referenceType.name()
            );
            return users.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find user by email", ex);
            throw new TechnicalException("Failed to find user by email", ex);
        }
    }

    @Override
    public Set<User> findByIds(final List<String> ids) throws TechnicalException {
        final String[] lastId = new String[1];
        List<String> uniqueIds = ids
            .stream()
            .filter(
                id -> {
                    if (id.equals(lastId[0])) {
                        return false;
                    } else {
                        lastId[0] = id;
                        return true;
                    }
                }
            )
            .collect(Collectors.toList());
        LOGGER.debug("JdbcUserRepository.findByIds({})", uniqueIds);
        try {
            final List<User> users = jdbcTemplate.query(
                SELECT_ESCAPED_USER_TABLE_NAME + " u where u.id in ( " + ORM.buildInClause(uniqueIds) + " )",
                (PreparedStatement ps) -> ORM.setArguments(ps, uniqueIds, 1),
                ORM.getRowMapper()
            );
            return new HashSet<>(users);
        } catch (final Exception ex) {
            final String msg = "Failed to find users by ids";
            LOGGER.error(msg, ex);
            throw new TechnicalException(msg, ex);
        }
    }

    @Override
    public Page<User> search(UserCriteria criteria, Pageable pageable) throws TechnicalException {
        LOGGER.debug("JdbcUserRepository<{}>.search()", getOrm().getTableName());

        try {
            List result;
            if (criteria == null) {
                result = jdbcTemplate.query(getOrm().getSelectAllSql() + "order by lastname, firstname", getRowMapper());
            } else {
                final StringBuilder query = new StringBuilder(SELECT_ESCAPED_USER_TABLE_NAME);

                query.append(" where 1=1 ");

                if (criteria.getStatuses() != null && criteria.getStatuses().length > 0) {
                    List<UserStatus> statuses = Arrays.asList(criteria.getStatuses());
                    ORM.buildInCondition(false, query, STATUS_FIELD, statuses);
                }

                if (criteria.hasNoStatus()) {
                    query.append(" and ").append(escapeReservedWord(STATUS_FIELD)).append(" is null ");
                }

                if (criteria.getReferenceId() != null) {
                    query.append(" and reference_id = ? ");
                }
                if (criteria.getReferenceType() != null) {
                    query.append(" and reference_type = ? ");
                }

                query.append(" order by lastname, firstname ");

                result =
                    jdbcTemplate.query(
                        query.toString(),
                        (PreparedStatement ps) -> {
                            int idx = 1;
                            if (criteria.getStatuses() != null && criteria.getStatuses().length > 0) {
                                List<UserStatus> statuses = Arrays.asList(criteria.getStatuses());
                                idx = ORM.setArguments(ps, statuses, idx);
                            }
                            if (criteria.getReferenceId() != null) {
                                idx = ORM.setArguments(ps, Arrays.asList(criteria.getReferenceId()), idx);
                            }
                            if (criteria.getReferenceType() != null) {
                                idx = ORM.setArguments(ps, Arrays.asList(criteria.getReferenceType().name()), idx);
                            }
                        },
                        ORM.getRowMapper()
                    );
            }
            return getResultAsPage(pageable, result);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all {} items:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find all " + getOrm().getTableName() + " items", ex);
        }
    }
}
