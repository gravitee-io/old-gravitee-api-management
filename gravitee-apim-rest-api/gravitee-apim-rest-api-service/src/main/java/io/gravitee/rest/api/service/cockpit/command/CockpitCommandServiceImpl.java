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
package io.gravitee.rest.api.service.cockpit.command;

import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.Payload;
import io.gravitee.cockpit.api.command.Reply;
import io.gravitee.cockpit.api.command.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.bridge.BridgePayload;
import io.gravitee.cockpit.api.command.bridge.BridgeReply;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class CockpitCommandServiceImpl implements CockpitCommandService {

    private final CockpitConnector cockpitConnector;

    public CockpitCommandServiceImpl(
        // Need to make this injection lazy to be sure cockpit-connector plugin has been loaded before looking for this bean
        @Lazy CockpitConnector cockpitConnector
    ) {
        this.cockpitConnector = cockpitConnector;
    }

    @Override
    public BridgeReply send(BridgeCommand command) {
        return (BridgeReply) send((Command<BridgePayload>) command);
    }

    @Override
    public <T extends Payload> Reply send(Command<T> command) {
        return cockpitConnector.sendCommand(command).blockingGet();
    }
}
