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

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.alert.api.condition.Filter;
import io.gravitee.alert.api.condition.StringCondition;
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.alert.api.trigger.TriggerProvider;
import io.gravitee.alert.api.trigger.command.AlertNotificationCommand;
import io.gravitee.alert.api.trigger.command.Command;
import io.gravitee.alert.api.trigger.command.Handler;
import io.gravitee.alert.api.trigger.command.ResolvePropertyCommand;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.notifier.api.Notification;
import io.gravitee.plugin.alert.AlertTriggerProviderManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertEventRepository;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.AlertEventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.alert.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.impl.alert.EmailNotifierConfiguration;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AlertServiceImpl extends TransactionalService implements AlertService, InitializingBean {

    private final Logger LOGGER = LoggerFactory.getLogger(AlertServiceImpl.class);

    private static final String UNKNOWN_SERVICE = "1";
    private static final String FIELD_API = "api";
    private static final String FIELD_APPLICATION = "application";
    private static final String FIELD_TENANT = "tenant";
    private static final String FIELD_PLAN = "plan";

    private static final String METADATA_NAME = "name";
    private static final String METADATA_DELETED = "deleted";
    private static final String METADATA_UNKNOWN = "unknown";
    private static final String METADATA_VERSION = "version";
    private static final String METADATA_UNKNOWN_API_NAME = "Unknown API (not found)";
    private static final String METADATA_UNKNOWN_APPLICATION_NAME = "Unknown application (keyless)";
    private static final String METADATA_DELETED_API_NAME = "Deleted API";
    private static final String METADATA_DELETED_APPLICATION_NAME = "Deleted application";
    private static final String METADATA_DELETED_TENANT_NAME = "Deleted tenant";
    private static final String METADATA_DELETED_PLAN_NAME = "Deleted plan";

    @Value("${notifiers.email.subject:[Gravitee.io] %s}")
    private String subject;

    @Value("${notifiers.email.host:#{null}}")
    private String host;

    @Value("${notifiers.email.port}")
    private String port;

    @Value("${notifiers.email.username:#{null}}")
    private String username;

    @Value("${notifiers.email.password:#{null}}")
    private String password;

    @Value("${notifiers.email.starttls.enabled:false}")
    private boolean startTLSEnabled;

    @Value("${notifiers.email.ssl.trustAll:false}")
    private boolean sslTrustAll;

    @Value("${notifiers.email.ssl.keyStore:#{null}}")
    private String sslKeyStore;

    @Value("${notifiers.email.ssl.keyStorePassword:#{null}}")
    private String sslKeyStorePassword;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private AlertTriggerRepository alertTriggerRepository;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private PlanService planService;

    @Autowired
    private AlertEventRepository alertEventRepository;

    @Autowired
    private TriggerProvider triggerProvider;

    @Autowired
    private AlertTriggerProviderManager triggerProviderManager;

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private ApiMetadataService apiMetadataService;

    @Override
    public AlertStatusEntity getStatus() {
        AlertStatusEntity status = new AlertStatusEntity();

        status.setEnabled(parameterService.findAsBoolean(Key.ALERT_ENABLED));
        status.setPlugins(triggerProviderManager.findAll().size());

        return status;
    }

    @Override
    public AlertTriggerEntity create(final NewAlertTriggerEntity newAlertTrigger) {
        checkAlert();

        try {
            // Get trigger
            AlertTrigger alertTrigger = convert(newAlertTrigger);

            alertTrigger.setCreatedAt(new Date());
            alertTrigger.setUpdatedAt(alertTrigger.getCreatedAt());

            return create0(alertTrigger);
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to create an alert " + newAlertTrigger;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    private AlertTriggerEntity create0(AlertTrigger alertTrigger) throws TechnicalException {
        final AlertTrigger createdAlert = alertTriggerRepository.create(alertTrigger);

        final AlertTriggerEntity alertTriggerEntity = convert(createdAlert);

        enhance(alertTriggerEntity, alertTriggerEntity.getReferenceType(), alertTriggerEntity.getReferenceId());

        // Obviously, we are not deploying rule templates :)
        if (!alertTriggerEntity.isTemplate()) {
            triggerOrCancelAlert(alertTriggerEntity);
        }

        return alertTriggerEntity;
    }

    @Override
    public AlertTriggerEntity update(final UpdateAlertTriggerEntity updateAlertTrigger) {
        checkAlert();

        try {
            final Optional<AlertTrigger> alertOptional = alertTriggerRepository.findById(updateAlertTrigger.getId());
            if (alertOptional.isPresent()) {
                final AlertTrigger alertToUpdate = alertOptional.get();
                if (!alertToUpdate.getReferenceId().equals(updateAlertTrigger.getReferenceId())) {
                    throw new AlertNotFoundException(updateAlertTrigger.getId());
                }

                AlertTrigger trigger = convert(updateAlertTrigger);
                trigger.setId(updateAlertTrigger.getId());
                trigger.setReferenceId(alertOptional.get().getReferenceId());
                trigger.setReferenceType(alertOptional.get().getReferenceType());
                trigger.setCreatedAt(alertOptional.get().getCreatedAt());
                trigger.setType(alertOptional.get().getType());
                trigger.setUpdatedAt(new Date());

                final AlertTriggerEntity alertTriggerEntity = convert(alertTriggerRepository.update(trigger));

                enhance(alertTriggerEntity, updateAlertTrigger.getReferenceType(), updateAlertTrigger.getReferenceId());

                // Obviously, we are not deploying rule templates :)
                if (!alertTriggerEntity.isTemplate()) {
                    triggerOrCancelAlert(alertTriggerEntity);
                }

                return alertTriggerEntity;
            } else {
                throw new AlertNotFoundException(updateAlertTrigger.getId());
            }
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to update an alert " + updateAlertTrigger;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    private List<AlertTriggerEntity> findAll() {
        try {
            final Set<AlertTrigger> triggers = alertTriggerRepository.findAll();
            return triggers.stream().map(this::convert).collect(toList());
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to list all alerts";
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public List<AlertTriggerEntity> findByReference(final AlertReferenceType referenceType, final String referenceId) {
        try {
            final List<AlertTrigger> triggers = alertTriggerRepository.findByReference(referenceType.name(), referenceId);
            return triggers
                .stream()
                .map(
                    new Function<AlertTrigger, AlertTriggerEntity>() {
                        @Override
                        public AlertTriggerEntity apply(AlertTrigger alertTrigger) {
                            AlertTriggerEntity entity = convert(alertTrigger);

                            getLastEvent(entity.getId())
                                .ifPresent(
                                    alertEvent -> {
                                        entity.setLastAlertAt(alertEvent.getCreatedAt());
                                        entity.setLastAlertMessage(alertEvent.getMessage());
                                    }
                                );

                            final Date from = new Date(System.currentTimeMillis());

                            Map<String, Integer> counters = new HashMap<>();
                            counters.put(
                                "5m",
                                countEvents(entity.getId(), from.toInstant().minus(Duration.ofMinutes(5)).toEpochMilli(), from.getTime())
                            );
                            counters.put(
                                "1h",
                                countEvents(entity.getId(), from.toInstant().minus(Duration.ofHours(1)).toEpochMilli(), from.getTime())
                            );
                            counters.put(
                                "1d",
                                countEvents(entity.getId(), from.toInstant().minus(Duration.ofDays(1)).toEpochMilli(), from.getTime())
                            );
                            counters.put(
                                "1M",
                                countEvents(entity.getId(), from.toInstant().minus(Duration.ofDays(30)).toEpochMilli(), from.getTime())
                            );

                            entity.setCounters(counters);
                            return entity;
                        }
                    }
                )
                .sorted(comparing(AlertTriggerEntity::getName))
                .collect(toList());
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to list alerts by reference " + referenceType + '/' + referenceId;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public void delete(final String alertId, final String referenceId) {
        try {
            final Optional<AlertTrigger> optionalAlert = alertTriggerRepository.findById(alertId);
            if (!optionalAlert.isPresent() || !optionalAlert.get().getReferenceId().equals(referenceId)) {
                throw new AlertNotFoundException(alertId);
            }
            final AlertTriggerEntity alert = convert(optionalAlert.get());

            // Remove from repository
            alertTriggerRepository.delete(alertId);
            alertEventRepository.deleteAll(alertId);

            // Notify alert plugins
            disableTrigger(alert);
        } catch (TechnicalException te) {
            final String msg = "An error occurs while trying to delete the alert " + alertId;
            LOGGER.error(msg, te);
            throw new TechnicalManagementException(msg, te);
        }
    }

    @Override
    public Page<AlertEventEntity> findEvents(final String alertId, final AlertEventQuery eventQuery) {
        Page<AlertEvent> alertEventsRepo = alertEventRepository.search(
            new AlertEventCriteria.Builder().alert(alertId).from(eventQuery.getFrom()).to(eventQuery.getTo()).build(),
            new PageableBuilder().pageNumber(eventQuery.getPageNumber()).pageSize(eventQuery.getPageSize()).build()
        );

        if (alertEventsRepo.getPageElements() == 0) {
            return new Page<>(Collections.emptyList(), 1, 0, 0);
        }

        List<AlertEventEntity> alertEvents = alertEventsRepo
            .getContent()
            .stream()
            .map(
                new Function<AlertEvent, AlertEventEntity>() {
                    @Override
                    public AlertEventEntity apply(AlertEvent alertEventRepo) {
                        AlertEventEntity alertEvent = new AlertEventEntity();
                        alertEvent.setCreatedAt(alertEventRepo.getCreatedAt());
                        alertEvent.setMessage(alertEventRepo.getMessage());
                        return alertEvent;
                    }
                }
            )
            .collect(toList());

        return new Page<>(
            alertEvents,
            alertEventsRepo.getPageNumber(),
            (int) alertEventsRepo.getPageElements(),
            alertEventsRepo.getTotalElements()
        );
    }

    private Set<AlertTriggerEntity> findByEvent(AlertEventType event) {
        try {
            LOGGER.debug("findByEvent: {}", event);
            Set<AlertTriggerEntity> set = alertTriggerRepository
                .findAll()
                .stream()
                .filter(
                    alert ->
                        alert.isTemplate() &&
                        alert.getEventRules() != null &&
                        alert.getEventRules().stream().map(AlertEventRule::getEvent).collect(Collectors.toList()).contains(event)
                )
                .map(this::convert)
                .sorted(Comparator.comparing(AlertTriggerEntity::getName))
                .collect(Collectors.toCollection(LinkedHashSet::new));
            LOGGER.debug("findByEvent : {} - DONE", set);
            return set;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find alert triggers by event", ex);
            throw new TechnicalManagementException("An error occurs while trying to find alert triggers by event", ex);
        }
    }

    @Override
    public void createDefaults(AlertReferenceType referenceType, String referenceId) {
        if (getStatus().isEnabled()) {
            Set<AlertTriggerEntity> defaultAlerts = findByEvent(AlertEventType.API_CREATE);

            for (AlertTriggerEntity alert : defaultAlerts) {
                AlertTrigger trigger = convert(alert);
                AlertTriggerEntity triggerEntity = convert(trigger);
                triggerEntity.setId(UUID.toString(UUID.random()));
                triggerEntity.setReferenceType(AlertReferenceType.API);
                triggerEntity.setReferenceId(referenceId);
                triggerEntity.setTemplate(false);
                triggerEntity.setEnabled(true);
                triggerEntity.setEventRules(null);
                triggerEntity.setParentId(alert.getId());
                triggerEntity.setCreatedAt(new Date());
                triggerEntity.setUpdatedAt(trigger.getCreatedAt());

                try {
                    create0(convert(triggerEntity));
                } catch (TechnicalException te) {
                    LOGGER.error("Unable to create default alert", te);
                }
            }
        }
    }

    @Override
    public void applyDefaults(final String alertId, final AlertReferenceType referenceType) {
        try {
            final Optional<AlertTrigger> optionalAlert = alertTriggerRepository.findById(alertId);
            if (!optionalAlert.isPresent()) {
                throw new AlertNotFoundException(alertId);
            }
            final AlertTriggerEntity alert = convert(optionalAlert.get());

            if (!alert.isTemplate()) {
                throw new AlertTemplateInvalidException(alertId);
            }

            if (referenceType == AlertReferenceType.API) {
                apiRepository
                    .search(null)
                    .stream()
                    .map(Api::getId)
                    .forEach(
                        new Consumer<String>() {
                            @Override
                            public void accept(String apiId) {
                                try {
                                    boolean create = alertTriggerRepository
                                        .findByReference(AlertReferenceType.API.name(), apiId)
                                        .stream()
                                        .noneMatch(alertTrigger -> alertId.equals(alertTrigger.getParentId()));

                                    if (create) {
                                        AlertTrigger trigger = convert(alert);
                                        AlertTriggerEntity triggerEntity = convert(trigger);
                                        triggerEntity.setId(UUID.toString(UUID.random()));
                                        triggerEntity.setReferenceType(AlertReferenceType.API);
                                        triggerEntity.setReferenceId(apiId);
                                        triggerEntity.setTemplate(false);
                                        triggerEntity.setEnabled(true);
                                        triggerEntity.setEventRules(null);
                                        triggerEntity.setParentId(alertId);
                                        triggerEntity.setCreatedAt(new Date());
                                        triggerEntity.setUpdatedAt(trigger.getCreatedAt());

                                        create0(convert(triggerEntity));
                                    }
                                } catch (TechnicalException te) {
                                    LOGGER.error("Unable to create default alert for API {}", apiId, te);
                                }
                            }
                        }
                    );
            }
        } catch (TechnicalException te) {
            final String msg = "An error occurs while trying to apply template alert " + alertId;
            LOGGER.error(msg, te);
            throw new TechnicalManagementException(msg, te);
        }
    }

    private void enhance(final AlertTriggerEntity trigger, final AlertReferenceType referenceType, final String referenceId) {
        // Notifications
        List<Notification> notifications = trigger.getNotifications();
        if (notifications == null) {
            notifications = new ArrayList<>();
            trigger.setNotifications(notifications);
        }

        // Set the email notifier configuration in case
        notifications.forEach(
            new Consumer<Notification>() {
                @Override
                public void accept(Notification notification) {
                    if (NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID.equalsIgnoreCase(notification.getType())) {
                        setDefaultEmailNotifier(notification);
                    }
                }
            }
        );

        // Filters
        List<Filter> filters = trigger.getFilters();
        if (filters == null) {
            filters = new ArrayList<>();
            trigger.setFilters(filters);
        }

        switch (referenceType) {
            case API:
            case APPLICATION:
                filters.add(StringCondition.equals(referenceType.name().toLowerCase(), referenceId).build());
                break;
        }
    }

    private void setDefaultEmailNotifier(Notification notification) {
        EmailNotifierConfiguration configuration = new EmailNotifierConfiguration();

        if (host == null) {
            configuration.setHost(environment.getProperty("email.host"));
            final String emailPort = environment.getProperty("email.port");
            if (emailPort != null) {
                configuration.setPort(Integer.parseInt(emailPort));
            }
            configuration.setUsername(environment.getProperty("email.username"));
            configuration.setPassword(environment.getProperty("email.password"));
            configuration.setStartTLSEnabled(environment.getProperty("email.properties.starttls.enable", Boolean.class, false));
        } else {
            configuration.setHost(host);
            configuration.setPort(Integer.parseInt(port));
            configuration.setUsername(username);
            configuration.setPassword(password);
            configuration.setStartTLSEnabled(startTLSEnabled);
            configuration.setSslKeyStore(sslKeyStore);
            configuration.setSslKeyStorePassword(sslKeyStorePassword);
            configuration.setSslTrustAll(sslTrustAll);
        }

        try {
            JsonNode emailNode = mapper.readTree(notification.getConfiguration());
            configuration.setFrom(emailNode.path("from").asText());
            configuration.setTo(emailNode.path("to").asText());
            configuration.setSubject(String.format(subject, emailNode.path("subject").asText()));
            configuration.setBody(emailNode.path("body").asText());

            notification.setConfiguration(mapper.writeValueAsString(configuration));
            notification.setType("email-notifier");
        } catch (IOException e) {
            LOGGER.error("Unexpected error while converting system email configuration to email notifier");
        }
    }

    private void checkAlert() {
        if (!parameterService.findAsBoolean(Key.ALERT_ENABLED) || triggerProviderManager.findAll().isEmpty()) {
            throw new AlertUnavailableException();
        }
    }

    private void triggerOrCancelAlert(final Trigger trigger) {
        if (trigger.isEnabled()) {
            pushTrigger(trigger);
        } else {
            disableTrigger(trigger);
        }
    }

    private void pushTrigger(final Trigger trigger) {
        triggerProvider.register(trigger);
    }

    private void disableTrigger(Trigger trigger) {
        trigger.setEnabled(false);
        triggerProvider.unregister(trigger);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        triggerProvider.addListener(
            new TriggerProvider.OnConnectionListener() {
                @Override
                public void doOnConnect() {
                    LOGGER.info("Connected to alerting system. Sync alert triggers...");
                    // On reconnect, ensure to push all the triggers again
                    findAll()
                        .stream()
                        .filter(alertTriggerEntity -> !alertTriggerEntity.isTemplate())
                        .forEach(
                            new Consumer<AlertTriggerEntity>() {
                                @Override
                                public void accept(AlertTriggerEntity alertTriggerEntity) {
                                    enhance(alertTriggerEntity, alertTriggerEntity.getReferenceType(), alertTriggerEntity.getReferenceId());
                                    triggerOrCancelAlert(alertTriggerEntity);
                                }
                            }
                        );
                    LOGGER.info("Alert triggers synchronized with the alerting system.");
                }
            }
        );

        triggerProvider.addListener(
            new TriggerProvider.OnDisconnectionListener() {
                @Override
                public void doOnDisconnect() {
                    LOGGER.error("Connection with the alerting system has been loose.");
                }
            }
        );

        triggerProvider.addListener(
            new TriggerProvider.OnCommandListener() {
                @Override
                public void doOnCommand(Command command) {
                    if (command instanceof AlertNotificationCommand) {
                        handleAlertNotificationCommand((AlertNotificationCommand) command);
                    } else {
                        LOGGER.warn("Unknown alert command: {}", command);
                    }
                }
            }
        );

        triggerProvider.addListener(
            new TriggerProvider.OnCommandResultListener() {
                @Override
                public <T> void doOnCommand(Command command, Handler<T> resultHandler) {
                    Supplier<T> supplier = null;

                    if (command instanceof ResolvePropertyCommand) {
                        supplier = (Supplier<T>) new ResolvePropertyCommandHandler((ResolvePropertyCommand) command);
                    } else {
                        LOGGER.warn("Unknown alert command: {}", command);
                    }

                    if (supplier != null) {
                        resultHandler.handle(supplier.get());
                    } else {
                        resultHandler.handle(null);
                    }
                }
            }
        );
    }

    private void handleAlertNotificationCommand(AlertNotificationCommand command) {
        try {
            AlertEvent alertEvent = new AlertEvent();

            alertEvent.setId(UUID.toString(UUID.random()));
            alertEvent.setAlert(command.getTrigger());
            alertEvent.setCreatedAt(new Date(command.getTimestamp()));
            alertEvent.setUpdatedAt(alertEvent.getCreatedAt());
            alertEvent.setMessage(command.getMessage());

            alertEventRepository.create(alertEvent);
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to create an alert event from command {}" + command;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    private final class ResolvePropertyCommandHandler implements Supplier<Map<String, Map<String, Object>>> {

        private final ResolvePropertyCommand command;

        ResolvePropertyCommandHandler(ResolvePropertyCommand command) {
            this.command = command;
        }

        @Override
        public Map<String, Map<String, Object>> get() {
            Map<String, String> properties = command.getProperties();
            Map<String, Map<String, Object>> values = new HashMap<>();

            if (properties != null) {
                properties
                    .entrySet()
                    .stream()
                    .forEach(
                        new Consumer<Map.Entry<String, String>>() {
                            @Override
                            public void accept(Map.Entry<String, String> entry) {
                                switch (entry.getKey()) {
                                    case FIELD_API:
                                        values.put(entry.getKey(), getAPIMetadata(entry.getValue()));
                                        break;
                                    case FIELD_APPLICATION:
                                        values.put(entry.getKey(), getApplicationMetadata(entry.getValue()));
                                        break;
                                    case FIELD_PLAN:
                                        values.put(entry.getKey(), getPlanMetadata(entry.getValue()));
                                        break;
                                }
                            }
                        }
                    );
            }

            return values;
        }

        private Map<String, Object> getAPIMetadata(String api) {
            Map<String, Object> metadata = new HashMap<>();

            try {
                if (api.equals(UNKNOWN_SERVICE)) {
                    metadata.put(METADATA_NAME, METADATA_UNKNOWN_API_NAME);
                    metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
                } else {
                    ApiEntity apiEntity = apiService.findById(api);
                    metadata = mapper.convertValue(apiEntity, Map.class);
                    metadata.put("id", api);
                    metadata.put("primaryOwner", mapper.convertValue(apiEntity.getPrimaryOwner(), Map.class));
                    metadata.remove("picture");
                    metadata.remove("proxy");
                    metadata.remove("paths");
                    metadata.remove("properties");
                    metadata.remove("services");
                    metadata.remove("resources");
                    metadata.remove("response_templates");
                    metadata.remove("path_mappings");

                    final List<ApiMetadataEntity> metadataList = apiMetadataService.findAllByApi(api);
                    final Map<String, String> mapMetadata = new HashMap<>(metadataList.size());
                    metadataList.forEach(m -> mapMetadata.put(m.getKey(), m.getValue() == null ? m.getDefaultValue() : m.getValue()));
                    metadata.put("metadata", mapper.convertValue(mapMetadata, Map.class));
                }
            } catch (ApiNotFoundException anfe) {
                metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                metadata.put(METADATA_NAME, METADATA_DELETED_API_NAME);
            }

            return metadata;
        }

        private Map<String, Object> getApplicationMetadata(String application) {
            Map<String, Object> metadata = new HashMap<>();

            try {
                if (application.equals(UNKNOWN_SERVICE)) {
                    metadata.put(METADATA_NAME, METADATA_UNKNOWN_APPLICATION_NAME);
                    metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
                } else {
                    ApplicationEntity applicationEntity = applicationService.findById(application);
                    metadata = mapper.convertValue(applicationEntity, Map.class);
                    metadata.remove("picture");
                }
            } catch (ApplicationNotFoundException anfe) {
                metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                metadata.put(METADATA_NAME, METADATA_DELETED_APPLICATION_NAME);
            }

            return metadata;
        }

        private Map<String, Object> getPlanMetadata(String plan) {
            Map<String, Object> metadata = new HashMap<>();

            try {
                PlanEntity planEntity = planService.findById(plan);
                metadata = mapper.convertValue(planEntity, Map.class);
            } catch (PlanNotFoundException anfe) {
                metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                metadata.put(METADATA_NAME, METADATA_DELETED_PLAN_NAME);
            }

            return metadata;
        }
    }

    private int countEvents(final String triggerId, final long from, final long to) {
        return (int) alertEventRepository
            .search(
                new AlertEventCriteria.Builder().alert(triggerId).from(from).to(to).build(),
                new PageableBuilder().pageNumber(0).pageSize(1).build()
            )
            .getTotalElements();
    }

    private Optional<AlertEvent> getLastEvent(final String triggerId) {
        Page<AlertEvent> alertEventsRepo = alertEventRepository.search(
            new AlertEventCriteria.Builder().alert(triggerId).from(0).to(0).build(),
            new PageableBuilder().pageNumber(0).pageSize(1).build()
        );

        return (alertEventsRepo.getPageElements() == 0) ? Optional.empty() : Optional.of(alertEventsRepo.getContent().get(0));
    }

    private AlertTrigger convert(final AlertTriggerEntity alertEntity) {
        final AlertTrigger alert = new AlertTrigger();
        alert.setId(alertEntity.getId());
        alert.setName(alertEntity.getName());
        alert.setDescription(alertEntity.getDescription());
        alert.setReferenceId(alertEntity.getReferenceId());
        alert.setReferenceType(alertEntity.getReferenceType().name());
        alert.setEnabled(alertEntity.isTemplate() || alertEntity.isEnabled());
        alert.setType(alertEntity.getType());
        alert.setSeverity(alertEntity.getSeverity().name());
        alert.setTemplate(alertEntity.isTemplate());
        alert.setParentId(alertEntity.getParentId());
        alert.setCreatedAt(alertEntity.getCreatedAt());

        if (alertEntity.getEventRules() != null && !alertEntity.getEventRules().isEmpty()) {
            alert.setEventRules(
                alertEntity
                    .getEventRules()
                    .stream()
                    .map(alertEventRuleEntity -> new AlertEventRule(AlertEventType.valueOf(alertEventRuleEntity.getEvent().toUpperCase())))
                    .collect(toList())
            );
        }

        try {
            String definition = mapper.writeValueAsString(alertEntity);
            alert.setDefinition(definition);
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurs while transforming the alert trigger definition into string", ex);
        }

        return alert;
    }

    private AlertTrigger convert(final NewAlertTriggerEntity alertEntity) {
        final AlertTrigger alert = new AlertTrigger();
        alert.setId(UUID.toString(UUID.random()));
        alertEntity.setId(alert.getId());
        alert.setName(alertEntity.getName());
        alert.setDescription(alertEntity.getDescription());
        alert.setReferenceId(alertEntity.getReferenceId());
        alert.setReferenceType(alertEntity.getReferenceType().name());
        alert.setEnabled(alertEntity.isEnabled());
        alert.setType(alertEntity.getType());
        alert.setSeverity(alertEntity.getSeverity().name());
        alert.setTemplate(alertEntity.isTemplate());

        if (alertEntity.getEventRules() != null && !alertEntity.getEventRules().isEmpty()) {
            alert.setEventRules(
                alertEntity
                    .getEventRules()
                    .stream()
                    .map(alertEventRuleEntity -> new AlertEventRule(AlertEventType.valueOf(alertEventRuleEntity.getEvent().toUpperCase())))
                    .collect(toList())
            );
        }

        try {
            String definition = mapper.writeValueAsString(alertEntity);
            alert.setDefinition(definition);
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurs while transforming the alert trigger definition into string", ex);
        }

        return alert;
    }

    private AlertTrigger convert(final UpdateAlertTriggerEntity alertEntity) {
        final AlertTrigger alert = new AlertTrigger();
        alert.setId(UUID.toString(UUID.random()));
        alert.setName(alertEntity.getName());
        alert.setDescription(alertEntity.getDescription());
        alert.setEnabled(alertEntity.isEnabled());
        alert.setSeverity(alertEntity.getSeverity().name());

        if (alertEntity.getEventRules() != null && !alertEntity.getEventRules().isEmpty()) {
            alert.setEventRules(
                alertEntity
                    .getEventRules()
                    .stream()
                    .map(alertEventRuleEntity -> new AlertEventRule(AlertEventType.valueOf(alertEventRuleEntity.getEvent().toUpperCase())))
                    .collect(toList())
            );
        } else {
            alert.setEventRules(null);
        }

        try {
            String definition = mapper.writeValueAsString(alertEntity);
            alert.setDefinition(definition);
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurs while transforming the alert trigger definition into string", ex);
        }

        return alert;
    }

    private AlertTriggerEntity convert(final AlertTrigger alert) {
        try {
            Trigger trigger = mapper.readValue(alert.getDefinition(), Trigger.class);

            final AlertTriggerEntity alertTriggerEntity = new AlertTriggerEntityWrapper(trigger);
            alertTriggerEntity.setDescription(alert.getDescription());
            alertTriggerEntity.setReferenceId(alert.getReferenceId());
            alertTriggerEntity.setReferenceType(AlertReferenceType.valueOf(alert.getReferenceType()));
            alertTriggerEntity.setCreatedAt(alert.getCreatedAt());
            alertTriggerEntity.setUpdatedAt(alert.getUpdatedAt());
            alertTriggerEntity.setType(alert.getType());
            if (alert.getSeverity() != null) {
                alertTriggerEntity.setSeverity(Trigger.Severity.valueOf(alert.getSeverity()));
            } else {
                alertTriggerEntity.setSeverity(Trigger.Severity.INFO);
            }

            alertTriggerEntity.setEnabled(alert.isEnabled());
            alertTriggerEntity.setTemplate(alert.isTemplate());
            alertTriggerEntity.setParentId(alert.getParentId());

            if (alert.getEventRules() != null) {
                alertTriggerEntity.setEventRules(
                    alert
                        .getEventRules()
                        .stream()
                        .map(alertEventRule -> new AlertEventRuleEntity(alertEventRule.getEvent().name()))
                        .collect(Collectors.toList())
                );
            }

            return alertTriggerEntity;
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurs while transforming the alert trigger from its definition", ex);
        }

        return null;
    }
}
