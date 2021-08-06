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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.PageConfigurationKeys;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.portal.rest.model.Metadata;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.Page.TypeEnum;
import io.gravitee.rest.api.portal.rest.model.PageConfiguration;
import io.gravitee.rest.api.portal.rest.model.PageConfiguration.DocExpansionEnum;
import io.gravitee.rest.api.portal.rest.model.PageConfiguration.ViewerEnum;
import io.gravitee.rest.api.portal.rest.model.PageLinks;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PageMapper {

    public Page convert(PageEntity page) {
        final Page pageItem = new Page();

        if (page.getConfiguration() != null) {
            PageConfiguration pageConfiguration = convertPageConfiguration(page.getConfiguration());
            pageItem.setConfiguration(pageConfiguration);
        }
        pageItem.setId(page.getId());

        if (page.getMetadata() != null) {
            AtomicInteger counter = new AtomicInteger(0);
            List<Metadata> metadataList = page
                .getMetadata()
                .entrySet()
                .stream()
                .map(e -> new Metadata().name(e.getKey()).value(e.getValue()).order(Integer.toString(counter.getAndIncrement())))
                .collect(Collectors.toList());
            pageItem.setMetadata(metadataList);
        }
        pageItem.setName(page.getName());
        pageItem.setOrder(page.getOrder());
        pageItem.setParent(page.getParentId());
        if (page.getType() != null) {
            pageItem.setType(TypeEnum.fromValue(page.getType()));
        }
        if (page.getLastModificationDate() != null) {
            pageItem.setUpdatedAt(page.getLastModificationDate().toInstant().atOffset(ZoneOffset.UTC));
        }

        return pageItem;
    }

    private PageConfiguration convertPageConfiguration(Map<String, String> configuration) {
        PageConfiguration pageConfiguration = new PageConfiguration();
        String displayOperationId = configuration.get(PageConfigurationKeys.SWAGGER_SWAGGERUI_DISPLAY_OPERATION_ID);
        String docExpansion = configuration.get(PageConfigurationKeys.SWAGGER_SWAGGERUI_DOC_EXPANSION);
        String enableFiltering = configuration.get(PageConfigurationKeys.SWAGGER_SWAGGERUI_ENABLE_FILTERING);
        String maxDisplayedTags = configuration.get(PageConfigurationKeys.SWAGGER_SWAGGERUI_MAX_DISPLAYED_TAGS);
        String showCommonExtensions = configuration.get(PageConfigurationKeys.SWAGGER_SWAGGERUI_SHOW_COMMON_EXTENSIONS);
        String showExtensions = configuration.get(PageConfigurationKeys.SWAGGER_SWAGGERUI_SHOW_EXTENSIONS);
        String showUrl = configuration.get(PageConfigurationKeys.SWAGGER_SWAGGERUI_SHOW_URL);
        String tryIt = configuration.get(PageConfigurationKeys.SWAGGER_SWAGGERUI_TRY_IT);
        String tryItAnonymous = configuration.get(PageConfigurationKeys.SWAGGER_SWAGGERUI_TRY_IT_ANONYMOUS);
        String tryItURL = configuration.get(PageConfigurationKeys.SWAGGER_SWAGGERUI_TRY_IT_URL);
        String viewer = configuration.get(PageConfigurationKeys.SWAGGER_VIEWER);

        if (displayOperationId != null) {
            pageConfiguration.setDisplayOperationId(Boolean.parseBoolean(displayOperationId));
        }
        if (docExpansion != null) {
            pageConfiguration.setDocExpansion(DocExpansionEnum.fromValue(docExpansion));
        }
        if (enableFiltering != null) {
            pageConfiguration.setEnableFiltering(Boolean.parseBoolean(enableFiltering));
        }
        if (maxDisplayedTags != null) {
            pageConfiguration.setMaxDisplayedTags(Integer.parseInt(maxDisplayedTags));
        }
        if (showCommonExtensions != null) {
            pageConfiguration.setShowCommonExtensions(Boolean.parseBoolean(showCommonExtensions));
        }
        if (showExtensions != null) {
            pageConfiguration.setShowExtensions(Boolean.parseBoolean(showExtensions));
        }
        if (showUrl != null) {
            pageConfiguration.setShowUrl(showUrl);
        }
        if (tryIt != null) {
            pageConfiguration.setTryIt(Boolean.parseBoolean(tryIt));
        }
        if (tryItAnonymous != null) {
            pageConfiguration.setTryItAnonymous(Boolean.parseBoolean(tryItAnonymous));
        }
        if (tryItURL != null) {
            pageConfiguration.setTryItUrl(tryItURL);
        }
        if (viewer != null) {
            pageConfiguration.setViewer(ViewerEnum.fromValue(viewer));
        }
        return pageConfiguration;
    }

    public PageLinks computePageLinks(String basePath, String parentPath) {
        PageLinks pageLinks = new PageLinks();
        pageLinks.setContent(basePath + "/content");
        pageLinks.setParent(parentPath);
        pageLinks.setSelf(basePath);

        return pageLinks;
    }
}
