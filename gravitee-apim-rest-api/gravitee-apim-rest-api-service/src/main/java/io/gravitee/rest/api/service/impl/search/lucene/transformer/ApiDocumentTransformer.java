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
package io.gravitee.rest.api.service.impl.search.lucene.transformer;

import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import org.apache.lucene.document.*;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiDocumentTransformer implements DocumentTransformer<ApiEntity> {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_TYPE_VALUE = "api";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_NAME_LOWERCASE = "name_lowercase";
    public static final String FIELD_NAME_SPLIT = "name_split";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_DESCRIPTION_LOWERCASE = "description_lowercase";
    public static final String FIELD_DESCRIPTION_SPLIT = "description_split";
    public static final String FIELD_OWNER = "ownerName";
    public static final String FIELD_OWNER_LOWERCASE = "ownerName_lowercase";
    public static final String FIELD_OWNER_MAIL = "ownerMail";
    public static final String FIELD_LABELS = "labels";
    public static final String FIELD_LABELS_LOWERCASE = "labels_lowercase";
    public static final String FIELD_LABELS_SPLIT = "labels_split";
    public static final String FIELD_CATEGORIES = "categories";
    public static final String FIELD_CATEGORIES_SPLIT = "categories_split";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";
    public static final String FIELD_PATHS = "paths";
    public static final String FIELD_HOSTS = "hosts";
    public static final String FIELD_PATHS_SPLIT = "paths_split";
    public static final String FIELD_HOSTS_SPLIT = "hosts_split";
    public static final String FIELD_TAGS = "tags";
    public static final String FIELD_TAGS_SPLIT = "tags_split";
    public static final String FIELD_METADATA = "metadata";
    public static final String FIELD_METADATA_SPLIT = "metadata_split";

    @Override
    public Document transform(io.gravitee.rest.api.model.api.ApiEntity api) {
        Document doc = new Document();

        doc.add(new StringField(FIELD_REFERENCE_TYPE, api.getReferenceType(), Field.Store.NO));
        doc.add(new StringField(FIELD_REFERENCE_ID, api.getReferenceId(), Field.Store.NO));
        doc.add(new StringField(FIELD_ID, api.getId(), Field.Store.YES));
        doc.add(new StringField(FIELD_TYPE, FIELD_TYPE_VALUE, Field.Store.YES));
        if (api.getName() != null) {
            doc.add(new StringField(FIELD_NAME, api.getName(), Field.Store.NO));
            doc.add(new StringField(FIELD_NAME_LOWERCASE, api.getName().toLowerCase(), Field.Store.NO));
            doc.add(new TextField(FIELD_NAME_SPLIT, api.getName(), Field.Store.NO));
        }
        if (api.getDescription() != null) {
            doc.add(new StringField(FIELD_DESCRIPTION, api.getDescription(), Field.Store.NO));
            doc.add(new StringField(FIELD_DESCRIPTION_LOWERCASE, api.getDescription().toLowerCase(), Field.Store.NO));
            doc.add(new TextField(FIELD_DESCRIPTION_SPLIT, api.getDescription(), Field.Store.NO));
        }
        if (api.getPrimaryOwner() != null) {
            doc.add(new TextField(FIELD_OWNER, api.getPrimaryOwner().getDisplayName(), Field.Store.NO));
            doc.add(new StringField(FIELD_OWNER_LOWERCASE, api.getPrimaryOwner().getDisplayName().toLowerCase(), Field.Store.NO));
            if (api.getPrimaryOwner().getEmail() != null) {
                doc.add(new TextField(FIELD_OWNER_MAIL, api.getPrimaryOwner().getEmail(), Field.Store.NO));
            }
        }

        if (api.getProxy() != null) {
            api
                .getProxy()
                .getVirtualHosts()
                .forEach(
                    virtualHost -> {
                        doc.add(new StringField(FIELD_PATHS, virtualHost.getPath(), Field.Store.NO));
                        doc.add(new TextField(FIELD_PATHS_SPLIT, virtualHost.getPath(), Field.Store.NO));
                        if (virtualHost.getHost() != null && !virtualHost.getHost().isEmpty()) {
                            doc.add(new StringField(FIELD_HOSTS, virtualHost.getHost(), Field.Store.NO));
                            doc.add(new TextField(FIELD_HOSTS_SPLIT, virtualHost.getHost(), Field.Store.NO));
                        }
                    }
                );
        }

        // labels
        if (api.getLabels() != null) {
            for (String label : api.getLabels()) {
                doc.add(new StringField(FIELD_LABELS, label, Field.Store.NO));
                doc.add(new StringField(FIELD_LABELS_LOWERCASE, label.toLowerCase(), Field.Store.NO));
                doc.add(new TextField(FIELD_LABELS_SPLIT, label, Field.Store.NO));
            }
        }

        // categories
        if (api.getCategories() != null) {
            for (String category : api.getCategories()) {
                doc.add(new StringField(FIELD_CATEGORIES, category, Field.Store.NO));
                doc.add(new TextField(FIELD_CATEGORIES_SPLIT, category, Field.Store.NO));
            }
        }

        // tags
        if (api.getTags() != null) {
            for (String tag : api.getTags()) {
                doc.add(new StringField(FIELD_TAGS, tag, Field.Store.NO));
                doc.add(new TextField(FIELD_TAGS_SPLIT, tag, Field.Store.NO));
            }
        }

        if (api.getCreatedAt() != null) {
            doc.add(new LongPoint(FIELD_CREATED_AT, api.getCreatedAt().getTime()));
        }
        if (api.getUpdatedAt() != null) {
            doc.add(new LongPoint(FIELD_UPDATED_AT, api.getUpdatedAt().getTime()));
        }

        // metadata
        if (api.getMetadata() != null) {
            api
                .getMetadata()
                .values()
                .forEach(
                    metadataValue -> {
                        doc.add(new StringField(FIELD_METADATA, metadataValue.toString(), Field.Store.NO));
                        doc.add(new TextField(FIELD_METADATA_SPLIT, metadataValue.toString(), Field.Store.NO));
                    }
                );
        }

        return doc;
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return ApiEntity.class.isAssignableFrom(source);
    }
}
