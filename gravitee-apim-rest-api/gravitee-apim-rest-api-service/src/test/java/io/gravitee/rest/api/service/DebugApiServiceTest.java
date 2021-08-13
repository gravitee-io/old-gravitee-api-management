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
package io.gravitee.rest.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.Plan;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.exceptions.DebugApiInvalidDefinitionVersionException;
import io.gravitee.rest.api.service.exceptions.DebugApiNoValidPlanException;
import io.gravitee.rest.api.service.impl.DebugApiServiceImpl;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DebugApiServiceTest {

    private static final String API_ID = "api#1";
    private static final String ENVIRONMENT_ID = "environment#1";
    private static final String INSTANCE_ID = "instance#1";
    private static final String USER_ID = "user#1";
    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private ApiService apiService;

    @Mock
    private EventService eventService;

    @Mock
    private InstanceService instanceService;

    private DebugApiService debugApiService;

    @Before
    public void setup() {
        debugApiService = new DebugApiServiceImpl(apiService, eventService, objectMapper, instanceService);

        ApiEntity apiEntity = mock(ApiEntity.class);
        when(apiEntity.getReferenceId()).thenReturn(ENVIRONMENT_ID);
        when(apiService.findById(API_ID)).thenReturn(apiEntity);

        InstanceEntity instanceEntity = mock(InstanceEntity.class);
        when(instanceEntity.getId()).thenReturn(INSTANCE_ID);
        when(instanceEntity.getEnvironments()).thenReturn(Sets.newSet(ENVIRONMENT_ID));
        when(instanceService.findAllStarted()).thenReturn(Arrays.asList(instanceEntity));
    }

    @Test
    public void debug_shouldCallEventServiceWithSpecificProperties() {
        DebugApiEntity debugApiEntity = prepareDebugApiEntity(PlanStatus.PUBLISHED, DefinitionVersion.V2);

        debugApiService.debug(API_ID, USER_ID, debugApiEntity);

        ArgumentCaptor<Map<String, String>> propertiesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(eventService).create(eq(EventType.DEBUG_API), anyString(), propertiesCaptor.capture());

        assertThat(propertiesCaptor.getValue())
            .contains(
                entry(Event.EventProperties.API_ID.getValue(), API_ID),
                entry(Event.EventProperties.USER.getValue(), USER_ID),
                entry(Event.EventProperties.API_DEBUG_STATUS.getValue(), ApiDebugStatus.TO_DEBUG.name())
            );
    }

    @Test(expected = DebugApiNoValidPlanException.class)
    public void debug_shouldThrowIfApiHasOnlyDeprecatedPlan() {
        DebugApiEntity debugApiEntity = prepareDebugApiEntity(PlanStatus.DEPRECATED, DefinitionVersion.V2);

        debugApiService.debug(API_ID, USER_ID, debugApiEntity);
    }

    @Test(expected = DebugApiInvalidDefinitionVersionException.class)
    public void debug_shouldThrowIfApiDefinitionIsNotV2() {
        DebugApiEntity debugApiEntity = prepareDebugApiEntity(PlanStatus.PUBLISHED, DefinitionVersion.V1);

        debugApiService.debug(API_ID, USER_ID, debugApiEntity);
    }

    private DebugApiEntity prepareDebugApiEntity(PlanStatus planStatus, DefinitionVersion definitionVersion) {
        Plan deprecatedPlan = new Plan();
        deprecatedPlan.setStatus(planStatus.name());

        DebugApiEntity debugApiEntity = new DebugApiEntity();
        debugApiEntity.setRequest(new HttpRequest());
        debugApiEntity.setGraviteeDefinitionVersion(definitionVersion.getLabel());
        debugApiEntity.setPlans(List.of(deprecatedPlan));

        return debugApiEntity;
    }
}
