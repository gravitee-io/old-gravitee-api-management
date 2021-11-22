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
package io.gravitee.gateway.standalone.vertx.ws;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.ws.WebSocket;
import io.gravitee.gateway.standalone.vertx.VertxHttpServerRequest;
import io.vertx.core.http.HttpServerRequest;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxWebSocketServerRequest extends VertxHttpServerRequest {

    private VertxWebSocket vertxWebSocket;

    VertxWebSocketServerRequest(HttpServerRequest httpServerRequest, IdGenerator idGenerator) {
        super(httpServerRequest, idGenerator);

        this.vertxWebSocket = new VertxWebSocket(httpServerRequest);
    }

    @Override
    public Request bodyHandler(io.gravitee.gateway.api.handler.Handler<io.gravitee.gateway.api.buffer.Buffer> bodyHandler) {
        return this;
    }

    @Override
    public Request endHandler(io.gravitee.gateway.api.handler.Handler<Void> endHandler) {
        return this;
    }

    @Override
    public WebSocket websocket() {
        return vertxWebSocket;
    }

    @Override
    public boolean isWebSocket() {
        return true;
    }

    @Override
    public Response create() {
        return new VertxWebSocketServerResponse(this);
    }
}
