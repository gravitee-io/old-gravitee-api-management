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
package io.gravitee.gateway.standalone.policy;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.BufferedReadWriteStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.SimpleReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OverrideResponseContentPolicy {

    public final static String STREAM_POLICY_CONTENT = "Intercepted streamable response content";

    @OnResponse
    public void onResponse(Request request, Response response, PolicyChain policyChain) {
        response.headers().set(HttpHeaders.CONTENT_LENGTH, Integer.toString((STREAM_POLICY_CONTENT.length())));

        policyChain.doNext(request, response);
    }

    @OnResponseContent
    public ReadWriteStream onResponseContent(Request request) {
        return new BufferedReadWriteStream() {

            @Override
            public SimpleReadWriteStream<Buffer> write(Buffer content) {
                // We dot want to get the request content, skipping
                return this;
            }

            @Override
            public void end() {
                Buffer content = Buffer.buffer(STREAM_POLICY_CONTENT);

                // Write content
                super.write(content);

                // Mark the end of content
                super.end();
            }
        };
    }
}
