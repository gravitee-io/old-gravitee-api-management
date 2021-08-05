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
package io.gravitee.rest.api.service;

import io.gravitee.fetcher.api.FetcherConfiguration;
import io.gravitee.fetcher.api.FetcherException;
import io.gravitee.fetcher.api.FilesFetcher;
import io.gravitee.fetcher.api.Resource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageService_ImportSimplePageMockFetcher implements FilesFetcher {

    public PageService_ImportSimplePageMockFetcher(PageService_MockSinglePageFetcherConfiguration cfg) {
        super();
    }

    @Override
    public Resource fetch() throws FetcherException {
        Resource resource = new Resource();
        resource.setContent(new ByteArrayInputStream("Sample".getBytes()));
        resource.setMetadata(Collections.emptyMap());
        return resource;
    }

    @Override
    public String[] files() throws FetcherException {
        return new String[] { "poc.md" };
    }

    @Override
    public FetcherConfiguration getConfiguration() {
        return new PageService_MockSinglePageFetcherConfiguration();
    }
}
