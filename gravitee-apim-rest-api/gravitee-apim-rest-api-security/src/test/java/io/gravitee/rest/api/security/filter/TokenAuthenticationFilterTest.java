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
package io.gravitee.rest.api.security.filter;

import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.management.model.Token;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class TokenAuthenticationFilterTest {

    @Mock
    private CookieGenerator cookieGenerator;

    @Mock
    private UserService userService;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuthoritiesProvider authoritiesProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Test
    public void shouldGenerateAuthorities() throws Exception {
        final String USER_ID = "userid1";
        final String TOKEN = "b4c6102e-6c95-464f-8610-2e6c95064f02";
        final String BEARER = "Bearer " + TOKEN;

        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(
            "JWT_SECRET_TOEKN_TEST",
            cookieGenerator,
            userService,
            tokenService,
            authoritiesProvider
        );

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER);

        final Token token = mock(Token.class);
        when(token.getReferenceId()).thenReturn(USER_ID);
        when(tokenService.findByToken(TOKEN)).thenReturn(token);

        UserEntity user = mock(UserEntity.class);
        when(user.getId()).thenReturn(USER_ID);
        when(userService.findById(USER_ID)).thenReturn(user);

        filter.doFilter(request, response, filterChain);

        verify(authoritiesProvider).retrieveAuthorities(USER_ID);
    }

    @Test
    public void shouldRejectRequest_UnknownToken() throws Exception {
        final String USER_ID = "SomeId";
        final String TOKEN = "b4c6102e-6c95-464f-8610-2e6c95064f02";
        final String BEARER = "Bearer " + TOKEN;

        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(
            "JWT_SECRET_TOEKN_TEST",
            cookieGenerator,
            userService,
            tokenService,
            authoritiesProvider
        );

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER);

        when(tokenService.findByToken(TOKEN)).thenThrow(new IllegalStateException("Token not found"));

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpStatusCode.UNAUTHORIZED_401);
        verify(authoritiesProvider, never()).retrieveAuthorities(USER_ID);
    }

    @Test
    public void shouldRejectRequest_UnknownUser() throws Exception {
        final String USER_ID = "SomeId";
        final String TOKEN = "b4c6102e-6c95-464f-8610-2e6c95064f02";
        final String BEARER = "Bearer " + TOKEN;

        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(
            "JWT_SECRET_TOEKN_TEST",
            cookieGenerator,
            userService,
            tokenService,
            authoritiesProvider
        );

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER);

        final Token token = mock(Token.class);
        when(token.getReferenceId()).thenReturn(USER_ID);
        when(tokenService.findByToken(TOKEN)).thenReturn(token);

        when(userService.findById(USER_ID)).thenThrow(new UserNotFoundException(USER_ID));

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpStatusCode.UNAUTHORIZED_401);
        verify(authoritiesProvider, never()).retrieveAuthorities(USER_ID);
    }
}
