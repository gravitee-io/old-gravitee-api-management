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
package io.gravitee.gateway.security.oauth2;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import java.text.ParseException;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LazyJwtToken {

    private final String token;

    private Map<String, Object> headers;

    private Map<String, Object> claims;

    private boolean parsed = false;

    LazyJwtToken(final String token) {
        this.token = token;
    }

    public Map<String, Object> getHeaders() {
        parse();
        return headers;
    }

    public Map<String, Object> getClaims() {
        parse();
        return claims;
    }

    private void parse() {
        if (! parsed) {
            parsed = true;

            try {
                JWT jwt = JWTParser.parse(token);
                headers = jwt.getHeader().toJSONObject();
                claims = jwt.getJWTClaimsSet().getClaims();
            } catch (ParseException ex){
                // Nothing to do in case of a bad JWT token
            }
        }
    }
}
