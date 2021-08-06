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
package io.gravitee.rest.api.services.dynamicproperties.provider.http;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.utils.UUID;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.utils.NodeUtils;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.services.dynamicproperties.model.DynamicProperty;
import io.gravitee.rest.api.services.dynamicproperties.provider.Provider;
import io.gravitee.rest.api.services.dynamicproperties.provider.http.mapper.JoltMapper;
import io.gravitee.rest.api.services.dynamicproperties.provider.http.vertx.VertxCompletableFuture;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpProvider implements Provider {

    private final Logger logger = LoggerFactory.getLogger(HttpProvider.class);

    private static final String HTTPS_SCHEME = "https";

    private final HttpDynamicPropertyProviderConfiguration dpConfiguration;

    private JoltMapper mapper;

    private Vertx vertx;

    private Node node;

    public HttpProvider(final DynamicPropertyService dpService) {
        Objects.requireNonNull(dpService, "Service must not be null");

        this.dpConfiguration = (HttpDynamicPropertyProviderConfiguration) dpService.getConfiguration();
        this.mapper = new JoltMapper(dpConfiguration.getSpecification());
    }

    @Override
    public CompletableFuture<Collection<DynamicProperty>> get() {
        CompletableFuture<Buffer> future = new VertxCompletableFuture<>(vertx);

        URI requestUri = URI.create(dpConfiguration.getUrl());
        boolean ssl = HTTPS_SCHEME.equalsIgnoreCase(requestUri.getScheme());

        final HttpClientOptions options = new HttpClientOptions()
            .setSsl(ssl)
            .setTrustAll(true)
            .setMaxPoolSize(1)
            .setKeepAlive(false)
            .setTcpKeepAlive(false)
            .setConnectTimeout(2000);

        final HttpClient httpClient = vertx.createHttpClient(options);

        final int port = requestUri.getPort() != -1 ? requestUri.getPort() : (HTTPS_SCHEME.equals(requestUri.getScheme()) ? 443 : 80);

        try {
            HttpClientRequest request = httpClient.request(HttpMethod.GET, port, requestUri.getHost(), requestUri.toString());

            request.putHeader(HttpHeaders.USER_AGENT, NodeUtils.userAgent(node));
            request.putHeader("X-Gravitee-Request-Id", RandomString.generate());

            request.handler(
                response -> {
                    if (response.statusCode() == HttpStatusCode.OK_200) {
                        response.bodyHandler(
                            buffer -> {
                                future.complete(buffer);

                                // Close client
                                httpClient.close();
                            }
                        );
                    } else {
                        future.complete(null);

                        // Close client
                        httpClient.close();
                    }
                }
            );

            request.exceptionHandler(
                event -> {
                    try {
                        future.completeExceptionally(event);

                        // Close client
                        httpClient.close();
                    } catch (IllegalStateException ise) {
                        // Do not take care about exception when closing client
                    }
                }
            );

            request.end();
        } catch (Exception ex) {
            logger.error("Unable to look for dynamic properties", ex);
            future.completeExceptionally(ex);

            // Close client
            httpClient.close();
        }

        return future.thenApply(
            buffer -> {
                if (buffer == null) {
                    return null;
                }
                return mapper.map(buffer.toString());
            }
        );
    }

    @Override
    public String name() {
        return "custom";
    }

    public void setMapper(JoltMapper mapper) {
        this.mapper = mapper;
    }

    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}
