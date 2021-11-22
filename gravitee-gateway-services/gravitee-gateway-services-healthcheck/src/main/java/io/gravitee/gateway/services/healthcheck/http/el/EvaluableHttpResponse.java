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
package io.gravitee.gateway.services.healthcheck.http.el;

import io.gravitee.common.http.HttpHeaders;
import io.vertx.core.http.HttpClientResponse;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class EvaluableHttpResponse {

    private final int statusCode;
    private final String content;
    private final HttpHeaders httpHeaders = new HttpHeaders();

    public EvaluableHttpResponse(final HttpClientResponse response, final String content) {
        this.statusCode = response.statusCode();
        this.content = content;

        // Copy HTTP headers
        response.headers().names().forEach(headerName ->
                httpHeaders.put(headerName, response.headers().getAll(headerName)));
    }

    public int getStatus() {
        return statusCode;
    }

    public String getContent() {
        return content;
    }

    public HttpHeaders getHeaders() {
        return httpHeaders;
    }
}
