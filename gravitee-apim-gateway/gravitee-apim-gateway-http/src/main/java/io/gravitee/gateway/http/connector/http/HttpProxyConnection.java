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
package io.gravitee.gateway.http.connector.http;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http2.HttpFrame;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.gateway.core.proxy.EmptyProxyResponse;
import io.gravitee.gateway.http.connector.AbstractHttpProxyConnection;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpProxyConnection<T extends HttpProxyResponse> extends AbstractHttpProxyConnection {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private static final Set<CharSequence> HOP_HEADERS;

    static {
        Set<CharSequence> hopHeaders = new HashSet<>();

        // Hop-by-hop headers
        hopHeaders.add(HttpHeaderNames.CONNECTION);
        hopHeaders.add(HttpHeaderNames.KEEP_ALIVE);
        hopHeaders.add(HttpHeaderNames.PROXY_AUTHORIZATION);
        hopHeaders.add(HttpHeaderNames.PROXY_AUTHENTICATE);
        hopHeaders.add(HttpHeaderNames.PROXY_CONNECTION);
        hopHeaders.add(HttpHeaderNames.TE);
        hopHeaders.add(HttpHeaderNames.TRAILER);
        hopHeaders.add(HttpHeaderNames.UPGRADE);

        HOP_HEADERS = Collections.unmodifiableSet(hopHeaders);
    }

    private HttpClientRequest httpClientRequest;
    private final ProxyRequest proxyRequest;
    private T proxyResponse;
    private Handler<Throwable> timeoutHandler;
    private boolean canceled = false;
    private boolean transmitted = false;
    private boolean headersWritten = false;
    private boolean content = false;

    public HttpProxyConnection(HttpEndpoint endpoint, ProxyRequest proxyRequest) {
        super(endpoint);
        this.proxyRequest = proxyRequest;
    }

    @Override
    public ProxyConnection connect(HttpClient httpClient, int port, String host, String uri, Handler<Void> tracker) {
        // Remove HOP-by-HOP headers
        for (CharSequence header : HOP_HEADERS) {
            proxyRequest.headers().remove(header.toString());
        }

        httpClientRequest = prepareUpstreamRequest(httpClient, port, host, uri);

        cancelHandler(tracker);

        httpClientRequest.handler(
            event -> {
                // Prepare upstream response
                handleUpstreamResponse(event, tracker);

                // And send it to the client
                sendToClient(proxyResponse);
            }
        );

        httpClientRequest.connectionHandler(
            connection -> {
                connection.exceptionHandler(
                    ex -> {
                        // I don't want to fill my logs with error
                    }
                );
            }
        );

        httpClientRequest.exceptionHandler(
            event -> {
                if (!isCanceled() && !isTransmitted()) {
                    proxyRequest.metrics().setMessage(event.getMessage());

                    if (
                        this.timeoutHandler() != null &&
                        (
                            event instanceof ConnectException ||
                            event instanceof TimeoutException ||
                            event instanceof NoRouteToHostException ||
                            event instanceof UnknownHostException
                        )
                    ) {
                        handleConnectTimeout(event);
                    } else {
                        ProxyResponse clientResponse = new EmptyProxyResponse(
                            ((event instanceof ConnectTimeoutException) || (event instanceof TimeoutException))
                                ? HttpStatusCode.GATEWAY_TIMEOUT_504
                                : HttpStatusCode.BAD_GATEWAY_502
                        );

                        clientResponse.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
                        sendToClient(clientResponse);
                        tracker.handle(null);
                    }
                }
            }
        );

        return this;
    }

    protected HttpClientRequest prepareUpstreamRequest(HttpClient httpClient, int port, String host, String uri) {
        // Prepare HTTP request
        httpClientRequest = httpClient.request(HttpMethod.valueOf(proxyRequest.method().name()), port, host, uri);

        httpClientRequest.setTimeout(endpoint.getHttpClientOptions().getReadTimeout());
        httpClientRequest.setFollowRedirects(endpoint.getHttpClientOptions().isFollowRedirects());

        if (proxyRequest.method() == io.gravitee.common.http.HttpMethod.OTHER) {
            httpClientRequest.setRawMethod(proxyRequest.rawMethod());
        }

        return httpClientRequest;
    }

    protected T createProxyResponse(HttpClientResponse clientResponse) {
        return (T) new HttpProxyResponse(clientResponse);
    }

    protected T handleUpstreamResponse(final HttpClientResponse clientResponse, Handler<Void> tracker) {
        this.proxyResponse = createProxyResponse(clientResponse);

        // Copy HTTP headers
        clientResponse
            .headers()
            .names()
            .forEach(headerName -> proxyResponse.headers().put(headerName, clientResponse.headers().getAll(headerName)));

        proxyResponse.pause();

        proxyResponse.cancelHandler(tracker);

        // Copy body content
        clientResponse.handler(event -> proxyResponse.bodyHandler().handle(Buffer.buffer(event.getBytes())));

        // Signal end of the response
        clientResponse.endHandler(
            event -> {
                // Write trailing headers to client response
                if (!clientResponse.trailers().isEmpty()) {
                    clientResponse.trailers().forEach(header -> proxyResponse.trailers().set(header.getKey(), header.getValue()));
                }

                proxyResponse.endHandler().handle(null);
                tracker.handle(null);
            }
        );

        clientResponse.exceptionHandler(
            throwable -> {
                LOGGER.error(
                    "Unexpected error while handling backend response for request {} {} - {}",
                    httpClientRequest.method(),
                    httpClientRequest.absoluteURI(),
                    throwable.getMessage()
                );

                proxyResponse.endHandler().handle(null);
                tracker.handle(null);
            }
        );

        clientResponse.customFrameHandler(
            frame ->
                proxyResponse.writeCustomFrame(HttpFrame.create(frame.type(), frame.flags(), Buffer.buffer(frame.payload().getBytes())))
        );

        return proxyResponse;
    }

    @Override
    public ProxyConnection cancel() {
        this.canceled = true;
        this.httpClientRequest.reset();
        cancelHandler.handle(null);
        if (proxyResponse != null) {
            proxyResponse.bodyHandler(null);
        }
        return this;
    }

    private boolean isCanceled() {
        return this.canceled;
    }

    private boolean isTransmitted() {
        return transmitted;
    }

    @Override
    public ProxyConnection exceptionHandler(Handler<Throwable> timeoutHandler) {
        this.timeoutHandler = timeoutHandler;
        return this;
    }

    @Override
    public ProxyConnection responseHandler(Handler<ProxyResponse> responseHandler) {
        this.responseHandler = responseHandler;
        return this;
    }

    @Override
    protected void sendToClient(ProxyResponse proxyResponse) {
        transmitted = true;
        super.sendToClient(proxyResponse);
    }

    private void handleConnectTimeout(Throwable throwable) {
        this.timeoutHandler.handle(throwable);
    }

    private Handler<Throwable> timeoutHandler() {
        return this.timeoutHandler;
    }

    @Override
    public HttpProxyConnection<T> write(Buffer chunk) {
        // There is some request content, set the flag to true
        content = true;

        if (!headersWritten) {
            this.writeHeaders();
        }

        httpClientRequest.write(io.vertx.core.buffer.Buffer.buffer(chunk.getBytes()));

        return this;
    }

    @Override
    public WriteStream<Buffer> drainHandler(Handler<Void> drainHandler) {
        httpClientRequest.drainHandler(aVoid -> drainHandler.handle(null));
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return httpClientRequest.writeQueueFull();
    }

    private void writeHeaders() {
        writeUpstreamHeaders();

        headersWritten = true;
    }

    protected void writeUpstreamHeaders() {
        HttpHeaders headers = proxyRequest.headers();

        // Check chunk flag on the request if there are some content to push and if transfer_encoding is set
        // with chunk value
        if (content) {
            String encoding = headers.getFirst(HttpHeaders.TRANSFER_ENCODING);
            if (encoding != null && encoding.contains(HttpHeadersValues.TRANSFER_ENCODING_CHUNKED)) {
                httpClientRequest.setChunked(true);
            }
        } else {
            proxyRequest.headers().remove(HttpHeaders.TRANSFER_ENCODING);
        }

        // Copy headers to upstream
        proxyRequest.headers().forEach(httpClientRequest::putHeader);
    }

    @Override
    public void end() {
        if (!headersWritten) {
            this.writeHeaders();
        }

        if (!canceled) {
            httpClientRequest.end();
        }
    }

    @Override
    public ProxyConnection writeCustomFrame(HttpFrame frame) {
        httpClientRequest.writeCustomFrame(frame.type(), frame.flags(), io.vertx.core.buffer.Buffer.buffer(frame.payload().getBytes()));

        return this;
    }
}
