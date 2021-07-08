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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import java.sql.Types;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcApiKeyRepository extends JdbcAbstractCrudRepository<ApiKey, String> implements ApiKeyRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcApiKeyRepository.class);

    JdbcApiKeyRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "keys");
    }

    @Override
    protected JdbcObjectMapper<ApiKey> buildOrm() {
        return JdbcObjectMapper
            .builder(ApiKey.class, this.tableName, "key")
            .addColumn("key", Types.NVARCHAR, String.class)
            .addColumn("subscription", Types.NVARCHAR, String.class)
            .addColumn("application", Types.NVARCHAR, String.class)
            .addColumn("plan", Types.NVARCHAR, String.class)
            .addColumn("expire_at", Types.TIMESTAMP, Date.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("revoked", Types.BOOLEAN, boolean.class)
            .addColumn("paused", Types.BOOLEAN, boolean.class)
            .addColumn("revoked_at", Types.TIMESTAMP, Date.class)
            .addColumn("days_to_expiration_on_last_notification", Types.INTEGER, Integer.class)
            .build();
    }

    @Override
    protected String getId(ApiKey item) {
        return item.getKey();
    }

    @Override
    public List<ApiKey> findByCriteria(ApiKeyCriteria akc) throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.findByCriteria({})", akc);
        try {
            List<Object> args = new ArrayList<>();
            StringBuilder query = new StringBuilder(getOrm().getSelectAllSql()).append(" ");

            boolean first = true;
            if (!akc.isIncludeRevoked()) {
                first = addClause(first, query);
                query.append(" ( revoked = ? ) ");
                args.add(false);
            }
            if ((akc.getPlans() != null) && !akc.getPlans().isEmpty()) {
                first = addClause(first, query);
                query.append(" ( ").append(escapeReservedWord("plan")).append(" in ( ");
                boolean subFirst = true;
                for (String plan : akc.getPlans()) {
                    if (!subFirst) {
                        query.append(", ");
                    }
                    subFirst = false;
                    query.append(" ?");
                    args.add(plan);
                }
                query.append(" ) ) ");
            }
            if (akc.getFrom() > 0) {
                first = addClause(first, query);
                query.append(" ( updated_at >= ? ) ");
                args.add(new Date(akc.getFrom()));
            }
            if (akc.getTo() > 0) {
                first = addClause(first, query);
                query.append(" ( updated_at <= ? ) ");
                args.add(new Date(akc.getTo()));
            }
            if (akc.getExpireAfter() > 0) {
                first = addClause(first, query);
                query.append(" ( expire_at >= ? ) ");
                args.add(new Date(akc.getExpireAfter()));
            }
            if (akc.getExpireBefore() > 0) {
                addClause(first, query);
                query.append(" ( expire_at <= ? ) ");
                args.add(new Date(akc.getExpireBefore()));
            }
            query.append(" order by updated_at desc ");
            return jdbcTemplate.query(query.toString(), args.toArray(), getOrm().getRowMapper());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find api keys by criteria:", ex);
            throw new TechnicalException("Failed to find api keys by criteria", ex);
        }
    }

    private boolean addClause(boolean first, StringBuilder query) {
        if (first) {
            query.append(" where ");
        } else {
            query.append(" and ");
        }
        return false;
    }

    @Override
    public Set<ApiKey> findBySubscription(String subscription) throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.findBySubscription({})", subscription);
        try {
            List<ApiKey> apiKeys = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where subscription = ?",
                getOrm().getRowMapper(),
                subscription
            );
            return new HashSet<>(apiKeys);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find api keys by subscription:", ex);
            throw new TechnicalException("Failed to find api keys by subscription", ex);
        }
    }

    @Override
    public Set<ApiKey> findByPlan(String plan) throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.findByPlan({})", plan);
        try {
            List<ApiKey> items = jdbcTemplate.query(getOrm().getSelectAllSql() + " where plan = ?", getOrm().getRowMapper(), plan);
            return new HashSet<>(items);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find api keys by plan:", ex);
            throw new TechnicalException("Failed to find api keys by plan", ex);
        }
    }
}
