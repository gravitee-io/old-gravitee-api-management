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
package io.gravitee.rest.api.service.impl;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.vertx.VertxCompletableFuture;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpClientServiceImpl extends AbstractService implements HttpClientService {

    private final Logger LOGGER = LoggerFactory.getLogger(HttpClientServiceImpl.class);

    private static final String HTTPS_SCHEME = "https";

    @Value("${httpClient.timeout:10000}")
    private int httpClientTimeout;

    @Value("${httpClient.proxy.type:HTTP}")
    private String httpClientProxyType;

    @Value("${httpClient.proxy.http.host:#{systemProperties['http.proxyHost'] ?: 'localhost'}}")
    private String httpClientProxyHttpHost;

    @Value("${httpClient.proxy.http.port:#{systemProperties['http.proxyPort'] ?: 3128}}")
    private int httpClientProxyHttpPort;

    @Value("${httpClient.proxy.http.username:#{null}}")
    private String httpClientProxyHttpUsername;

    @Value("${httpClient.proxy.http.password:#{null}}")
    private String httpClientProxyHttpPassword;

    @Value("${httpClient.proxy.https.host:#{systemProperties['https.proxyHost'] ?: 'localhost'}}")
    private String httpClientProxyHttpsHost;

    @Value("${httpClient.proxy.https.port:#{systemProperties['https.proxyPort'] ?: 3128}}")
    private int httpClientProxyHttpsPort;

    @Value("${httpClient.proxy.https.username:#{null}}")
    private String httpClientProxyHttpsUsername;

    @Value("${httpClient.proxy.https.password:#{null}}")
    private String httpClientProxyHttpsPassword;

    @Value("#{systemProperties['httpClient.proxy'] == null ? false : true }")
    private boolean isProxyConfigured;

    @Autowired
    private Vertx vertx;

    private HttpClient getHttpClient(String uriScheme, Boolean useSystemProxy) {
        boolean ssl = HTTPS_SCHEME.equalsIgnoreCase(uriScheme);

        final HttpClientOptions options = new HttpClientOptions()
            .setSsl(ssl)
            .setTrustAll(true)
            .setVerifyHost(false)
            .setMaxPoolSize(1)
            .setKeepAlive(false)
            .setTcpKeepAlive(false)
            .setConnectTimeout(httpClientTimeout);

        if ((useSystemProxy != null && useSystemProxy == Boolean.TRUE) || (useSystemProxy == null && this.isProxyConfigured)) {
            ProxyOptions proxyOptions = new ProxyOptions();
            proxyOptions.setType(ProxyType.valueOf(httpClientProxyType));
            if (HTTPS_SCHEME.equals(uriScheme)) {
                proxyOptions.setHost(httpClientProxyHttpsHost);
                proxyOptions.setPort(httpClientProxyHttpsPort);
                proxyOptions.setUsername(httpClientProxyHttpsUsername);
                proxyOptions.setPassword(httpClientProxyHttpsPassword);
            } else {
                proxyOptions.setHost(httpClientProxyHttpHost);
                proxyOptions.setPort(httpClientProxyHttpPort);
                proxyOptions.setUsername(httpClientProxyHttpUsername);
                proxyOptions.setPassword(httpClientProxyHttpPassword);
            }
            options.setProxyOptions(proxyOptions);
        }

        return vertx.createHttpClient(options);
    }

    @Override
    public Buffer request(HttpMethod method, String uri, Map<String, String> headers, String body, Boolean useSystemProxy) {
        if (uri == null || uri.isEmpty()) {
            LOGGER.error("HttpClient configuration is empty");
            return null;
        }

        CompletableFuture<Buffer> future = new VertxCompletableFuture<>(vertx);
        URI requestUri = URI.create(uri);

        final HttpClient httpClient = this.getHttpClient(requestUri.getScheme(), useSystemProxy);

        final int port = requestUri.getPort() != -1 ? requestUri.getPort() : (HTTPS_SCHEME.equals(requestUri.getScheme()) ? 443 : 80);

        HttpClientRequest request = httpClient.request(
            io.vertx.core.http.HttpMethod.valueOf(method.name()),
            port,
            requestUri.getHost(),
            requestUri.getPath(),
            response -> LOGGER.debug("Web response status code : {}", response.statusCode())
        );
        request.setTimeout(httpClientTimeout);

        //headers
        if (headers != null) {
            headers.forEach(request::putHeader);
        }
        if (body != null) {
            if (!request.headers().contains(HttpHeaders.CONTENT_TYPE)) {
                request.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            }
            request.putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(body.getBytes().length));
            request.write(body);
        }
        request.putHeader("X-Gravitee-Request-Id", RandomString.generate());

        request.handler(
            response -> {
                if (response.statusCode() >= 200 && response.statusCode() <= 299) {
                    response.bodyHandler(
                        buffer -> {
                            future.complete(buffer);

                            // Close client
                            httpClient.close();
                        }
                    );
                } else {
                    response.bodyHandler(
                        buffer -> {
                            future.completeExceptionally(
                                new TechnicalManagementException(
                                    " Error on url '" +
                                    uri +
                                    "'. Status code: " +
                                    response.statusCode() +
                                    ". Message: " +
                                    buffer.toString(),
                                    null
                                )
                            );

                            // Close client
                            httpClient.close();
                        }
                    );
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

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new TechnicalManagementException(e.getMessage(), e);
        }
    }
}
