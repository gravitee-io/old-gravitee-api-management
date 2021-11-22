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
package io.gravitee.gateway.services.heartbeat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import io.gravitee.common.service.AbstractService;
import io.gravitee.common.util.Version;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.services.heartbeat.event.InstanceEventPayload;
import io.gravitee.gateway.services.heartbeat.event.Plugin;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HeartbeatService extends AbstractService implements MessageListener<Event>, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatService.class);

    final static String EVENT_LAST_HEARTBEAT_PROPERTY = "last_heartbeat_at";
    final static String EVENT_STARTED_AT_PROPERTY = "started_at";
    final static String EVENT_STOPPED_AT_PROPERTY = "stopped_at";
    final static String EVENT_ID_PROPERTY = "id";
    final static String EVENT_STATE_PROPERTY = "create";

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${services.heartbeat.enabled:true}")
    private boolean enabled;

    @Value("${services.heartbeat.delay:5000}")
    private int delay;

    @Value("${services.heartbeat.unit:MILLISECONDS}")
    private TimeUnit unit;

    @Value("${services.heartbeat.storeSystemProperties:true}")
    private boolean storeSystemProperties;

    @Value("${http.port:8082}")
    private String port;

    @Autowired
    private Node node;

    @Autowired
    private PluginRegistry pluginRegistry;

    @Autowired
    private EventRepository eventRepository;

    private ExecutorService executorService;

    private Event heartbeatEvent;

    @Autowired
    private GatewayConfiguration gatewayConfiguration;

    @Autowired
    private ClusterManager clusterManager;

    @Autowired
    private HazelcastInstance hzInstance;

    // How to avoid duplicate
    private ITopic<Event> topic;

    // FIXME: use String only with Hazelcast 3.12.x, will be UUID from 4.1.x
    private String subscriptionId;

    @Override
    public void afterPropertiesSet() throws Exception {
        topic = hzInstance.getTopic("heartbeats");
        subscriptionId = topic.addMessageListener(this);
    }

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();
            LOGGER.info("Start gateway heartbeat");

            heartbeatEvent = prepareEvent();
            topic.publish(heartbeatEvent);

            // Remove the state to not include it in the underlying repository as it's just used for internal
            // purpose
            heartbeatEvent.getProperties().remove(EVENT_STATE_PROPERTY);

            executorService = Executors.newSingleThreadScheduledExecutor(
                    r -> new Thread(r, "gio-heartbeat"));

            HeartbeatThread monitorThread = new HeartbeatThread(topic, heartbeatEvent);

            LOGGER.info("Monitoring scheduled with fixed delay {} {} ", delay, unit.name());

            ((ScheduledExecutorService) executorService).scheduleWithFixedDelay(
                    monitorThread, 0, delay, unit);

            LOGGER.info("Start gateway heartbeat : DONE");
        }
    }

    @Override
    public void onMessage(Message<Event> message) {
        // Writing event to the repository is the responsibility of the master node
        if (clusterManager.isMasterNode()) {
            Event event = message.getMessageObject();
            try {
                String state = event.getProperties().get(EVENT_STATE_PROPERTY);
                if (state != null) {
                    eventRepository.create(event);
                } else {
                    eventRepository.update(event);
                }
            } catch (Exception ex) {
                LOGGER.error("An error occurs while pushing heartbeat event id[{}] type[{}]", event.getId(), event.getType(), ex);
                // Push back the event into the topic in case of error
                topic.publish(event);
            }
        }
    }

    @Override
    public Object preStop() throws Exception {
        if (enabled) {
            heartbeatEvent.setType(EventType.GATEWAY_STOPPED);
            heartbeatEvent.getProperties().put(EVENT_STOPPED_AT_PROPERTY, Long.toString(new Date().getTime()));
            LOGGER.debug("Pre-stopping Heartbeat Service");
            LOGGER.debug("Sending a {} event", heartbeatEvent.getType());

            topic.publish(heartbeatEvent);

            topic.removeMessageListener(subscriptionId);
        }
        return this;
    }

    @Override
    protected void doStop() throws Exception {
        if (enabled) {
            if (! executorService.isShutdown()) {
                LOGGER.info("Stop gateway monitor");
                executorService.shutdownNow();
            } else {
                LOGGER.info("Gateway monitor already shut-downed");
            }

            super.doStop();
            LOGGER.info("Stop gateway monitor : DONE");
        }
    }

    private Event prepareEvent() {
        Event event = new Event();
        event.setId(UUID.toString(UUID.random()));
        event.setType(EventType.GATEWAY_STARTED);
        event.setCreatedAt(new Date());
        event.setEnvironmentId("DEFAULT");
        event.setUpdatedAt(event.getCreatedAt());
        final Map<String, String> properties = new HashMap<>();
        properties.put(EVENT_STATE_PROPERTY, "create");
        properties.put(EVENT_ID_PROPERTY, node.id());

        final String now = Long.toString(event.getCreatedAt().getTime());
        properties.put(EVENT_STARTED_AT_PROPERTY, now);
        properties.put(EVENT_LAST_HEARTBEAT_PROPERTY, now);
        event.setProperties(properties);

        InstanceEventPayload instance = createInstanceInfo();

        try {
            String payload = objectMapper.writeValueAsString(instance);
            event.setPayload(payload);
        } catch (JsonProcessingException jsex) {
            LOGGER.error("An error occurs while transforming instance information into JSON", jsex);
        }
        return event;
    }

    private InstanceEventPayload createInstanceInfo() {
        InstanceEventPayload instanceInfo = new InstanceEventPayload();

        instanceInfo.setId(node.id());
        instanceInfo.setVersion(Version.RUNTIME_VERSION.toString());

        Optional<List<String>> shardingTags = gatewayConfiguration.shardingTags();
        instanceInfo.setTags(shardingTags.orElse(null));

        instanceInfo.setPlugins(plugins());
        instanceInfo.setSystemProperties(getSystemProperties());
        instanceInfo.setPort(port);

        Optional<String> tenant = gatewayConfiguration.tenant();
        instanceInfo.setTenant(tenant.orElse(null));

        try {
            instanceInfo.setHostname(InetAddress.getLocalHost().getHostName());
            instanceInfo.setIp(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException uhe) {
            LOGGER.warn("Could not get hostname / IP", uhe);
        }

        return instanceInfo;
    }

    private Set<Plugin> plugins() {
        return pluginRegistry.plugins().stream().map(regPlugin -> {
            Plugin plugin = new Plugin();
            plugin.setId(regPlugin.id());
            plugin.setName(regPlugin.manifest().name());
            plugin.setDescription(regPlugin.manifest().description());
            plugin.setVersion(regPlugin.manifest().version());
            plugin.setType(regPlugin.type().toLowerCase());
            plugin.setPlugin(regPlugin.clazz());
            return plugin;
        }).collect(Collectors.toSet());
    }

    @Override
    protected String name() {
        return "Gateway Heartbeat";
    }

    private Map getSystemProperties() {
        if (storeSystemProperties) {
            return System.getProperties()
                    .entrySet()
                    .stream()
                    .filter(entry -> !entry.getKey().toString().toUpperCase().startsWith("GRAVITEE"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        return Collections.emptyMap();
    }
}
