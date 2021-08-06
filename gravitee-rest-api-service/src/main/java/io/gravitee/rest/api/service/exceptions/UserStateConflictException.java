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
package io.gravitee.rest.api.service.exceptions;

import static io.gravitee.common.http.HttpStatusCode.CONFLICT_409;
import static io.gravitee.common.http.HttpStatusCode.SERVICE_UNAVAILABLE_503;

import java.util.Map;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserStateConflictException extends AbstractManagementException {

    public UserStateConflictException(String message) {
        super(message);
    }

    @Override
    public int getHttpStatusCode() {
        return CONFLICT_409;
    }

    @Override
    public String getTechnicalCode() {
        return "user.state.conflict";
    }

    @Override
    public Map<String, String> getParameters() {
        return null;
    }
}
