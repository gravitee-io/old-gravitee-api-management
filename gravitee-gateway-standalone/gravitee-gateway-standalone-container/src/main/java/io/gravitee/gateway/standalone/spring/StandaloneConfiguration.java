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
package io.gravitee.gateway.standalone.spring;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.el.ExpressionLanguageInitializer;
import io.gravitee.gateway.dictionary.spring.DictionaryConfiguration;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.spring.ApiHandlerConfiguration;
import io.gravitee.gateway.reactor.spring.ReactorConfiguration;
import io.gravitee.gateway.report.spring.ReporterConfiguration;
import io.gravitee.gateway.standalone.node.GatewayNode;
import io.gravitee.gateway.standalone.vertx.VertxReactorConfiguration;
import io.gravitee.node.cluster.spring.ClusterConfiguration;
import io.gravitee.node.container.NodeFactory;
import io.gravitee.node.vertx.spring.VertxConfiguration;
import io.gravitee.plugin.alert.spring.AlertPluginConfiguration;
import io.gravitee.plugin.core.spring.PluginConfiguration;
import io.gravitee.plugin.discovery.spring.ServiceDiscoveryPluginConfiguration;
import io.gravitee.plugin.policy.spring.PolicyPluginConfiguration;
import io.gravitee.plugin.resource.spring.ResourcePluginConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Import({
        ClusterConfiguration.class,
        VertxConfiguration.class,
        ReactorConfiguration.class,
        VertxReactorConfiguration.class,
        PluginConfiguration.class,
        PolicyPluginConfiguration.class,
        ResourcePluginConfiguration.class,
        ReporterConfiguration.class,
        ApiHandlerConfiguration.class,
        DictionaryConfiguration.class,
        AlertPluginConfiguration.class,
        ServiceDiscoveryPluginConfiguration.class
})
public class StandaloneConfiguration {

    @Bean
    public EventManager eventManager() {
        return new EventManagerImpl();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new GraviteeMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<Enum> modifyEnumDeserializer(DeserializationConfig config,
                                                                 final JavaType type,
                                                                 BeanDescription beanDesc,
                                                                 final JsonDeserializer<?> deserializer) {
                return new JsonDeserializer<Enum>() {
                    @Override
                    public Enum deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
                        Class<? extends Enum> rawClass = (Class<Enum<?>>) type.getRawClass();
                        return Enum.valueOf(rawClass, jp.getValueAsString().toUpperCase());
                    }
                };
            }
        });

        mapper.registerModule(module);

        return mapper;
    }

    @Bean
    public ExpressionLanguageInitializer expressionLanguageInitializer() {

        return new ExpressionLanguageInitializer();
    }

    @Bean
    public NodeFactory node() {
        return new NodeFactory(GatewayNode.class);
    }

    @Bean
    public static GatewayConfiguration gatewayConfiguration() {
        return new GatewayConfiguration();
    }
}