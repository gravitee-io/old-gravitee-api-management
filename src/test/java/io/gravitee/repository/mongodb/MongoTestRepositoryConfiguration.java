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
package io.gravitee.repository.mongodb;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.distribution.Version;
import io.gravitee.repository.mongodb.common.AbstractRepositoryConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ComponentScan("io.gravitee.repository.mongodb.management")
@EnableMongoRepositories
public class MongoTestRepositoryConfiguration extends AbstractRepositoryConfiguration {

    @Bean
    public MongoTestFactory factory() throws Exception {
        return MongoTestFactory.with(Version.Main.V3_5);
    }

    @Override
    protected String getDatabaseName() {
        return "gravitee";
    }

    @Override
    public MongoClient mongoClient() {
        try {
            return factory().newMongo();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean(name = "managementMongoTemplate")
    public MongoOperations mongoOperations() {
        return new MongoTemplate(mongoClient(), "gravitee");
    }
}
