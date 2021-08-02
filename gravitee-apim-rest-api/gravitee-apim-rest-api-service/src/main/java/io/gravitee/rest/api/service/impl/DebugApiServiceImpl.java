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

import static java.util.Map.entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.*;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.rest.api.model.DebugApiEntity;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.DebugApiService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.DebugApiInvalidDefinitionVersionException;
import io.gravitee.rest.api.service.exceptions.DebugApiNoValidPlanException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DebugApiServiceImpl implements DebugApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebugApiServiceImpl.class);
    private final ApiService apiService;
    private final EventService eventService;
    private final ObjectMapper objectMapper;

    public DebugApiServiceImpl(ApiService apiService, EventService eventService, ObjectMapper objectMapper) {
        this.apiService = apiService;
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }

    @Override
    public EventEntity debug(String apiId, String userId, DebugApiEntity debugApiEntity) {
        try {
            LOGGER.debug("Debug API : {}", apiId);
            if (!apiService.exists(apiId)) {
                throw new ApiNotFoundException(apiId);
            }

            Map<String, String> properties = Map.ofEntries(
                entry(Event.EventProperties.API_ID.getValue(), apiId),
                entry(Event.EventProperties.USER.getValue(), userId),
                entry(Event.EventProperties.API_DEBUG_STATUS.getValue(), ApiDebugStatus.TO_DEBUG.name())
            );

            DebugApi debugApi = convert(debugApiEntity, apiId);

            validatePlan(debugApi);
            validateDefinitionVersion(apiId, debugApi);

            return eventService.create(EventType.DEBUG_API, objectMapper.writeValueAsString(debugApi), properties);
        } catch (JsonProcessingException ex) {
            LOGGER.error("An error occurs while trying to debug API: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to debug API: " + apiId, ex);
        }
    }

    private void validateDefinitionVersion(String apiId, DebugApi debugApi) {
        if (!debugApi.getDefinitionVersion().equals(DefinitionVersion.V2)) {
            throw new DebugApiInvalidDefinitionVersionException(apiId);
        }
    }

    private void validatePlan(DebugApi debugApi) {
        boolean hasValidPlan = debugApi
            .getPlans()
            .stream()
            .anyMatch(
                plan ->
                    PlanStatus.STAGING.name().equalsIgnoreCase(plan.getStatus()) ||
                    PlanStatus.PUBLISHED.name().equalsIgnoreCase(plan.getStatus())
            );

        if (!hasValidPlan) {
            throw new DebugApiNoValidPlanException(debugApi.getId());
        }
    }

    private DebugApi convert(DebugApiEntity debugApiEntity, String apiId) {
        DebugApi debugApi = new DebugApi();
        debugApi.setId(apiId);
        debugApi.setRequest(debugApiEntity.getRequest());
        debugApi.setResponse(debugApiEntity.getResponse());
        debugApi.setDefinitionVersion(DefinitionVersion.valueOfLabel(debugApiEntity.getGraviteeDefinitionVersion()));
        debugApi.setName(debugApiEntity.getName());
        debugApi.setVersion(debugApiEntity.getVersion());
        debugApi.setProperties(debugApiEntity.getProperties());
        debugApi.setProxy(debugApiEntity.getProxy());
        debugApi.setResources(debugApiEntity.getResources());
        debugApi.setResponseTemplates(debugApiEntity.getResponseTemplates());
        debugApi.setServices(debugApiEntity.getServices());
        debugApi.setTags(debugApiEntity.getTags());
        debugApi.setPaths(debugApiEntity.getPaths());
        debugApi.setFlows(debugApiEntity.getFlows());
        debugApi.setPlans(debugApiEntity.getPlans());

        // Disable logging for the debugged API
        if (debugApiEntity.getProxy() != null && debugApiEntity.getProxy().getLogging() != null) {
            debugApi.getProxy().getLogging().setMode(LoggingMode.NONE);
            debugApi.getProxy().getLogging().setContent(LoggingContent.NONE);
            debugApi.getProxy().getLogging().setScope(LoggingScope.NONE);
        }

        return debugApi;
    }
}
