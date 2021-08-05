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

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanValidationType;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UpdatePlanEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.exceptions.PlanGeneralConditionStatusException;
import io.gravitee.rest.api.service.impl.PlanServiceImpl;
import io.gravitee.rest.api.service.processor.PlanSynchronizationProcessor;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanService_UpdateTest {

    private static final String PLAN_ID = "my-plan";
    private static final String API_ID = "my-api";

    @InjectMocks
    private PlanService planService = new PlanServiceImpl();

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private Plan plan;

    @Mock
    private SubscriptionEntity subscription;

    @Mock
    private AuditService auditService;

    @Mock
    private PageService pageService;

    @Mock
    private ApiService apiService;

    @Mock
    private ApiEntity apiEntity;

    @Mock
    private ParameterService parameterService;

    @Mock
    private PlanSynchronizationProcessor synchronizationProcessor;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(true);
        when(apiService.findById(API_ID)).thenReturn(apiEntity);

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());

        planService.update(updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test
    public void shouldUpdateAndUpdateApiDefinition() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(true);
        when(apiEntity.getGraviteeDefinitionVersion()).thenReturn(DefinitionVersion.V2.getLabel());
        when(apiService.findById(API_ID)).thenReturn(apiEntity);

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());

        planService.update(updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT));
        verify(apiService).update(anyString(), any());
    }

    @Test
    public void shouldUpdate_WithNewPublished_GeneralConditionPage() throws TechnicalException {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(true);

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(updatePlan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());
        when(apiService.findById(API_ID)).thenReturn(apiEntity);

        PageEntity unpublishedPage = new PageEntity();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(true);
        doReturn(unpublishedPage).when(pageService).findById(PAGE_ID);

        planService.update(updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test(expected = PlanGeneralConditionStatusException.class)
    public void shouldNotUpdate_WithNotPublished_GeneralConditionPage_PublishPlan() throws TechnicalException {
        shouldNotUpdate_withNotPublished_GCUPage(Plan.Status.PUBLISHED);
    }

    @Test(expected = PlanGeneralConditionStatusException.class)
    public void shouldNotUpdate_WithNotPublished_GeneralConditionPage_DeprecatedPlan() throws TechnicalException {
        shouldNotUpdate_withNotPublished_GCUPage(Plan.Status.DEPRECATED);
    }

    private void shouldNotUpdate_withNotPublished_GCUPage(Plan.Status planStatus) throws TechnicalException {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getStatus()).thenReturn(planStatus);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(true);

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(updatePlan.getGeneralConditions()).thenReturn(PAGE_ID);

        PageEntity unpublishedPage = new PageEntity();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(false);
        doReturn(unpublishedPage).when(pageService).findById(PAGE_ID);

        planService.update(updatePlan);
    }

    @Test
    public void shouldUpdate_withNotPublished_GCUPage_StagingPlan() throws TechnicalException {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(true);
        when(apiService.findById(API_ID)).thenReturn(apiEntity);

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(updatePlan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());

        planService.update(updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT));
    }
}
