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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Category;
import io.gravitee.repository.mongodb.management.internal.api.CategoryMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.CategoryMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoCategoryRepository implements CategoryRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoCategoryRepository.class);

    @Autowired
    private CategoryMongoRepository internalCategoryRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Category> findById(String categoryId) throws TechnicalException {
        LOGGER.debug("Find category by ID [{}]", categoryId);

        final CategoryMongo category = internalCategoryRepo.findById(categoryId).orElse(null);

        LOGGER.debug("Find category by ID [{}] - Done", categoryId);
        return Optional.ofNullable(mapper.map(category, Category.class));
    }

    @Override
    public Optional<Category> findByKey(String key, String environment) throws TechnicalException {
        LOGGER.debug("Find category by key [{}, {}]", key, environment);

        final CategoryMongo category = internalCategoryRepo.findByKeyAndEnvironment(key, environment).orElse(null);

        LOGGER.debug("Find category by key [{}, {}] - Done", key, environment);
        return Optional.ofNullable(mapper.map(category, Category.class));
    }

    @Override
    public Category create(Category category) throws TechnicalException {
        LOGGER.debug("Create category [{}]", category.getName());

        CategoryMongo categoryMongo = mapper.map(category, CategoryMongo.class);
        CategoryMongo createdCategoryMongo = internalCategoryRepo.insert(categoryMongo);

        Category res = mapper.map(createdCategoryMongo, Category.class);

        LOGGER.debug("Create category [{}] - Done", category.getName());

        return res;
    }

    @Override
    public Category update(Category category) throws TechnicalException {
        if (category == null || category.getName() == null) {
            throw new IllegalStateException("Category to update must have a name");
        }

        final CategoryMongo categoryMongo = internalCategoryRepo.findById(category.getId()).orElse(null);

        if (categoryMongo == null) {
            throw new IllegalStateException(String.format("No category found with name [%s]", category.getId()));
        }

        try {
            CategoryMongo categoryMongoUpdated = internalCategoryRepo.save(mapper.map(category, CategoryMongo.class));
            return mapper.map(categoryMongoUpdated, Category.class);
        } catch (Exception e) {
            LOGGER.error("An error occured when updating category", e);
            throw new TechnicalException("An error occured when updating category");
        }
    }

    @Override
    public void delete(String categoryId) throws TechnicalException {
        try {
            internalCategoryRepo.deleteById(categoryId);
        } catch (Exception e) {
            LOGGER.error("An error occured when deleting category [{}]", categoryId, e);
            throw new TechnicalException("An error occured when deleting category");
        }
    }

    @Override
    public Set<Category> findAll() throws TechnicalException {
        final List<CategoryMongo> categories = internalCategoryRepo.findAll();
        return categories.stream().map(categoryMongo -> mapper.map(categoryMongo, Category.class)).collect(Collectors.toSet());
    }

    @Override
    public Set<Category> findAllByEnvironment(String environmentId) throws TechnicalException {
        final List<CategoryMongo> categories = internalCategoryRepo.findByEnvironmentId(environmentId);
        return categories.stream().map(categoryMongo -> mapper.map(categoryMongo, Category.class)).collect(Collectors.toSet());
    }
}
