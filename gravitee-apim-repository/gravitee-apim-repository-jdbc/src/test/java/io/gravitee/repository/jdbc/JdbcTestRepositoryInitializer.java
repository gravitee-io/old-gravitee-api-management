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
package io.gravitee.repository.jdbc;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;

import io.gravitee.repository.config.TestRepositoryInitializer;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 *
 * @author njt
 */
@Component
public class JdbcTestRepositoryInitializer implements TestRepositoryInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTestRepositoryInitializer.class);

    private final DataSource dataSource;

    private final String prefix;
    private final String rateLimitPrefix;

    private static final List<String> tablesToTruncate = Arrays.asList(
        "apis",
        "keys",
        "api_groups",
        "api_headers",
        "api_labels",
        "api_tags",
        "api_categories",
        "applications",
        "application_groups",
        "application_metadata",
        "audits",
        "audit_properties",
        "commands",
        "command_acknowledgments",
        "command_tags",
        "client_registration_providers",
        "client_registration_provider_scopes",
        "custom_user_fields",
        "custom_user_fields_values",
        "dictionaries",
        "dictionary_property",
        "environments",
        "environment_hrids",
        "environment_domain_restrictions",
        "events",
        "event_properties",
        "event_environments",
        "generic_notification_configs",
        "generic_notification_config_hooks",
        "groups",
        "group_event_rules",
        "identity_providers",
        "identity_provider_activations",
        "installation",
        "installation_informations",
        "memberships",
        "metadata",
        "media",
        "notification_templates",
        "organizations",
        "organization_hrids",
        "organization_domain_restrictions",
        "parameters",
        "pages",
        "page_acl",
        "page_attached_media",
        "page_configuration",
        "page_revisions",
        "plans",
        "plan_characteristics",
        "plan_excluded_groups",
        "plan_tags",
        "portal_notifications",
        "portal_notification_configs",
        "portal_notification_config_hooks",
        "ratings",
        "rating_answers",
        "roles",
        "role_permissions",
        "subscriptions",
        "tags",
        "tickets",
        "tenants",
        "users",
        "categories",
        "alert_triggers",
        "entrypoints",
        "page_metadata",
        "invitations",
        "tag_groups",
        "workflows",
        "quality_rules",
        "api_quality_rules",
        "dashboards",
        "alert_events",
        "themes",
        "alert_event_rules",
        "tokens",
        "node_monitoring",
        "flows",
        "flow_steps",
        "flow_methods",
        "flow_consumers"
    );

    private static final List<String> tablesToDrop = concatenate(
        tablesToTruncate,
        Arrays.asList("databasechangelog", "databasechangeloglock")
    );

    private static <T> List<T> concatenate(List<T> first, List<T> second) {
        final List result = new ArrayList<>(first.size() + second.size());
        result.addAll(first);
        result.addAll(second);
        return result;
    }

    @Autowired
    public JdbcTestRepositoryInitializer(DataSource dataSource, Properties graviteeProperties) {
        LOGGER.debug("Constructed");
        this.dataSource = dataSource;
        this.prefix = graviteeProperties.getProperty("management.jdbc.prefix", "");
        this.rateLimitPrefix = graviteeProperties.getProperty("ratelimit.jdbc.prefix", "");
        final JdbcTemplate jt = new JdbcTemplate(dataSource);
        for (String table : tablesToDrop) {
            LOGGER.debug("Dropping {}", table);
            jt.execute("drop table if exists " + escapeReservedWord(prefix + table));
        }
        jt.execute("drop table if exists " + escapeReservedWord(rateLimitPrefix + "ratelimit"));
    }

    @Override
    public void setUp() {
        LOGGER.debug("setUp");
        LOGGER.debug("Running Liquibase on {}", dataSource);

        System.setProperty("liquibase.databaseChangeLogTableName", prefix + "databasechangelog");
        System.setProperty("liquibase.databaseChangeLogLockTableName", prefix + "databasechangeloglock");
        System.setProperty("gravitee_prefix", prefix);
        System.setProperty("gravitee_rate_limit_prefix", rateLimitPrefix);

        try (final Connection conn = dataSource.getConnection()) {
            final Liquibase liquibase = new Liquibase(
                "liquibase/master.yml",
                new ClassLoaderResourceAccessor(this.getClass().getClassLoader()),
                new JdbcConnection(conn)
            );
            liquibase.setIgnoreClasspathPrefix(true);
            liquibase.update((Contexts) null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to set up database: ", ex);
        }
    }

    @Override
    public void tearDown() {
        LOGGER.debug("tearDown");
        final JdbcTemplate jt = new JdbcTemplate(dataSource);
        System.clearProperty("liquibase.databaseChangeLogTableName");
        System.clearProperty("liquibase.databaseChangeLogLockTableName");
        System.clearProperty("gravitee_prefix");
        jt.execute(
            (Connection con) -> {
                for (final String table : tablesToTruncate) {
                    jt.execute("truncate table " + escapeReservedWord(prefix + table));
                }
                jt.execute("truncate table " + escapeReservedWord(rateLimitPrefix + "ratelimit"));
                return null;
            }
        );
    }
}
