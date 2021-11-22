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
package io.gravitee.gateway.reactor.impl;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.handler.EntrypointResolver;
import io.gravitee.gateway.reactor.handler.HandlerEntrypoint;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultReactor extends AbstractService implements
        Reactor, EventListener<ReactorEvent, Reactable> {

    private final Logger LOGGER = LoggerFactory.getLogger(DefaultReactor.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @Autowired
    private EntrypointResolver entrypointResolver;

    @Autowired
    private GatewayConfiguration gatewayConfiguration;

    @Autowired
    private RequestProcessorChainFactory requestProcessorChainFactory;

    @Autowired
    private ResponseProcessorChainFactory responseProcessorChainFactory;

    @Autowired
    private NotFoundProcessorChainFactory notFoundProcessorChainFactory;

    @Override
    public void route(Request serverRequest, Response serverResponse, Handler<ExecutionContext> handler) {
        LOGGER.debug("Receiving a request {} for path {}", serverRequest.id(), serverRequest.path());

        // Prepare invocation execution context
        ExecutionContext context = new SimpleExecutionContext(serverRequest, serverResponse);

        // Set gateway tenant
        gatewayConfiguration.tenant().ifPresent(tenant -> serverRequest.metrics().setTenant(tenant));

        // Set gateway zone
        gatewayConfiguration.zone().ifPresent(zone -> serverRequest.metrics().setZone(zone));

        // Prepare handler chain
        requestProcessorChainFactory
                .create()
                .handler(ctx -> {
                    HandlerEntrypoint entrypoint = entrypointResolver.resolve(ctx);

                    if (entrypoint != null) {
                        entrypoint
                                .target()
                                .handler(context1 -> {
                                    // Ensure that response has been ended before going further
                                    context1.response().end();

                                    processResponse(context1, handler);
                                })
                                .handle(ctx);
                    } else {
                        processNotFound(ctx, handler);
                    }
                })
                .errorHandler(__ -> processResponse(context, handler))
                .exitHandler(__ -> processResponse(context, handler))
                .handle(context);
    }

    private void processNotFound(ExecutionContext context, Handler<ExecutionContext> handler) {
        notFoundProcessorChainFactory
                .create()
                .handler(handler)
                .handle(context);
    }

    private void processResponse(ExecutionContext context, Handler<ExecutionContext> handler) {
        responseProcessorChainFactory
                .create()
                .handler(handler)
                .handle(context);
    }

    @Override
    public void onEvent(Event<ReactorEvent, Reactable> event) {
        switch (event.type()) {
            case DEPLOY:
                reactorHandlerRegistry.create(event.content());
                break;
            case UPDATE:
                reactorHandlerRegistry.update(event.content());
                break;
            case UNDEPLOY:
                reactorHandlerRegistry.remove(event.content());
                break;
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        eventManager.subscribeForEvents(this, ReactorEvent.class);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        reactorHandlerRegistry.clear();
    }
}
