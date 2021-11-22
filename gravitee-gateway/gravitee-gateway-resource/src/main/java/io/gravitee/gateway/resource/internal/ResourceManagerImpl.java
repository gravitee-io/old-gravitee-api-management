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
package io.gravitee.gateway.resource.internal;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.resource.ResourceConfigurationFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginClassLoader;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.resource.api.ResourceConfiguration;
import io.gravitee.resource.api.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResourceManagerImpl extends AbstractLifecycleComponent<ResourceManager> implements ResourceLifecycleManager {

    private final Logger logger = LoggerFactory.getLogger(ResourceManagerImpl.class);

    @Autowired
    private ApplicationContext applicationContext;

    private final Map<String, io.gravitee.resource.api.Resource> resources = new HashMap<>();
    private final Map<String, PluginClassLoader> classloaders = new HashMap<>();

    @Override
    protected void doStart() throws Exception {
        // Initialize required resources
        initialize();

        // Start resources
        resources.entrySet()
                .stream()
                .forEach(resource -> {
                    try {
                        logger.info("Start resource {} [{}]", resource.getKey(), resource.getValue().getClass());
                        resource.getValue().start();
                    } catch (Exception ex) {
                        logger.error("Unable to start resource", ex);
                    }
                });
    }

    @Override
    protected void doStop() throws Exception {
        // Stop resources
        resources.entrySet()
                .stream()
                .forEach(resource -> {
                    try {
                        logger.info("Stop resource {} [{}]", resource.getKey(), resource.getValue().getClass());
                        resource.getValue().stop();
                    } catch (Exception ex) {
                        logger.error("Unable to stop resource", ex);
                    }
                });

        // Close resource classLoaders
        resources.values().forEach(resource -> {
            ClassLoader resourceClassLoader = resource.getClass().getClassLoader();
            if (resourceClassLoader instanceof PluginClassLoader) {
                try {
                    ((PluginClassLoader)resourceClassLoader).close();
                } catch (IOException ioe) {
                    logger.error("Unable to close classloader for resource {}", resource.getClass(), ioe);
                }
            }
        });

        // Be sure to remove all references to resources
        resources.clear();
    }

    private void initialize() {
        String[] beanNamesForType = applicationContext.getParent().getBeanNamesForType(
                ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, ResourcePlugin.class));

        ConfigurablePluginManager<ResourcePlugin> rpm = (ConfigurablePluginManager<ResourcePlugin>) applicationContext.getBean(beanNamesForType[0]);

        ResourceClassLoaderFactory rclf = applicationContext.getBean(ResourceClassLoaderFactory.class);
        ResourceConfigurationFactory rcf = applicationContext.getBean(ResourceConfigurationFactory.class);
        ReactorHandler rh = applicationContext.getBean(ReactorHandler.class);
        Reactable reactable = applicationContext.getBean(Reactable.class);

        Set<Resource> resourceDeps = reactable.dependencies(Resource.class);

        resourceDeps.forEach(resource -> {
            final ResourcePlugin resourcePlugin = rpm.get(resource.getType());
            if (resourcePlugin == null) {
                logger.error("Resource [{}] can not be found in plugin registry", resource.getType());
                throw new IllegalStateException("Resource ["+resource.getType()+"] can not be found in plugin registry");
            }

            PluginClassLoader resourceClassLoader = classloaders.computeIfAbsent(
                    resourcePlugin.id(), s -> rclf.getOrCreateClassLoader(resourcePlugin, rh.getClass().getClassLoader()));
            
            logger.debug("Loading resource {} for {}", resource.getName(), rh);

            try {
                Class<? extends io.gravitee.resource.api.Resource> resourceClass = (Class<? extends io.gravitee.resource.api.Resource>) ClassUtils.forName(resourcePlugin.resource().getName(), resourceClassLoader);
                Map<Class<?>, Object> injectables = new HashMap<>();

                if (resourcePlugin.configuration() != null) {
                    Class<? extends ResourceConfiguration> resourceConfigurationClass = (Class<? extends ResourceConfiguration>) ClassUtils.forName(resourcePlugin.configuration().getName(), resourceClassLoader);
                    injectables.put(resourceConfigurationClass, rcf.create(resourceConfigurationClass, resource.getConfiguration()));
                }

                io.gravitee.resource.api.Resource resourceInstance = new ResourceFactory().create(resourceClass, injectables);

                if (resourceInstance instanceof ApplicationContextAware) {
                    ((ApplicationContextAware) resourceInstance).setApplicationContext(applicationContext);
                }

                resources.put(resource.getName(), resourceInstance);
            } catch (Exception ex) {
                logger.error("Unable to create resource", ex);
                if (resourceClassLoader != null) {
                    try {
                        resourceClassLoader.close();
                    } catch (IOException ioe) {
                        logger.error("Unable to close classloader for resource", ioe);
                    }
                }
            }
        });
    }

    @Override
    public Object getResource(String name) {
        return resources.get(name);
    }

    @Override
    public <T> T getResource(Class<T> requiredType) {
            Optional<T> resource = (Optional<T>) resources.values().stream()
                    .filter(resourceEntry -> resourceEntry.getClass().isAssignableFrom(requiredType))
                    .findFirst();

            return resource.orElse(null);
    }

    @Override
    public <T> T getResource(String name, Class<T> requiredType) {
        Object resource = getResource(name);
        if (resource == null) {
            return null;
        }

        if (requiredType.isAssignableFrom(resource.getClass())) {
            return (T) resource;
        } else {
            throw new IllegalArgumentException("Required type parameter does not match the resource type");
        }
    }

    @Override
    public Class<?> getType(String name) {
        io.gravitee.resource.api.Resource resource = (io.gravitee.resource.api.Resource) getResource(name);

        if (resource != null) {
            return resource.getClass();
        }

        return null;
    }

    @Override
    public boolean containsResource(String name) {
        return resources.containsKey(name);
    }

    @Override
    public Collection<? extends io.gravitee.resource.api.Resource> getResources() {
        return new ArrayList<>(resources.values());
    }
}
