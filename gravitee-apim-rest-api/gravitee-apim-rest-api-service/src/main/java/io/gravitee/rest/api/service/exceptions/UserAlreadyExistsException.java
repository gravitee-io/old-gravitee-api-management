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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.management.model.UserReferenceType;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserAlreadyExistsException extends AbstractManagementException {

    private final String source;
    private final String userId;
    private final String referenceId;
    private final UserReferenceType referenceType;

    public UserAlreadyExistsException(String source, String userId, String referenceId, UserReferenceType referenceType) {
        this.source = source;
        this.userId = userId;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return "A user [" + userId + "] already exists for " + referenceType.name() + " " + referenceId + ".";
    }

    @Override
    public String getTechnicalCode() {
        return "user.exists";
    }

    @Override
    public Map<String, String> getParameters() {
        return new HashMap<String, String>() {
            {
                put("user", userId);
                put("referenceId", referenceId);
                put("referenceType", referenceType.name());
                put("source", source);
            }
        };
    }
}
