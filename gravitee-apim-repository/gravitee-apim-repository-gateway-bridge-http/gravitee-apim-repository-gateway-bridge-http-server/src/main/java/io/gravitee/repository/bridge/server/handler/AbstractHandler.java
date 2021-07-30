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
package io.gravitee.repository.bridge.server.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.vertx.core.AsyncResult;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractHandler {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    protected <T> void handleResponse(final RoutingContext ctx, AsyncResult<T> result) {
        final HttpServerResponse response = ctx.response();

        try {
            if (result.succeeded()) {
                T data = result.result();

                final Class<?> dataClass = data.getClass();

                if (Optional.class.isAssignableFrom(dataClass) && data != null) {
                    Optional<T> opt = (Optional<T>) (data);
                    data = opt.orElse(null);
                }

                if (Collection.class.isAssignableFrom(dataClass) && data == null) {
                    data = (T) Collections.emptySet();
                }

                response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                response.setStatusCode((data != null) ? HttpStatusCode.OK_200 : HttpStatusCode.NOT_FOUND_404);
                response.setChunked(true);

                final ObjectMapper objectMapper = DatabindCodec.prettyMapper();
                objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                try {
                    if (data != null) {
                        response.write(objectMapper.writeValueAsString(data));
                    }

                    response.end();
                } catch (JsonProcessingException jpe) {
                    LOGGER.error("Unable to transform data object to JSON", jpe);
                    response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
                    response.end(jpe.getMessage());
                }
            } else {
                LOGGER.error("Unexpected error from the bridge", result.cause());
                response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
                response.end(result.cause().getMessage());
            }
        } catch (Exception ex) {
            LOGGER.error("Unexpected error from the bridge", result.cause());
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            response.end(result.cause().getMessage());
        }
    }

    protected Set<String> readListParam(String strList) {
        if (!StringUtils.isEmpty(strList)) {
            return Stream.of(strList.split(",")).collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }
}
