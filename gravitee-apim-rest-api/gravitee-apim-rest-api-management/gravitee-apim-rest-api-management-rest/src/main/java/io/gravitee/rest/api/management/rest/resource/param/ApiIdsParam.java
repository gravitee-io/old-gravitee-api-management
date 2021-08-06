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
package io.gravitee.rest.api.management.rest.resource.param;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiIdsParam {

    private static final String SEPARATOR = ",";
    private List<String> ids;

    public ApiIdsParam(String param) {
        ids = new ArrayList<>();

        if (param != null) {
            String[] params = param.replaceAll("\\s", "").split(SEPARATOR);
            for (String _param : params) {
                try {
                    if (!_param.isEmpty()) {
                        ids.add(_param);
                    }
                } catch (Exception ex) {
                    // nothing to do
                }
            }
        }
    }

    public List<String> getIds() {
        return ids;
    }
}
