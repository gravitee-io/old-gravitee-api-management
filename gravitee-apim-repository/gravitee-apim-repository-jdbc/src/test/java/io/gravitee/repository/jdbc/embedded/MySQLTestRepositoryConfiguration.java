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
package io.gravitee.repository.jdbc.embedded;

import static com.wix.mysql.EmbeddedMysql.anEmbeddedMysql;
import static com.wix.mysql.distribution.Version.v5_7_latest;
import static java.lang.String.format;

import com.wix.mysql.EmbeddedMysql;
import com.wix.mysql.config.MysqldConfig;
import io.gravitee.repository.jdbc.AbstractJdbcTestRepositoryConfiguration;
import javax.inject.Inject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Conditional(MySQLCondition.class)
public class MySQLTestRepositoryConfiguration extends AbstractJdbcTestRepositoryConfiguration {

    @Inject
    private EmbeddedMysql embeddedMysql;

    @Override
    protected String getJdbcUrl() {
        final MysqldConfig config = embeddedMysql.getConfig();
        return format(
            "jdbc:mysql://localhost:%s/gravitee?useSSL=false&user=%s&password=%s",
            config.getPort(),
            config.getUsername(),
            config.getPassword()
        );
    }

    @Bean(destroyMethod = "stop")
    public EmbeddedMysql embeddedMysql() {
        return anEmbeddedMysql(v5_7_latest).addSchema("gravitee").start();
    }
}
