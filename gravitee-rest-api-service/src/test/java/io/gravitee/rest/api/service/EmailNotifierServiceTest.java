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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.rest.api.service.notification.PortalHook;
import io.gravitee.rest.api.service.notifiers.impl.EmailNotifierServiceImpl;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EmailNotifierServiceTest {

    @InjectMocks
    private EmailNotifierServiceImpl service = new EmailNotifierServiceImpl();

    @Mock
    private EmailService mockEmailService;

    @Test
    public void shouldNotSendEmailIfNoConfig() {
        service.trigger(ApiHook.API_STARTED, null, null);
        verify(mockEmailService, never()).sendAsyncEmailNotification(any());
        verify(mockEmailService, never()).sendEmailNotification(any());

        service.trigger(ApiHook.API_STARTED, new GenericNotificationConfig(), null);
        verify(mockEmailService, never()).sendAsyncEmailNotification(any());
        verify(mockEmailService, never()).sendEmailNotification(any());

        GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setConfig("");
        service.trigger(ApiHook.API_STARTED, cfg, null);
        verify(mockEmailService, never()).sendAsyncEmailNotification(any());
        verify(mockEmailService, never()).sendEmailNotification(any());
    }

    @Test
    public void shouldNotSendEmailIfNoHook() {
        GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setConfig("test@mail.com");
        service.trigger(null, cfg, null);
        verify(mockEmailService, never()).sendAsyncEmailNotification(any());
        verify(mockEmailService, never()).sendEmailNotification(any());
    }

    @Test
    public void shouldHaveATemplateForApiHooks() {
        GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setConfig("test@mail.com");
        ApiEntity api = new ApiEntity();
        api.setName("api-name");
        PlanEntity plan = new PlanEntity();
        plan.setId("plan-12345");
        plan.setName("plan-name");
        Map<String, Object> params = new HashMap<>();
        params.put((NotificationParamsBuilder.PARAM_API), api);
        params.put((NotificationParamsBuilder.PARAM_PLAN), plan);
        for (ApiHook hook : ApiHook.values()) {
            if (!ApiHook.MESSAGE.equals(hook)) {
                reset(mockEmailService);
                service.trigger(hook, cfg, params);
                verify(mockEmailService, times(1))
                    .sendAsyncEmailNotification(
                        argThat(
                            notification ->
                                notification.getSubject() != null &&
                                !notification.getSubject().isEmpty() &&
                                notification.getTo() != null &&
                                notification.getTo().length == 1 &&
                                notification.getTo()[0].equals("test@mail.com")
                        )
                    );
                verify(mockEmailService, never()).sendEmailNotification(any());
            }
        }
    }

    @Test
    public void shouldHaveATemplateForApplicationHooks() {
        GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setConfig("test@mail.com");
        ApiEntity api = new ApiEntity();
        api.setName("api-name");
        PlanEntity plan = new PlanEntity();
        plan.setName("plan-name");
        Map<String, Object> params = new HashMap<>();
        params.put((NotificationParamsBuilder.PARAM_API), api);
        params.put((NotificationParamsBuilder.PARAM_PLAN), plan);
        for (ApplicationHook hook : ApplicationHook.values()) {
            reset(mockEmailService);
            service.trigger(hook, cfg, params);
            verify(mockEmailService, times(1))
                .sendAsyncEmailNotification(
                    argThat(
                        notification ->
                            notification.getSubject() != null &&
                            !notification.getSubject().isEmpty() &&
                            notification.getTo() != null &&
                            notification.getTo().length == 1 &&
                            notification.getTo()[0].equals("test@mail.com")
                    )
                );
            verify(mockEmailService, never()).sendEmailNotification(any());
        }
    }

    @Test
    public void shouldHaveATemplateForPortalHooks() {
        GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setConfig("test@mail.com");
        for (PortalHook hook : PortalHook.values()) {
            if (!PortalHook.MESSAGE.equals(hook) && !PortalHook.GROUP_INVITATION.equals(hook)) {
                reset(mockEmailService);
                service.trigger(hook, cfg, Collections.emptyMap());
                verify(mockEmailService, times(1))
                    .sendAsyncEmailNotification(
                        argThat(
                            notification ->
                                notification.getSubject() != null &&
                                !notification.getSubject().isEmpty() &&
                                notification.getTo() != null &&
                                notification.getTo().length == 1 &&
                                notification.getTo()[0].equals("test@mail.com")
                        )
                    );
                verify(mockEmailService, never()).sendEmailNotification(any());
            }
        }
    }

    @Test
    public void shouldHaveATemplateForApplicationHooksWithFreemarker() {
        GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setConfig("test@mail.com, ${api.primaryOwner.email} ");
        ApiEntity api = new ApiEntity();
        api.setName("api-name");
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("primary@owner.com");
        api.setPrimaryOwner(new PrimaryOwnerEntity(userEntity));
        PlanEntity plan = new PlanEntity();
        plan.setId("plan-id");
        plan.setName("plan-name");
        Map<String, Object> params = new HashMap<>();
        params.put((NotificationParamsBuilder.PARAM_API), api);
        params.put((NotificationParamsBuilder.PARAM_PLAN), plan);
        for (ApplicationHook hook : ApplicationHook.values()) {
            reset(mockEmailService);
            service.trigger(hook, cfg, params);
            verify(mockEmailService, times(1))
                .sendAsyncEmailNotification(
                    argThat(
                        notification ->
                            notification.getSubject() != null &&
                            !notification.getSubject().isEmpty() &&
                            notification.getTo() != null &&
                            notification.getTo().length == 2 &&
                            notification.getTo()[0].equals("test@mail.com") &&
                            notification.getTo()[1].equals("primary@owner.com")
                    )
                );
            verify(mockEmailService, never()).sendEmailNotification(any());
        }
    }

    @Test
    public void shouldHaveEmail() {
        GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setConfig("test@mail.com");
        ApiEntity api = new ApiEntity();
        api.setName("api-name");
        PlanEntity plan = new PlanEntity();
        plan.setName("plan-name");
        Map<String, Object> params = new HashMap<>();
        params.put((NotificationParamsBuilder.PARAM_API), api);
        params.put((NotificationParamsBuilder.PARAM_PLAN), plan);

        List<String> mails = service.getMails(cfg, params);
        assertNotNull(mails);
        assertFalse(mails.isEmpty());
        assertThat(mails, CoreMatchers.hasItem(cfg.getConfig()));
    }

    @Test
    public void shouldHaveEmptyEmailList() {
        GenericNotificationConfig cfg = new GenericNotificationConfig();
        Map<String, Object> params = new HashMap<>();
        List<String> mails = service.getMails(cfg, params);
        assertNotNull(mails);
        assertTrue(mails.isEmpty());
    }

    @Test
    public void shouldHaveEmptyEmailList_NoConfig() {
        Map<String, Object> params = new HashMap<>();
        List<String> mails = service.getMails(null, params);
        assertNotNull(mails);
        assertTrue(mails.isEmpty());
    }
}
