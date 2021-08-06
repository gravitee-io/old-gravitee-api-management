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

import static io.gravitee.rest.api.service.builder.EmailNotificationBuilder.EmailTemplate.SUPPORT_TICKET;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableMap;
import io.gravitee.rest.api.model.ApiModelEntity;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.NewTicketEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.exceptions.EmailRequiredException;
import io.gravitee.rest.api.service.exceptions.SupportUnavailableException;
import io.gravitee.rest.api.service.impl.TicketServiceImpl;
import io.gravitee.rest.api.service.impl.upgrade.DefaultMetadataUpgrader;
import io.gravitee.rest.api.service.notification.PortalHook;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class TicketServiceTest {

    private static final String USERNAME = "my-username";
    private static final String USER_EMAIL = "my@email.com";
    private static final String USER_FIRSTNAME = "Firstname";
    private static final String USER_LASTNAME = "Lastname";
    private static final String API_ID = "my-api-id";
    private static final String APPLICATION_ID = "my-application-id";
    private static final String EMAIL_SUBJECT = "email-subject";
    private static final String EMAIL_CONTENT = "Email\nContent";
    private static final boolean EMAIL_COPY_TO_SENDER = false;
    private static final String EMAIL_SUPPORT = "email@support.com";

    @InjectMocks
    private TicketService ticketService = new TicketServiceImpl();

    @Mock
    private UserService userService;

    @Mock
    private MetadataService metadataService;

    @Mock
    private ApiService apiService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private EmailService emailService;

    @Mock
    private NewTicketEntity newTicketEntity;

    @Mock
    private UserEntity user;

    @Mock
    private ApiModelEntity api;

    @Mock
    private ApplicationEntity application;

    @Mock
    private ParameterService mockParameterService;

    @Mock
    private NotifierService mockNotifierService;

    @Test(expected = SupportUnavailableException.class)
    public void shouldNotCreateIfSupportDisabled() {
        when(mockParameterService.findAsBoolean(Key.PORTAL_SUPPORT_ENABLED)).thenReturn(Boolean.FALSE);
        ticketService.create(USERNAME, newTicketEntity);
        verify(mockNotifierService, never()).trigger(eq(PortalHook.NEW_SUPPORT_TICKET), anyMap());
    }

    @Test(expected = EmailRequiredException.class)
    public void shouldNotCreateIfUserEmailIsMissing() {
        when(mockParameterService.findAsBoolean(Key.PORTAL_SUPPORT_ENABLED)).thenReturn(Boolean.TRUE);
        when(userService.findById(USERNAME)).thenReturn(user);

        ticketService.create(USERNAME, newTicketEntity);
        verify(mockNotifierService, never()).trigger(eq(PortalHook.NEW_SUPPORT_TICKET), anyMap());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotCreateIfDefaultEmailSupportIsMissing() {
        when(mockParameterService.findAsBoolean(Key.PORTAL_SUPPORT_ENABLED)).thenReturn(Boolean.TRUE);
        when(userService.findById(USERNAME)).thenReturn(user);
        when(user.getEmail()).thenReturn(USER_EMAIL);
        when(newTicketEntity.getApi()).thenReturn(API_ID);
        when(apiService.findByIdForTemplates(API_ID, true)).thenReturn(api);

        ticketService.create(USERNAME, newTicketEntity);
        verify(mockNotifierService, never()).trigger(eq(PortalHook.NEW_SUPPORT_TICKET), anyMap());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotCreateIfDefaultEmailSupportHasNotBeenChanged() {
        when(mockParameterService.findAsBoolean(Key.PORTAL_SUPPORT_ENABLED)).thenReturn(Boolean.TRUE);
        when(newTicketEntity.getApi()).thenReturn(API_ID);

        when(userService.findById(USERNAME)).thenReturn(user);
        when(user.getEmail()).thenReturn(USER_EMAIL);
        when(apiService.findByIdForTemplates(API_ID, true)).thenReturn(api);

        final Map<String, String> metadata = new HashMap<>();
        metadata.put(DefaultMetadataUpgrader.METADATA_EMAIL_SUPPORT_KEY, DefaultMetadataUpgrader.DEFAULT_METADATA_EMAIL_SUPPORT);
        when(api.getMetadata()).thenReturn(metadata);

        ticketService.create(USERNAME, newTicketEntity);
        verify(mockNotifierService, never()).trigger(eq(PortalHook.NEW_SUPPORT_TICKET), anyMap());
    }

    @Test
    public void shouldCreateWithApi() {
        when(mockParameterService.findAsBoolean(Key.PORTAL_SUPPORT_ENABLED)).thenReturn(Boolean.TRUE);
        when(newTicketEntity.getApi()).thenReturn(API_ID);
        when(newTicketEntity.getApplication()).thenReturn(APPLICATION_ID);
        when(newTicketEntity.getSubject()).thenReturn(EMAIL_SUBJECT);
        when(newTicketEntity.isCopyToSender()).thenReturn(EMAIL_COPY_TO_SENDER);
        when(newTicketEntity.getContent()).thenReturn(EMAIL_CONTENT);

        when(userService.findById(USERNAME)).thenReturn(user);
        when(user.getEmail()).thenReturn(USER_EMAIL);
        when(user.getFirstname()).thenReturn(USER_FIRSTNAME);
        when(user.getLastname()).thenReturn(USER_LASTNAME);
        when(apiService.findByIdForTemplates(API_ID, true)).thenReturn(api);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);

        final Map<String, String> metadata = new HashMap<>();
        metadata.put(DefaultMetadataUpgrader.METADATA_EMAIL_SUPPORT_KEY, EMAIL_SUPPORT);
        when(api.getMetadata()).thenReturn(metadata);

        ticketService.create(USERNAME, newTicketEntity);

        verify(emailService)
            .sendEmailNotification(
                new EmailNotificationBuilder()
                    .replyTo(USER_EMAIL)
                    .fromName(USER_FIRSTNAME + ' ' + USER_LASTNAME)
                    .to(EMAIL_SUPPORT)
                    .subject(EMAIL_SUBJECT)
                    .copyToSender(EMAIL_COPY_TO_SENDER)
                    .template(SUPPORT_TICKET)
                    .params(ImmutableMap.of("user", user, "api", api, "content", "Email<br />Content", "application", application))
                    .build()
            );
        verify(mockNotifierService, times(1)).trigger(eq(PortalHook.NEW_SUPPORT_TICKET), anyMap());
    }
}
