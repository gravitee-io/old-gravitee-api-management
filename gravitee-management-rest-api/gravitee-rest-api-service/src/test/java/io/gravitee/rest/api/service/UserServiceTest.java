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

import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.RESET_PASSWORD;
import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.USER_REGISTRATION;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.management.model.UserReferenceType;
import io.gravitee.repository.management.model.UserStatus;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.audit.AuditEntity;
import io.gravitee.rest.api.model.configuration.identity.GroupMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.RoleMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.common.JWTHelper;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.impl.UserServiceImpl;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

    private static final String USER_SOURCE = "usersource";
    private static final String USER_NAME = "tuser";
    private static final String EMAIL = "user@gravitee.io";
    private static final String FIRST_NAME = "The";
    private static final String LAST_NAME = "User";
    private static final String PASSWORD = "gh2gyf8!zjfnz";
    private static final String JWT_SECRET = "VERYSECURE";
    private static final String ORGANIZATION = "DEFAULT";
    private static final UserReferenceType USER_REFERENCE_TYPE = UserReferenceType.ORGANIZATION;
    private static final Set<UserRoleEntity> ROLES = Collections.singleton(new UserRoleEntity());

    static {
        UserRoleEntity r = ROLES.iterator().next();
        r.setScope(RoleScope.ENVIRONMENT);
        r.setName("USER");
    }

    @InjectMocks
    private UserServiceImpl userService = new UserServiceImpl();

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private RoleService roleService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private ConfigurableEnvironment environment;

    @Mock
    private NewExternalUserEntity newUser;

    @Mock
    private UpdateUserEntity updateUser;

    @Mock
    private User user;

    @Mock
    private Date date;

    @Mock
    private AuditService auditService;

    @Mock
    private NotifierService notifierService;

    @Mock
    private EmailService emailService;

    @Mock
    private ParameterService mockParameterService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private InvitationService invitationService;

    @Mock
    private ApiService apiService;

    @Mock
    private PortalNotificationService portalNotificationService;

    @Mock
    private PortalNotificationConfigService portalNotificationConfigService;

    @Mock
    private GenericNotificationConfigService genericNotificationConfigService;

    @Mock
    private GroupService groupService;

    @Mock
    private SocialIdentityProviderEntity identityProvider;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private TokenService tokenService;

    @Test
    public void shouldFindByUsername() throws TechnicalException {
        when(user.getId()).thenReturn(USER_NAME);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getFirstname()).thenReturn(FIRST_NAME);
        when(user.getLastname()).thenReturn(LAST_NAME);
        when(user.getPassword()).thenReturn(PASSWORD);
        when(userRepository.findBySource(USER_SOURCE, USER_NAME, ORGANIZATION, USER_REFERENCE_TYPE)).thenReturn(of(user));

        final UserEntity userEntity = userService.findBySource(USER_SOURCE, USER_NAME, false);

        assertEquals(USER_NAME, userEntity.getId());
        assertEquals(FIRST_NAME, userEntity.getFirstname());
        assertEquals(LAST_NAME, userEntity.getLastname());
        assertEquals(EMAIL, userEntity.getEmail());
        assertEquals(PASSWORD, userEntity.getPassword());
        assertEquals(null, userEntity.getRoles());
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldNotFindByUsernameBecauseNotExists() throws TechnicalException {
        when(userRepository.findBySource(USER_SOURCE, USER_NAME, ORGANIZATION, USER_REFERENCE_TYPE)).thenReturn(Optional.empty());

        userService.findBySource(USER_SOURCE, USER_NAME, false);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByUsernameBecauseTechnicalException() throws TechnicalException {
        when(userRepository.findBySource(USER_SOURCE, USER_NAME, ORGANIZATION, USER_REFERENCE_TYPE)).thenThrow(TechnicalException.class);

        userService.findBySource(USER_SOURCE, USER_NAME, false);
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        when(newUser.getEmail()).thenReturn(EMAIL);
        when(newUser.getFirstname()).thenReturn(FIRST_NAME);
        when(newUser.getLastname()).thenReturn(LAST_NAME);
        when(newUser.getSource()).thenReturn(USER_SOURCE);
        when(newUser.getSourceId()).thenReturn(USER_NAME);

        when(userRepository.findBySource(USER_SOURCE, USER_NAME, ORGANIZATION, USER_REFERENCE_TYPE)).thenReturn(Optional.empty());

        when(user.getId()).thenReturn(USER_NAME);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getFirstname()).thenReturn(FIRST_NAME);
        when(user.getLastname()).thenReturn(LAST_NAME);
        when(user.getPassword()).thenReturn(PASSWORD);
        when(user.getCreatedAt()).thenReturn(date);
        when(user.getUpdatedAt()).thenReturn(date);
        when(userRepository.create(any(User.class))).thenReturn(user);
        RoleEntity roleEnv = mock(RoleEntity.class);
        when(roleEnv.getScope()).thenReturn(RoleScope.ENVIRONMENT);
        when(roleEnv.getName()).thenReturn("USER");
        when(membershipService.getRoles(MembershipReferenceType.ENVIRONMENT, "DEFAULT", MembershipMemberType.USER, user.getId()))
            .thenReturn(new HashSet<>(Arrays.asList(roleEnv)));
        RoleEntity roleOrg = mock(RoleEntity.class);
        when(roleOrg.getScope()).thenReturn(RoleScope.ORGANIZATION);
        when(roleOrg.getName()).thenReturn("USER");
        when(membershipService.getRoles(MembershipReferenceType.ORGANIZATION, "DEFAULT", MembershipMemberType.USER, user.getId()))
            .thenReturn(new HashSet<>(Arrays.asList(roleOrg)));

        when(organizationService.findById(ORGANIZATION)).thenReturn(new OrganizationEntity());

        final UserEntity createdUserEntity = userService.create(newUser, false);

        verify(userRepository)
            .create(
                argThat(
                    userToCreate ->
                        USER_NAME.equals(userToCreate.getSourceId()) &&
                        USER_SOURCE.equals(userToCreate.getSource()) &&
                        EMAIL.equals(userToCreate.getEmail()) &&
                        FIRST_NAME.equals(userToCreate.getFirstname()) &&
                        LAST_NAME.equals(userToCreate.getLastname()) &&
                        userToCreate.getCreatedAt() != null &&
                        userToCreate.getUpdatedAt() != null &&
                        userToCreate.getCreatedAt().equals(userToCreate.getUpdatedAt())
                )
            );

        assertEquals(USER_NAME, createdUserEntity.getId());
        assertEquals(FIRST_NAME, createdUserEntity.getFirstname());
        assertEquals(LAST_NAME, createdUserEntity.getLastname());
        assertEquals(EMAIL, createdUserEntity.getEmail());
        assertEquals(PASSWORD, createdUserEntity.getPassword());
        assertEquals(ROLES, createdUserEntity.getRoles());
        assertEquals(date, createdUserEntity.getCreatedAt());
        assertEquals(date, createdUserEntity.getUpdatedAt());
    }

    @Test
    public void shouldUpdateUser() throws TechnicalException {
        final String USER_ID = "myuserid";
        final String USER_EMAIL = "my.user@acme.fr";
        final String GIO_SOURCE = "gravitee";

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        user.setSource(GIO_SOURCE);
        user.setReferenceId(ORGANIZATION);
        user.setReferenceType(USER_REFERENCE_TYPE);

        when(userRepository.update(any(User.class)))
            .thenAnswer(
                new Answer<User>() {
                    @Override
                    public User answer(InvocationOnMock invocation) throws Throwable {
                        Object[] args = invocation.getArguments();
                        return (User) args[0];
                    }
                }
            );

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findBySource(GIO_SOURCE, USER_EMAIL, ORGANIZATION, USER_REFERENCE_TYPE)).thenReturn(Optional.empty());

        when(updateUser.getEmail()).thenReturn(USER_EMAIL);
        String UPDATED_LAST_NAME = LAST_NAME + "updated";
        String UPDATED_FIRST_NAME = FIRST_NAME + "updated";
        when(updateUser.getFirstname()).thenReturn(UPDATED_FIRST_NAME);
        when(updateUser.getLastname()).thenReturn(UPDATED_LAST_NAME);
        userService.update(user.getId(), updateUser);

        verify(userRepository)
            .update(
                argThat(
                    userToUpdate ->
                        USER_ID.equals(userToUpdate.getId()) &&
                        GIO_SOURCE.equals(userToUpdate.getSource()) &&
                        USER_EMAIL.equals(userToUpdate.getEmail()) &&
                        USER_EMAIL.equals(userToUpdate.getSourceId()) && // update of sourceId authorized for gravitee source
                        UPDATED_FIRST_NAME.equals(userToUpdate.getFirstname()) &&
                        UPDATED_LAST_NAME.equals(userToUpdate.getLastname())
                )
            );
    }

    @Test
    public void shouldUpdateUser_butNotEmail() throws TechnicalException {
        final String USER_ID = "myuserid";
        final String USER_EMAIL = "my.user@acme.fr";
        final String SOURCE = "gravitee-no-email-update";

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        user.setSource(SOURCE);
        user.setSourceId(USER_ID);
        user.setReferenceId(ORGANIZATION);
        user.setReferenceType(USER_REFERENCE_TYPE);

        when(userRepository.update(any(User.class)))
            .thenAnswer(
                new Answer<User>() {
                    @Override
                    public User answer(InvocationOnMock invocation) throws Throwable {
                        Object[] args = invocation.getArguments();
                        return (User) args[0];
                    }
                }
            );

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        when(updateUser.getEmail()).thenReturn(USER_EMAIL);
        String UPDATED_LAST_NAME = LAST_NAME + "updated";
        String UPDATED_FIRST_NAME = FIRST_NAME + "updated";
        when(updateUser.getFirstname()).thenReturn(UPDATED_FIRST_NAME);
        when(updateUser.getLastname()).thenReturn(UPDATED_LAST_NAME);
        userService.update(user.getId(), updateUser);

        verify(userRepository)
            .update(
                argThat(
                    userToUpdate ->
                        USER_ID.equals(userToUpdate.getId()) &&
                        SOURCE.equals(userToUpdate.getSource()) &&
                        USER_EMAIL.equals(userToUpdate.getEmail()) &&
                        USER_ID.equals(userToUpdate.getSourceId()) && //sourceId shouldn't be updated in this case
                        UPDATED_FIRST_NAME.equals(userToUpdate.getFirstname()) &&
                        UPDATED_LAST_NAME.equals(userToUpdate.getLastname())
                )
            );
    }

    @Test(expected = UserAlreadyExistsException.class)
    public void shouldNotUpdateUser_EmailAlreadyInUse() throws TechnicalException {
        final String USER_ID = "myuserid";
        final String USER_EMAIL = "my.user@acme.fr";
        final String GIO_SOURCE = "gravitee";

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        user.setSource(GIO_SOURCE);
        user.setReferenceId(ORGANIZATION);
        user.setReferenceType(USER_REFERENCE_TYPE);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findBySource(GIO_SOURCE, USER_EMAIL, ORGANIZATION, USER_REFERENCE_TYPE)).thenReturn(Optional.of(new User()));

        when(updateUser.getEmail()).thenReturn(USER_EMAIL);
        String UPDATED_LAST_NAME = LAST_NAME + "updated";
        String UPDATED_FIRST_NAME = FIRST_NAME + "updated";
        when(updateUser.getFirstname()).thenReturn(UPDATED_FIRST_NAME);
        when(updateUser.getLastname()).thenReturn(UPDATED_LAST_NAME);
        userService.update(user.getId(), updateUser);

        verify(userRepository, never()).update(any());
    }

    @Test(expected = OrganizationNotFoundException.class)
    public void shouldNotCreateBecauseOrganizationDoesNotExist() throws TechnicalException {
        when(organizationService.findById(ORGANIZATION)).thenThrow(OrganizationNotFoundException.class);

        userService.create(newUser, false);

        verify(userRepository, never()).create(any());
    }

    @Test(expected = UserAlreadyExistsException.class)
    public void shouldNotCreateBecauseExists() throws TechnicalException {
        when(userRepository.findBySource(nullable(String.class), nullable(String.class), nullable(String.class), eq(USER_REFERENCE_TYPE)))
            .thenReturn(of(new User()));

        userService.create(newUser, false);

        verify(userRepository, never()).create(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateBecauseTechnicalException() throws TechnicalException {
        when(userRepository.create(any(User.class))).thenThrow(TechnicalException.class);

        userService.create(newUser, false);

        verify(userRepository, never()).create(any());
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldNotConnectBecauseNotExists() throws TechnicalException {
        when(userRepository.findById(USER_NAME)).thenReturn(Optional.empty());

        userService.connect(USER_NAME);

        verify(userRepository, never()).create(any());
    }

    @Test
    public void shouldCreateDefaultApplication() throws TechnicalException {
        setField(userService, "defaultApplicationForFirstConnection", true);
        when(user.getLastConnectionAt()).thenReturn(null);
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        userService.connect(USER_NAME);

        verify(applicationService, times(1)).create(any(), eq(USER_NAME));
    }

    @Test
    public void shouldNotCreateDefaultApplicationBecauseDisabled() throws TechnicalException {
        setField(userService, "defaultApplicationForFirstConnection", false);
        when(user.getLastConnectionAt()).thenReturn(null);
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        userService.connect(USER_NAME);

        verify(applicationService, never()).create(any(), eq(USER_NAME));
    }

    @Test
    public void shouldNotCreateDefaultApplicationBecauseAlreadyConnected() throws TechnicalException {
        when(user.getLastConnectionAt()).thenReturn(new Date());
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        userService.connect(USER_NAME);

        verify(applicationService, never()).create(any(), eq(USER_NAME));
    }

    @Test(expected = UserRegistrationUnavailableException.class)
    public void shouldNotCreateUserIfRegistrationIsDisabled() {
        when(mockParameterService.findAsBoolean(Key.PORTAL_USERCREATION_ENABLED)).thenReturn(Boolean.FALSE);
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100));

        userService.finalizeRegistration(userEntity);
    }

    @Test(expected = UserNotFoundException.class)
    public void createNewRegistrationUserThatIsNotCreatedYet() throws TechnicalException {
        when(mockParameterService.findAsBoolean(Key.PORTAL_USERCREATION_ENABLED)).thenReturn(Boolean.TRUE);
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(passwordValidator.validate(anyString())).thenReturn(true);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100));
        userEntity.setPassword(PASSWORD);

        userService.finalizeRegistration(userEntity);
    }

    @Test(expected = UserStateConflictException.class)
    public void createNewRegistrationUserWithResetPasswordAction() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100, RESET_PASSWORD.name()));
        userEntity.setPassword(PASSWORD);

        userService.finalizeRegistration(userEntity);
    }

    @Test
    public void changePassword() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(passwordValidator.validate(anyString())).thenReturn(true);

        User user = new User();
        user.setId("CUSTOM_LONG_ID");
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        when(userRepository.findById(USER_NAME)).thenReturn(Optional.of(user));
        when(userRepository.update(any())).thenAnswer(returnsFirstArg());

        ResetPasswordUserEntity userEntity = new ResetPasswordUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100, RESET_PASSWORD.name()));
        userEntity.setPassword(PASSWORD);

        userService.finalizeResetPassword(userEntity);

        verify(auditService)
            .createPortalAuditLog(anyMap(), argThat(evt -> evt.equals(User.AuditEvent.PASSWORD_CHANGED)), any(), any(), any());
    }

    @Test(expected = PasswordFormatInvalidException.class)
    public void changePassword_invalidPwd() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);

        User user = new User();
        user.setId("CUSTOM_LONG_ID");
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        when(userRepository.findById(USER_NAME)).thenReturn(Optional.of(user));

        ResetPasswordUserEntity userEntity = new ResetPasswordUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100, RESET_PASSWORD.name()));
        userEntity.setPassword(PASSWORD);

        userService.finalizeResetPassword(userEntity);
    }

    @Test(expected = UserStateConflictException.class)
    public void changePassword_withInvalidAction() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);

        ResetPasswordUserEntity userEntity = new ResetPasswordUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100));
        userEntity.setPassword(PASSWORD);

        userService.finalizeResetPassword(userEntity);
    }

    @Test(expected = PasswordFormatInvalidException.class)
    public void createAlreadyPreRegisteredUser_invalidPassword() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(mockParameterService.findAsBoolean(Key.PORTAL_USERCREATION_ENABLED)).thenReturn(Boolean.TRUE);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100));
        userEntity.setPassword(PASSWORD);

        userService.finalizeRegistration(userEntity);
    }

    @Test
    public void createAlreadyPreRegisteredUser() throws TechnicalException {
        when(mockParameterService.findAsBoolean(Key.PORTAL_USERCREATION_ENABLED)).thenReturn(Boolean.TRUE);
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(mockParameterService.findAsBoolean(Key.PORTAL_USERCREATION_ENABLED)).thenReturn(Boolean.TRUE);
        when(passwordValidator.validate(anyString())).thenReturn(true);

        User user = new User();
        user.setId("CUSTOM_LONG_ID");
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        when(userRepository.findById(USER_NAME)).thenReturn(Optional.of(user));
        when(userRepository.update(any(User.class))).thenReturn(user);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100));
        userEntity.setPassword(PASSWORD);

        userService.finalizeRegistration(userEntity);

        verify(userRepository)
            .update(
                argThat(
                    userToCreate ->
                        "CUSTOM_LONG_ID".equals(userToCreate.getId()) &&
                        EMAIL.equals(userToCreate.getEmail()) &&
                        FIRST_NAME.equals(userToCreate.getFirstname()) &&
                        LAST_NAME.equals(userToCreate.getLastname())
                )
            );
    }

    @Test(expected = UserRegistrationUnavailableException.class)
    public void shouldValidateJWTokenAndFail() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 - 100));
        userEntity.setPassword(PASSWORD);

        verify(userRepository, never()).findBySource(USER_SOURCE, USER_NAME, ORGANIZATION, USER_REFERENCE_TYPE);

        userService.finalizeRegistration(userEntity);
    }

    @Test
    public void shouldResetPassword() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(environment.getProperty("user.creation.token.expire-after", Integer.class, DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER))
            .thenReturn(1000);
        when(user.getId()).thenReturn(USER_NAME);
        when(user.getSource()).thenReturn("gravitee");
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        when(auditService.search(argThat(arg -> arg.getEvents().contains(User.AuditEvent.PASSWORD_RESET.name()))))
            .thenReturn(mock(MetadataPage.class));

        userService.resetPassword(USER_NAME);

        verify(user, never()).setPassword(null);
        verify(userRepository, never()).update(user);
        verify(emailService).sendAsyncEmailNotification(any());
    }

    @Test
    public void shouldResetPassword_auditEventNotMatch() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(environment.getProperty("user.creation.token.expire-after", Integer.class, DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER))
            .thenReturn(1000);
        when(user.getId()).thenReturn(USER_NAME);
        when(user.getSource()).thenReturn("gravitee");
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        MetadataPage mdPage = mock(MetadataPage.class);
        AuditEntity entity1 = new AuditEntity();
        entity1.setProperties(Collections.singletonMap("USER", "unknown"));
        when(mdPage.getContent()).thenReturn(Arrays.asList(entity1));
        when(auditService.search(argThat(arg -> arg.getEvents().contains(User.AuditEvent.PASSWORD_RESET.name())))).thenReturn(mdPage);

        userService.resetPassword(USER_NAME);

        verify(user, never()).setPassword(null);
        verify(userRepository, never()).update(user);
        verify(emailService).sendAsyncEmailNotification(any());
    }

    @Test(expected = PasswordAlreadyResetException.class)
    public void shouldNotResetPassword_AlreadyReset() throws TechnicalException {
        when(user.getId()).thenReturn(USER_NAME);
        when(user.getSource()).thenReturn("gravitee");
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        MetadataPage mdPage = mock(MetadataPage.class);
        AuditEntity entity1 = new AuditEntity();
        entity1.setProperties(Collections.singletonMap("USER", USER_NAME));
        when(mdPage.getContent()).thenReturn(Arrays.asList(entity1));
        when(auditService.search(argThat(arg -> arg.getEvents().contains(User.AuditEvent.PASSWORD_RESET.name())))).thenReturn(mdPage);

        userService.resetPassword(USER_NAME);

        verify(user, never()).setPassword(null);
        verify(userRepository, never()).update(user);
        verify(emailService, never()).sendAsyncEmailNotification(any());
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldNotResetPasswordCauseUserNotFound() throws TechnicalException {
        when(userRepository.findById(USER_NAME)).thenReturn(empty());
        userService.resetPassword(USER_NAME);
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldFailWhileResettingPassword() throws TechnicalException {
        when(userRepository.findBySource(any(), any(), any(), any())).thenReturn(Optional.empty());
        userService.resetPasswordFromSourceId("my@email.com", "HTTP://MY-RESET-PAGE");
    }

    @Test(expected = UserNotActiveException.class)
    public void shouldFailWhileResettingPasswordWhenUserFoundIsNotActive() throws TechnicalException {
        User user = new User();
        user.setId(USER_NAME);
        user.setSource("gravitee");
        user.setStatus(UserStatus.ARCHIVED);
        when(userRepository.findBySource(any(), any(), any(), any())).thenReturn(Optional.of(user));

        userService.resetPasswordFromSourceId("my@email.com", "HTTP://MY-RESET-PAGE");
    }

    @Test(expected = UserNotInternallyManagedException.class)
    public void shouldFailWhileResettingPasswordWhenUserFoundIsNotInternallyManaged() throws TechnicalException {
        User user = new User();
        user.setId(USER_NAME);
        user.setSource("not gravitee");
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findBySource(any(), any(), any(), any())).thenReturn(Optional.of(user));
        when(userRepository.findById(any())).thenReturn(Optional.of(user));

        userService.resetPasswordFromSourceId("my@email.com", "HTTP://MY-RESET-PAGE");
    }

    @Test(expected = UserNotInternallyManagedException.class)
    public void shouldNotResetPasswordCauseUserNotInternallyManaged() throws TechnicalException {
        when(user.getSource()).thenReturn("external");
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        userService.resetPassword(USER_NAME);
    }

    private String createJWT(long expirationSeconds, String action) {
        Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);

        Date issueAt = new Date();
        Instant expireAt = issueAt.toInstant().plus(Duration.ofSeconds(expirationSeconds));

        return JWT
            .create()
            .withIssuer(environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER))
            .withIssuedAt(issueAt)
            .withExpiresAt(Date.from(expireAt))
            .withSubject(USER_NAME)
            .withClaim(JWTHelper.Claims.EMAIL, EMAIL)
            .withClaim(JWTHelper.Claims.FIRSTNAME, FIRST_NAME)
            .withClaim(JWTHelper.Claims.LASTNAME, LAST_NAME)
            .withClaim(JWTHelper.Claims.ACTION, action)
            .sign(algorithm);
        /*
        HashMap<String, Object> claims = new HashMap<>();
        claims.put(JWTHelper.Claims.SUBJECT, USER_NAME);
        claims.put(JWTHelper.Claims.EMAIL, EMAIL);
        claims.put(JWTHelper.Claims.FIRSTNAME, FIRST_NAME);
        claims.put(JWTHelper.Claims.LASTNAME, LAST_NAME);
        claims.put(JWTHelper.Claims.ACTION, USER_REGISTRATION);
        claims.put("exp", expirationSeconds);
        return new JWTSigner(JWT_SECRET).sign(claims);
         */
    }

    private String createJWT(long expirationSeconds) {
        return createJWT(expirationSeconds, USER_REGISTRATION.name());
    }

    @Test
    public void shouldNotDeleteIfAPIPO() throws TechnicalException {
        ApiEntity apiEntity = mock(ApiEntity.class);
        PrimaryOwnerEntity primaryOwnerEntity = mock(PrimaryOwnerEntity.class);
        when(apiEntity.getPrimaryOwner()).thenReturn(primaryOwnerEntity);
        when(primaryOwnerEntity.getId()).thenReturn(USER_NAME);
        when(apiService.findByUser(USER_NAME, null, false)).thenReturn(Collections.singleton(apiEntity));

        try {
            userService.delete(USER_NAME);
            fail("should throw StillPrimaryOwnerException");
        } catch (StillPrimaryOwnerException e) {
            //success
            verify(membershipService, never()).removeMemberMemberships(MembershipMemberType.USER, USER_NAME);
            verify(userRepository, never()).update(any());
            verify(searchEngineService, never()).delete(any(), eq(false));
        }
    }

    @Test
    public void shouldNotDeleteIfApplicationPO() throws TechnicalException {
        ApplicationListItem applicationListItem = mock(ApplicationListItem.class);
        PrimaryOwnerEntity primaryOwnerEntity = mock(PrimaryOwnerEntity.class);
        when(applicationListItem.getPrimaryOwner()).thenReturn(primaryOwnerEntity);
        when(primaryOwnerEntity.getId()).thenReturn(USER_NAME);
        when(applicationService.findByUser(USER_NAME)).thenReturn(Collections.singleton(applicationListItem));

        try {
            userService.delete(USER_NAME);
            fail("should throw StillPrimaryOwnerException");
        } catch (StillPrimaryOwnerException e) {
            //success
            verify(membershipService, never()).removeMemberMemberships(MembershipMemberType.USER, USER_NAME);
            verify(userRepository, never()).update(any());
            verify(searchEngineService, never()).delete(any(), eq(false));
        }
    }

    @Test
    public void shouldDeleteUnanonymize() throws TechnicalException {
        String userId = "userId";
        String firstName = "first";
        String lastName = "last";
        String email = "email";
        when(apiService.findByUser(userId, null, false)).thenReturn(Collections.emptySet());
        when(applicationService.findByUser(userId)).thenReturn(Collections.emptySet());
        User user = new User();
        user.setId(userId);
        user.setSourceId("sourceId");
        Date updatedAt = new Date(1234567890L);
        user.setUpdatedAt(updatedAt);
        user.setFirstname(firstName);
        user.setLastname(lastName);
        user.setEmail(email);
        when(userRepository.findById(userId)).thenReturn(of(user));

        userService.delete(userId);

        verify(apiService, times(1)).findByUser(userId, null, false);
        verify(applicationService, times(1)).findByUser(userId);
        verify(membershipService, times(1)).removeMemberMemberships(MembershipMemberType.USER, userId);
        verify(userRepository, times(1))
            .update(
                argThat(
                    new ArgumentMatcher<User>() {
                        @Override
                        public boolean matches(User user) {
                            return (
                                userId.equals(user.getId()) &&
                                UserStatus.ARCHIVED.equals(user.getStatus()) &&
                                "deleted-sourceId".equals(user.getSourceId()) &&
                                !updatedAt.equals(user.getUpdatedAt()) &&
                                firstName.equals(user.getFirstname()) &&
                                lastName.equals(user.getLastname()) &&
                                email.equals(user.getEmail())
                            );
                        }
                    }
                )
            );
        verify(searchEngineService, times(1)).delete(any(), eq(false));
        verify(portalNotificationService, times(1)).deleteAll(user.getId());
        verify(portalNotificationConfigService, times(1)).deleteByUser(user.getId());
        verify(genericNotificationConfigService, times(1)).deleteByUser(eq(user));
        verify(tokenService, times(1)).revokeByUser(userId);
    }

    @Test
    public void shouldDeleteAnonymize() throws TechnicalException {
        setField(userService, "anonymizeOnDelete", true);

        String userId = "userId";
        String firstName = "first";
        String lastName = "last";
        String email = "email";
        when(apiService.findByUser(userId, null, false)).thenReturn(Collections.emptySet());
        when(applicationService.findByUser(userId)).thenReturn(Collections.emptySet());
        User user = new User();
        user.setId(userId);
        user.setSourceId("sourceId");
        Date updatedAt = new Date(1234567890L);
        user.setUpdatedAt(updatedAt);
        user.setFirstname(firstName);
        user.setLastname(lastName);
        user.setEmail(email);
        user.setPicture("picture");
        when(userRepository.findById(userId)).thenReturn(of(user));

        userService.delete(userId);

        verify(apiService, times(1)).findByUser(userId, null, false);
        verify(applicationService, times(1)).findByUser(userId);
        verify(membershipService, times(1)).removeMemberMemberships(MembershipMemberType.USER, userId);
        verify(userRepository, times(1))
            .update(
                argThat(
                    new ArgumentMatcher<User>() {
                        @Override
                        public boolean matches(User user) {
                            return (
                                userId.equals(user.getId()) &&
                                UserStatus.ARCHIVED.equals(user.getStatus()) &&
                                ("deleted-" + userId).equals(user.getSourceId()) &&
                                !updatedAt.equals(user.getUpdatedAt()) &&
                                "Unknown".equals(user.getFirstname()) &&
                                user.getLastname().isEmpty() &&
                                user.getEmail() == null &&
                                user.getPicture() == null
                            );
                        }
                    }
                )
            );
        verify(searchEngineService, times(1)).delete(any(), eq(false));
        verify(portalNotificationService, times(1)).deleteAll(user.getId());
        verify(portalNotificationConfigService, times(1)).deleteByUser(user.getId());
        verify(genericNotificationConfigService, times(1)).deleteByUser(eq(user));
        verify(tokenService, times(1)).revokeByUser(userId);
    }

    @Test(expected = EmailRequiredException.class)
    public void shouldThrowEmailRequiredExceptionWhenMissingMailInUserInfo() throws IOException {
        reset(identityProvider);

        Map<String, String> wrongUserProfileMapping = new HashMap<>();
        wrongUserProfileMapping.put(SocialIdentityProviderEntity.UserProfile.EMAIL, "theEmail");
        wrongUserProfileMapping.put(SocialIdentityProviderEntity.UserProfile.ID, "theEmail");
        wrongUserProfileMapping.put(SocialIdentityProviderEntity.UserProfile.SUB, "sub");
        wrongUserProfileMapping.put(SocialIdentityProviderEntity.UserProfile.FIRSTNAME, "given_name");
        wrongUserProfileMapping.put(SocialIdentityProviderEntity.UserProfile.LASTNAME, "family_name");
        wrongUserProfileMapping.put(SocialIdentityProviderEntity.UserProfile.PICTURE, "picture");
        when(identityProvider.getUserProfileMapping()).thenReturn(wrongUserProfileMapping);
        when(identityProvider.isEmailRequired()).thenReturn(Boolean.TRUE);

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(identityProvider, userInfo);
    }

    @Test(expected = ExpressionEvaluationException.class)
    public void shouldSpelEvaluationExceptionWhenWrongELGroupsMapping() throws IOException, TechnicalException {
        reset(identityProvider, userRepository);
        mockDefaultEnvironment();

        GroupMappingEntity condition1 = new GroupMappingEntity();
        condition1.setCondition("Some Soup");
        condition1.setGroups(Arrays.asList("Example group", "soft user"));

        GroupMappingEntity condition2 = new GroupMappingEntity();
        condition2.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_6'}");
        condition2.setGroups(Collections.singletonList("Others"));

        GroupMappingEntity condition3 = new GroupMappingEntity();
        condition3.setCondition("{#jsonPath(#profile, '$.job_id') != 'API_BREAKER'}");
        condition3.setGroups(Collections.singletonList("Api consumer"));

        when(identityProvider.getGroupMappings()).thenReturn(Arrays.asList(condition1, condition2, condition3));

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(identityProvider, userInfo);
    }

    @Test
    public void shouldRefreshExistingUser() throws IOException, TechnicalException {
        reset(identityProvider, userRepository);

        mockDefaultEnvironment();

        User user = mockUser();

        when(userRepository.findBySource(null, user.getSourceId(), ORGANIZATION, USER_REFERENCE_TYPE)).thenReturn(Optional.of(user));
        when(userRepository.findById(user.getSourceId())).thenReturn(Optional.of(user));
        when(userRepository.update(user)).thenReturn(user);

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());

        userService.createOrUpdateUserFromSocialIdentityProvider(identityProvider, userInfo);
        verify(userRepository, times(1)).update(refEq(user));
    }

    private User mockUser() {
        User user = new User();
        user.setId("janedoe@example.com");
        user.setSource("oauth2");
        user.setSourceId("janedoe@example.com");
        user.setLastname("Doe");
        user.setFirstname("Jane");
        user.setEmail("janedoe@example.com");
        user.setPicture("http://example.com/janedoe/me.jpg");
        return user;
    }

    @Test
    public void shouldCreateNewUser() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, roleService);

        mockDefaultEnvironment();

        when(roleService.findDefaultRoleByScopes(RoleScope.ORGANIZATION, RoleScope.ENVIRONMENT))
            .thenReturn(Arrays.asList(mockRoleEntity(RoleScope.ORGANIZATION, "USER"), mockRoleEntity(RoleScope.ENVIRONMENT, "USER")));

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());

        userService.createOrUpdateUserFromSocialIdentityProvider(identityProvider, userInfo);
        verify(userRepository, times(1)).create(any(User.class));
    }

    @Test
    public void shouldCreateNewUserWithNoMatchingGroupsMappingFromUserInfo() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, membershipService);
        mockDefaultEnvironment();
        mockGroupsMapping();

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);

        when(identityProvider.getId()).thenReturn("oauth2");
        when(userRepository.findBySource("oauth2", "janedoe@example.com", ORGANIZATION, USER_REFERENCE_TYPE)).thenReturn(Optional.empty());

        when(roleService.findDefaultRoleByScopes(RoleScope.ORGANIZATION, RoleScope.ENVIRONMENT))
            .thenReturn(Arrays.asList(mockRoleEntity(RoleScope.ORGANIZATION, "USER"), mockRoleEntity(RoleScope.ENVIRONMENT, "USER")));

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body_no_matching.json"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(identityProvider, userInfo);

        //verify group creations
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                any(MembershipService.MembershipReference.class),
                any(MembershipService.MembershipMember.class),
                any(MembershipService.MembershipRole.class)
            );
    }

    @Test
    public void shouldCreateNewUserWithGroupsMappingFromUserInfo() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, groupService, roleService, membershipService);
        mockDefaultEnvironment();
        mockGroupsMapping();
        mockRolesMapping();

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);

        when(identityProvider.getId()).thenReturn("oauth2");
        when(userRepository.findBySource("oauth2", "janedoe@example.com", ORGANIZATION, USER_REFERENCE_TYPE)).thenReturn(Optional.empty());

        //mock group search and association
        when(groupService.findById("Example group")).thenReturn(mockGroupEntity("group_id_1", "Example group"));
        when(groupService.findById("soft user")).thenReturn(mockGroupEntity("group_id_2", "soft user"));
        when(groupService.findById("Api consumer")).thenReturn(mockGroupEntity("group_id_4", "Api consumer"));

        // mock role search
        RoleEntity roleOrganizationAdmin = mockRoleEntity(RoleScope.ORGANIZATION, "ADMIN");
        RoleEntity roleOrganizationUser = mockRoleEntity(RoleScope.ORGANIZATION, "USER");
        RoleEntity roleEnvironmentAdmin = mockRoleEntity(RoleScope.ENVIRONMENT, "ADMIN");
        RoleEntity roleApiUser = mockRoleEntity(RoleScope.API, "USER");
        RoleEntity roleApplicationAdmin = mockRoleEntity(RoleScope.APPLICATION, "ADMIN");

        when(roleService.findByScopeAndName(RoleScope.ORGANIZATION, "ADMIN")).thenReturn(Optional.of(roleOrganizationAdmin));
        when(roleService.findByScopeAndName(RoleScope.ORGANIZATION, "USER")).thenReturn(Optional.of(roleOrganizationUser));
        when(roleService.findDefaultRoleByScopes(RoleScope.API, RoleScope.APPLICATION))
            .thenReturn(Arrays.asList(roleApiUser, roleApplicationAdmin));

        when(
            membershipService.updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_1"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "USER"),
                "oauth2"
            )
        )
            .thenReturn(mockMemberEntity());

        when(
            membershipService.updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_2"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "USER"),
                "oauth2"
            )
        )
            .thenReturn(mockMemberEntity());

        when(
            membershipService.updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_2"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"),
                "oauth2"
            )
        )
            .thenReturn(mockMemberEntity());

        when(
            membershipService.updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_4"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "USER"),
                "oauth2"
            )
        )
            .thenReturn(mockMemberEntity());

        when(
            membershipService.updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_4"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"),
                "oauth2"
            )
        )
            .thenReturn(mockMemberEntity());

        when(
            membershipService.updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.ORGANIZATION, "DEFAULT"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.ORGANIZATION, "ADMIN"),
                "oauth2"
            )
        )
            .thenReturn(mockMemberEntity());

        when(
            membershipService.updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.ORGANIZATION, "DEFAULT"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.ORGANIZATION, "USER"),
                "oauth2"
            )
        )
            .thenReturn(mockMemberEntity());

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(identityProvider, userInfo);

        //verify group creations
        verify(membershipService, times(1))
            .updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_1"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "USER"),
                "oauth2"
            );

        verify(membershipService, times(1))
            .updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_1"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"),
                "oauth2"
            );

        verify(membershipService, times(1))
            .updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_2"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "USER"),
                "oauth2"
            );

        verify(membershipService, times(1))
            .updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_2"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"),
                "oauth2"
            );

        verify(membershipService, times(0))
            .updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_3"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "USER"),
                "oauth2"
            );

        verify(membershipService, times(0))
            .updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_3"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"),
                "oauth2"
            );

        verify(membershipService, times(1))
            .updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_4"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "USER"),
                "oauth2"
            );

        verify(membershipService, times(1))
            .updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_4"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"),
                "oauth2"
            );

        verify(membershipService, times(1))
            .updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.ORGANIZATION, "DEFAULT"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.ORGANIZATION, "ADMIN"),
                "oauth2"
            );

        verify(membershipService, times(1))
            .updateRoleToMemberOnReferenceBySource(
                new MembershipService.MembershipReference(MembershipReferenceType.ORGANIZATION, "DEFAULT"),
                new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.ORGANIZATION, "USER"),
                "oauth2"
            );
    }

    @Test
    public void shouldCreateNewUserWithGroupsMappingFromUserInfoWhenGroupIsNotFound() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, groupService, roleService, membershipService);
        mockDefaultEnvironment();
        mockGroupsMapping();

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);

        when(identityProvider.getId()).thenReturn("oauth2");
        when(userRepository.findBySource("oauth2", "janedoe@example.com", ORGANIZATION, USER_REFERENCE_TYPE)).thenReturn(Optional.empty());

        when(roleService.findDefaultRoleByScopes(RoleScope.ORGANIZATION, RoleScope.ENVIRONMENT))
            .thenReturn(Arrays.asList(mockRoleEntity(RoleScope.ORGANIZATION, "USER"), mockRoleEntity(RoleScope.ENVIRONMENT, "USER")));

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(identityProvider, userInfo);

        //verify group creations
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                any(MembershipService.MembershipReference.class),
                any(MembershipService.MembershipMember.class),
                any(MembershipService.MembershipRole.class)
            );
    }

    private void mockDefaultEnvironment() {
        Map<String, String> userProfileMapping = new HashMap<>();
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.EMAIL, "email");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.ID, "email");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.SUB, "sub");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.FIRSTNAME, "given_name");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.LASTNAME, "family_name");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.PICTURE, "picture");
        when(identityProvider.getUserProfileMapping()).thenReturn(userProfileMapping);
    }

    private void mockGroupsMapping() {
        GroupMappingEntity condition1 = new GroupMappingEntity();
        condition1.setCondition(
            "{#jsonPath(#profile, '$.identity_provider_id') == 'idp_5' && #jsonPath(#profile, '$.job_id') != 'API_BREAKER'}"
        );
        condition1.setGroups(Arrays.asList("Example group", "soft user"));

        GroupMappingEntity condition2 = new GroupMappingEntity();
        condition2.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_6'}");
        condition2.setGroups(Collections.singletonList("Others"));

        GroupMappingEntity condition3 = new GroupMappingEntity();
        condition3.setCondition("{#jsonPath(#profile, '$.job_id') != 'API_BREAKER'}");
        condition3.setGroups(Collections.singletonList("Api consumer"));

        when(identityProvider.getGroupMappings()).thenReturn(Arrays.asList(condition1, condition2, condition3));
    }

    private void mockRolesMapping() {
        RoleMappingEntity role1 = new RoleMappingEntity();
        role1.setCondition(
            "{#jsonPath(#profile, '$.identity_provider_id') == 'idp_5' && #jsonPath(#profile, '$.job_id') != 'API_BREAKER'}"
        );
        role1.setOrganizations(Collections.singletonList("ADMIN"));

        RoleMappingEntity role2 = new RoleMappingEntity();
        role2.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_6'}");
        role2.setOrganizations(Collections.singletonList("USER"));

        RoleMappingEntity role3 = new RoleMappingEntity();
        role3.setCondition("{#jsonPath(#profile, '$.job_id') != 'API_BREAKER'}");
        role3.setOrganizations(Collections.singletonList("USER"));
        when(identityProvider.getRoleMappings()).thenReturn(Arrays.asList(role1, role2, role3));
    }

    private RoleEntity mockRoleEntity(RoleScope scope, String name) {
        RoleEntity role = new RoleEntity();
        role.setId(scope.name() + "_" + name);
        role.setScope(scope);
        role.setName(name);
        return role;
    }

    private GroupEntity mockGroupEntity(String id, String name) {
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(id);
        groupEntity.setName(name);
        return groupEntity;
    }

    private MemberEntity mockMemberEntity() {
        return mock(MemberEntity.class);
    }

    private InputStream read(String resource) throws IOException {
        return this.getClass().getResourceAsStream(resource);
    }
}
