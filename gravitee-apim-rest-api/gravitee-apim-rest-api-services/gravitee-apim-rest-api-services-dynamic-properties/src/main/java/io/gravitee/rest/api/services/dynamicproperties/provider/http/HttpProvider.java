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
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.utils.NodeUtils;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.services.dynamicproperties.model.DynamicProperty;
import io.gravitee.rest.api.services.dynamicproperties.provider.Provider;
import io.gravitee.rest.api.services.dynamicproperties.provider.http.mapper.JoltMapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpProvider implements Provider {

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
        Promise<Buffer> promise = Promise.promise();

        URI requestUri = URI.create(dpConfiguration.getUrl());
        boolean ssl = HTTPS_SCHEME.equalsIgnoreCase(requestUri.getScheme());

        final HttpClientOptions clientOptions = new HttpClientOptions()
            .setSsl(ssl)
            .setTrustAll(true)
            .setMaxPoolSize(1)
            .setKeepAlive(false)
            .setTcpKeepAlive(false)
            .setConnectTimeout(2000);

        final HttpClient httpClient = vertx.createHttpClient(clientOptions);

        final int port = requestUri.getPort() != -1 ? requestUri.getPort() : (HTTPS_SCHEME.equals(requestUri.getScheme()) ? 443 : 80);

        String relativeUri = (requestUri.getRawQuery() == null)
            ? requestUri.getRawPath()
            : requestUri.getRawPath() + '?' + requestUri.getRawQuery();

        RequestOptions options = new RequestOptions()
            .setMethod(HttpMethod.valueOf(dpConfiguration.getMethod().name()))
            .setHost(requestUri.getHost())
            .setPort(port)
            .setURI(relativeUri);

        //headers
        options.putHeader(HttpHeaders.USER_AGENT, NodeUtils.userAgent(node));
        options.putHeader("X-Gravitee-Request-Id", UuidString.generateRandom());

        if (dpConfiguration.getHeaders() != null) {
            dpConfiguration.getHeaders().forEach(httpHeader -> options.putHeader(httpHeader.getName(), httpHeader.getValue()));
        }

        httpClient
            .request(options)
            .onFailure(
                new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable event) {
                        promise.fail(event);

                        // Close client
                        httpClient.close();
                    }
                }
            )
            .onSuccess(
                new Handler<HttpClientRequest>() {
                    @Override
                    public void handle(HttpClientRequest request) {
                        request
                            .response(
                                new Handler<AsyncResult<HttpClientResponse>>() {
                                    @Override
                                    public void handle(AsyncResult<HttpClientResponse> asyncResponse) {
                                        if (asyncResponse.failed()) {
                                            promise.fail(asyncResponse.cause());

                                            // Close client
                                            httpClient.close();
                                        } else {
                                            final HttpClientResponse response = asyncResponse.result();

                                            if (response.statusCode() == HttpStatusCode.OK_200) {
                                                response.bodyHandler(
                                                    buffer -> {
                                                        promise.complete(buffer);

                                                        // Close client
                                                        httpClient.close();
                                                    }
                                                );
                                            } else {
                                                promise.complete(null);

                                                // Close client
                                                httpClient.close();
                                            }
                                        }
                                    }
                                }
                            )
                            .exceptionHandler(
                                new Handler<Throwable>() {
                                    @Override
                                    public void handle(Throwable throwable) {
                                        promise.fail(throwable);

                                        // Close client
                                        httpClient.close();
                                    }
                                }
                            );

                        if (!StringUtils.isEmpty(dpConfiguration.getBody())) {
                            request.end(dpConfiguration.getBody());
                        } else {
                            request.end();
                        }
                    }
                }
            );

        return promise
            .future()
            .map(
                new Function<Buffer, Collection<DynamicProperty>>() {
                    @Override
                    public Collection<DynamicProperty> apply(Buffer buffer) {
                        if (buffer == null) {
                            return null;
                        }
                        return mapper.map(buffer.toString());
                    }
                }
            )
            .toCompletionStage()
            .toCompletableFuture();
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
