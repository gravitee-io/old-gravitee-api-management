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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.AlertEventRepository;
import io.gravitee.repository.management.api.search.AlertEventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.AlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static io.gravitee.repository.jdbc.management.JdbcHelper.AND_CLAUSE;
import static io.gravitee.repository.jdbc.management.JdbcHelper.WHERE_CLAUSE;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcAlertEventRepository extends JdbcAbstractCrudRepository<AlertEvent, String> implements AlertEventRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(JdbcAlertEventRepository.class);

    private static final JdbcObjectMapper<AlertEvent> ORM = JdbcObjectMapper.builder(AlertEvent.class, "alert_events", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("alert", Types.NVARCHAR, String.class)
            .addColumn("message", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(final AlertEvent event) {
        return event.getId();
    }

    @Override
    public Page<AlertEvent> search(AlertEventCriteria criteria, Pageable pageable) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("JdbcAlertEventRepository.search({})", criteriaToString(criteria));
        }

        final List<Object> args = new ArrayList<>();
        final StringBuilder builder = new StringBuilder("select ev.* from alert_events ev ");

        boolean started = false;
        if (criteria.getFrom() > 0) {
            builder.append(WHERE_CLAUSE);
            builder.append("created_at >= ?");
            args.add(new Date(criteria.getFrom()));
            started = true;
        }
        if (criteria.getTo() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("created_at < ?");
            args.add(new Date(criteria.getTo()));
            started = true;
        }

        if (criteria.getAlert() != null && ! criteria.getAlert().isEmpty()) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("alert = ?");
            args.add(criteria.getAlert());
        }

        builder.append(" order by created_at desc ");
        String sql = builder.toString();
        LOGGER.debug("SQL: {}", sql);
        LOGGER.debug("Args: {}", args);

        List<AlertEvent> events = jdbcTemplate.query((Connection cnctn) -> {
            PreparedStatement stmt = cnctn.prepareStatement(sql);
            int idx = 1;
            for (final Object arg : args) {
                if (arg instanceof Date) {
                    final Date date = (Date) arg;
                    stmt.setTimestamp(idx++, new Timestamp(date.getTime()));
                } else {
                    stmt.setObject(idx++, arg);
                }
            }
            return stmt;
        }, ORM.getRowMapper());

        LOGGER.debug("Alert events found: {}", events);

        return getResultAsPage(pageable, events);
    }

    @Override
    public void deleteAll(String alertId) {
        jdbcTemplate.update("delete from alert_events where alert = ?", alertId);
    }

    private String criteriaToString(AlertEventCriteria criteria) {
        return "{ " + "from: " + criteria.getFrom() +
                ", " + "alert: " + criteria.getAlert() +
                ", " + "to: " + criteria.getTo() +
                " }";
    }
}