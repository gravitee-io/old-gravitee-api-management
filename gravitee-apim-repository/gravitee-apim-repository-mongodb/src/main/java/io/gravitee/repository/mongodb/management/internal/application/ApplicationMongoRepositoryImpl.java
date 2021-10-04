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
package io.gravitee.repository.mongodb.management.internal.application;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.mongodb.management.internal.model.ApplicationMongo;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationMongoRepositoryImpl implements ApplicationMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<ApplicationMongo> search(final ApplicationCriteria criteria, final Pageable pageable) {
        final Query query = new Query();

        query.fields().exclude("background");
        query.fields().exclude("picture");

        if (criteria != null) {
            if (criteria.getIds() != null && !criteria.getIds().isEmpty()) {
                query.addCriteria(where("id").in(criteria.getIds()));
            }
            if (criteria.getEnvironmentId() != null) {
                query.addCriteria(where("environmentId").is(criteria.getEnvironmentId()));
            }
            if (criteria.getName() != null && !criteria.getName().isEmpty()) {
                query.addCriteria(where("name").regex(criteria.getName(), "i"));
            }
            if (criteria.getStatus() != null) {
                query.addCriteria(where("status").is(criteria.getStatus()));
            }
        }

        query.with(new Sort(ASC, "name"));

        if (pageable != null) {
            query.with(new PageRequest(pageable.pageNumber(), pageable.pageSize()));
        }

        List<ApplicationMongo> apps = mongoTemplate.find(query, ApplicationMongo.class);

        long total = mongoTemplate.count(query, ApplicationMongo.class);

        return new Page<>(apps, pageable != null ? pageable.pageNumber() : 0, pageable != null ? pageable.pageSize() : 0, total);
    }
}
