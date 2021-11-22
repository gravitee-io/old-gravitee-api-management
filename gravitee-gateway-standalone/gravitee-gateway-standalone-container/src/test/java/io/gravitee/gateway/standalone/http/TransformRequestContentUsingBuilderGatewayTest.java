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
package io.gravitee.gateway.standalone.http;

import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.gateway.standalone.policy.TransformRequestContentUsingBuilderPolicy;
import io.gravitee.gateway.standalone.utils.StringUtils;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.Test;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor(value = "/io/gravitee/gateway/standalone/http/transform-request-content.json")
public class TransformRequestContentUsingBuilderGatewayTest extends AbstractWiremockGatewayTest {

    private static final String BODY_CONTENT = "Content to transform:";

    @Test
    public void shouldTransformRequestContent() throws Exception {
        wireMockRule.stubFor(post("/api").willReturn(
                ok("{{request.body}}").withTransformers("response-template")));

        org.apache.http.client.fluent.Request request = org.apache.http.client.fluent.Request.Post("http://localhost:8082/api");
        request.bodyString(BODY_CONTENT +" {#request.id}", ContentType.TEXT_PLAIN);

        HttpResponse response = request.execute().returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(response.getEntity().getContent());
        String [] parts = responseContent.split(":");

        assertTrue(responseContent.startsWith(BODY_CONTENT));
        assertTrue(UUID.fromString(parts[1].substring(1)) != null);

        wireMockRule.verify(1, postRequestedFor(urlPathEqualTo("/api")));
    }

    @Override
    public void register(ConfigurablePluginManager<PolicyPlugin> policyPluginManager) {
        super.register(policyPluginManager);

        PolicyPlugin transformRequestContentPolicy = PolicyBuilder.build("transform-request-content", TransformRequestContentUsingBuilderPolicy.class);
        policyPluginManager.register(transformRequestContentPolicy);
    }
}
