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
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.Token;
import io.gravitee.repository.management.model.Token;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class TokenRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/token-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Token> tokens = tokenRepository.findAll();
        assertNotNull(tokens);
        assertEquals(4, tokens.size());
        final Token token123 = tokens.stream().filter(token -> "token123".equals(token.getId())).findAny().get();
        assertToken(token123);
    }

    @Test
    public void shouldFindByReference() throws Exception {
        final List<Token> tokens = tokenRepository.findByReference("USER", "123");
        assertNotNull(tokens);
        assertEquals(2, tokens.size());
        final Token token123 = tokens.stream().filter(token -> "token123".equals(token.getId())).findAny().get();
        assertToken(token123);
    }

    private void assertToken(Token tokenProduct) {
        assertEquals("My personal token", tokenProduct.getName());
        assertEquals("USER", tokenProduct.getReferenceType());
        assertEquals("123", tokenProduct.getReferenceId());
        assertEquals("created at", new Date(1486771200000L), tokenProduct.getCreatedAt());
        assertEquals("expire at", new Date(1486772200000L), tokenProduct.getExpiresAt());
        assertEquals("last use at", new Date(1486773200000L), tokenProduct.getLastUseAt());
    }

    @Test
    public void shouldCreate() throws Exception {
        final Token token = new Token();
        token.setId("new-token");
        token.setToken("token0");
        token.setName("Token name");
        token.setCreatedAt(new Date(1486771200000L));
        token.setExpiresAt(new Date(1486772200000L));
        token.setLastUseAt(new Date(1486773200000L));
        token.setReferenceType("USER");
        token.setReferenceId("456");

        int nbTokensBeforeCreation = tokenRepository.findByReference("USER", "456").size();
        tokenRepository.create(token);
        int nbTokensAfterCreation = tokenRepository.findByReference("USER", "456").size();

        Assert.assertEquals(nbTokensBeforeCreation + 1, nbTokensAfterCreation);

        Optional<Token> optional = tokenRepository.findById("new-token");
        Assert.assertTrue("Token saved not found", optional.isPresent());

        final Token tokenSaved = optional.get();
        Assert.assertEquals("Invalid saved token.", token.getToken(), tokenSaved.getToken());
        Assert.assertEquals("Invalid saved token name.", token.getName(), tokenSaved.getName());
        Assert.assertEquals("Invalid token ref type.", token.getReferenceType(), tokenSaved.getReferenceType());
        Assert.assertEquals("Invalid token ref id.", token.getReferenceId(), tokenSaved.getReferenceId());
        Assert.assertEquals("Invalid token created date.", token.getCreatedAt(), tokenSaved.getCreatedAt());
        Assert.assertEquals("Invalid token expire date.", token.getExpiresAt(), tokenSaved.getExpiresAt());
        Assert.assertEquals("Invalid token last use date.", token.getLastUseAt(), tokenSaved.getLastUseAt());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Token> optional = tokenRepository.findById("token123");
        Assert.assertTrue("Token to update not found", optional.isPresent());
        Assert.assertEquals("Invalid saved token name.", "My personal token", optional.get().getName());

        final Token token = optional.get();
        token.setToken("new_token");
        token.setName("New token name");
        token.setReferenceType("New ref type");
        token.setReferenceId("New ref id");
        token.setCreatedAt(new Date(1486774200000L));
        token.setExpiresAt(new Date(1486775200000L));
        token.setLastUseAt(new Date(1486776200000L));

        int nbTokensBeforeUpdate = tokenRepository.findByReference("USER", "token123").size();
        tokenRepository.update(token);
        int nbTokensAfterUpdate = tokenRepository.findByReference("USER", "token123").size();

        Assert.assertEquals(nbTokensBeforeUpdate, nbTokensAfterUpdate);

        Optional<Token> optionalUpdated = tokenRepository.findById("token123");
        Assert.assertTrue("Token to update not found", optionalUpdated.isPresent());

        final Token tokenUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved token.", "new_token", tokenUpdated.getToken());
        Assert.assertEquals("Invalid saved token name.", "New token name", tokenUpdated.getName());
        Assert.assertEquals("Invalid token ref type.", "New ref type", tokenUpdated.getReferenceType());
        Assert.assertEquals("Invalid token ref id.", "New ref id", tokenUpdated.getReferenceId());
        Assert.assertEquals("Invalid token created date.", new Date(1486774200000L), tokenUpdated.getCreatedAt());
        Assert.assertEquals("Invalid token expire date.", new Date(1486775200000L), tokenUpdated.getExpiresAt());
        Assert.assertEquals("Invalid token expire date.", new Date(1486776200000L), tokenUpdated.getLastUseAt());
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbTokensBeforeDeletion = tokenRepository.findByReference("USER", "123").size();
        tokenRepository.delete("token_to_delete");
        int nbTokensAfterDeletion = tokenRepository.findByReference("USER", "123").size();

        Assert.assertEquals(nbTokensBeforeDeletion - 1, nbTokensAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownToken() throws Exception {
        Token unknownToken = new Token();
        unknownToken.setToken("unknown");
        tokenRepository.update(unknownToken);
        fail("An unknown token should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        tokenRepository.update(null);
        fail("A null token should not be updated");
    }
}
