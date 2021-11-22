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
package io.gravitee.gateway.standalone.vertx;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.standalone.vertx.grpc.VertxGrpcServerRequest;
import io.gravitee.gateway.standalone.vertx.http2.VertxHttp2ServerRequest;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxReactorHandler implements Handler<HttpServerRequest> {

    private final Reactor reactor;
    private IdGenerator idGenerator;

    public VertxReactorHandler(final Reactor reactor, IdGenerator idGenerator) {
        this.reactor = reactor;
        this.idGenerator = idGenerator;
    }

    @Override
    public void handle(HttpServerRequest httpServerRequest) {
        VertxHttpServerRequest request;

        if (httpServerRequest.version() == HttpVersion.HTTP_2) {
            if (MediaType.APPLICATION_GRPC.equals(httpServerRequest.getHeader(HttpHeaders.CONTENT_TYPE))) {
                request = new VertxGrpcServerRequest(httpServerRequest, idGenerator);
            } else {
                request = new VertxHttp2ServerRequest(httpServerRequest, idGenerator);
            }
        } else {
            request = new VertxHttpServerRequest(httpServerRequest, idGenerator);
        }

        route(request, request.create());
    }

    protected void route(final Request request, final Response response) {
        reactor.route(request, response, __ -> {});
    }
}
