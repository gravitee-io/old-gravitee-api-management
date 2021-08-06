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
package io.gravitee.rest.api.standalone;

import io.gravitee.node.container.spring.SpringBasedContainer;
import io.gravitee.rest.api.standalone.spring.StandaloneConfiguration;
import java.util.List;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeApisContainer extends SpringBasedContainer {

    @Override
    protected List<Class<?>> annotatedClasses() {
        List<Class<?>> classes = super.annotatedClasses();
        classes.add(StandaloneConfiguration.class);
        return classes;
    }

    @Override
    protected String name() {
        return "Gravitee.io - Rest APIs";
    }

    @Override
    protected void initializeLogging() {
        super.initializeLogging();

        // Move all java util logging logs to SLF4j
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    public static void main(String[] args) throws Exception {
        // If you want to run Gravitee standalone from your IDE, please do not forget
        // to specify -Dgravitee.home=/path/to/gravitee/home in order to make it works.
        GraviteeApisContainer container = new GraviteeApisContainer();
        container.start();
    }
}
