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
package io.gravitee.repository.config.mock;

import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Category;
import io.gravitee.repository.management.model.Category;

import java.util.Date;
import java.util.Set;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.collections.Sets.newSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CategoryRepositoryMock extends AbstractRepositoryMock<CategoryRepository> {

    public CategoryRepositoryMock() {
        super(CategoryRepository.class);
    }

    @Override
    void prepare(CategoryRepository categoryRepository) throws Exception {
        final Category newCategory = mock(Category.class);
        when(newCategory.getId()).thenReturn("fd19297e-01a3-4828-9929-7e01a3782809");
        when(newCategory.getKey()).thenReturn("new-category");
        when(newCategory.getName()).thenReturn("Category name");
        when(newCategory.getEnvironmentId()).thenReturn("DEFAULT");
        when(newCategory.getDescription()).thenReturn("Description for the new category");
        when(newCategory.getCreatedAt()).thenReturn(new Date(1486771200000L));
        when(newCategory.getUpdatedAt()).thenReturn(new Date(1486771200000L));
        when(newCategory.isHidden()).thenReturn(true);
        when(newCategory.getOrder()).thenReturn(1);
        when(newCategory.getPicture()).thenReturn("New picture");

        final Category categoryProducts = new Category();
        categoryProducts.setId("category");
        categoryProducts.setEnvironmentId("DEFAULT");
        categoryProducts.setName("Products");
        categoryProducts.setCreatedAt(new Date(1000000000000L));
        categoryProducts.setUpdatedAt(new Date(1111111111111L));
        categoryProducts.setHidden(false);
        categoryProducts.setOrder(1);

        final Category categoryProductsUpdated = mock(Category.class);
        when(categoryProductsUpdated.getName()).thenReturn("New product");
        when(categoryProductsUpdated.getEnvironmentId()).thenReturn("DEFAULT");
        when(categoryProductsUpdated.getDescription()).thenReturn("New description");
        when(categoryProductsUpdated.getCreatedAt()).thenReturn(new Date(1486771200000L));
        when(categoryProductsUpdated.getUpdatedAt()).thenReturn(new Date(1486771200000L));
        when(categoryProductsUpdated.isHidden()).thenReturn(true);
        when(categoryProductsUpdated.getOrder()).thenReturn(10);
        when(categoryProductsUpdated.getHighlightApi()).thenReturn("new Highlighted API");
        when(categoryProductsUpdated.getPicture()).thenReturn("New picture");

        final Category myCategory = new Category();
        myCategory.setId("123");
        myCategory.setKey("my-category");
        myCategory.setName("My category");
        myCategory.setCreatedAt(new Date(1000000000000L));
        myCategory.setUpdatedAt(new Date(1111111111111L));
        myCategory.setHidden(false);
        myCategory.setOrder(3);

        final Set<Category> categories = newSet(newCategory, categoryProducts, mock(Category.class), myCategory);
        final Set<Category> categoriesAfterDelete = newSet(newCategory, categoryProducts, myCategory);
        final Set<Category> categoriesAfterAdd = newSet(newCategory, categoryProducts, mock(Category.class), mock(Category.class), myCategory);

        when(categoryRepository.findAll()).thenReturn(categories, categoriesAfterAdd, categories, categoriesAfterDelete, categories);
        when(categoryRepository.findAllByEnvironment("DEFAULT")).thenReturn(categories);

        when(categoryRepository.create(any(Category.class))).thenReturn(newCategory);

        when(categoryRepository.findById("fd19297e-01a3-4828-9929-7e01a3782809")).thenReturn(of(newCategory));
        when(categoryRepository.findById("unknown")).thenReturn(empty());
        when(categoryRepository.findById("products")).thenReturn(of(categoryProducts), of(categoryProductsUpdated));
        when(categoryRepository.findByKey("my-category", "DEFAULT")).thenReturn(of(myCategory));

        when(categoryRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());
    }
}
