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
package io.gravitee.gateway.handlers.api.flow.condition.evaluation;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.handlers.api.flow.condition.ConditionEvaluator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PathBasedConditionEvaluatorTest {

    private final ConditionEvaluator evaluator = new PathBasedConditionEvaluator();

    @Mock
    private ExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private Flow flow;

    @Before
    public void setUp() {
        when(context.request()).thenReturn(request);
    }

    @Test
    public void shouldEvaluate_notEquals() {
        when(request.pathInfo()).thenReturn("/my/path");
        when(flow.getOperator()).thenReturn(Operator.EQUALS);
        when(flow.getPath()).thenReturn("/my/path2");

        assertFalse(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_notEquals2() {
        when(request.pathInfo()).thenReturn("/my/path2");
        when(flow.getOperator()).thenReturn(Operator.EQUALS);
        when(flow.getPath()).thenReturn("/my/path");

        assertFalse(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_equals() {
        when(request.pathInfo()).thenReturn("/my/path");
        when(flow.getOperator()).thenReturn(Operator.EQUALS);
        when(flow.getPath()).thenReturn("/my/path");

        assertTrue(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_equals_requestTrailingSlash() {
        when(request.pathInfo()).thenReturn("/my/path/");
        when(flow.getOperator()).thenReturn(Operator.EQUALS);
        when(flow.getPath()).thenReturn("/my/path");

        assertTrue(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_equals_flowTrailingSlash() {
        when(request.pathInfo()).thenReturn("/my/path");
        when(flow.getOperator()).thenReturn(Operator.EQUALS);
        when(flow.getPath()).thenReturn("/my/path/");

        assertTrue(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_notStartsWith() {
        when(request.pathInfo()).thenReturn("/my/path");
        when(flow.getOperator()).thenReturn(Operator.STARTS_WITH);
        when(flow.getPath()).thenReturn("/my/path2");

        assertFalse(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_startsWith() {
        when(request.pathInfo()).thenReturn("/my/path/subpath");
        when(flow.getOperator()).thenReturn(Operator.STARTS_WITH);
        when(flow.getPath()).thenReturn("/my/path");

        assertTrue(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_startsWith2() {
        when(request.pathInfo()).thenReturn("/my/path2");
        when(flow.getOperator()).thenReturn(Operator.STARTS_WITH);
        when(flow.getPath()).thenReturn("/my/path");

        assertTrue(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_startsWith_requestTrailingSlash() {
        when(request.pathInfo()).thenReturn("/my/path/subpath");
        when(flow.getOperator()).thenReturn(Operator.STARTS_WITH);
        when(flow.getPath()).thenReturn("/my/path/subpath/");

        assertTrue(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_startsWith_singleSlash() {
        when(request.pathInfo()).thenReturn("/my/path");
        when(flow.getOperator()).thenReturn(Operator.STARTS_WITH);
        when(flow.getPath()).thenReturn("/");

        assertTrue(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_pathParam() {
        when(request.pathInfo()).thenReturn("/my/path/subpath");
        when(flow.getOperator()).thenReturn(Operator.STARTS_WITH);
        when(flow.getPath()).thenReturn("/my/:param");

        assertTrue(evaluator.evaluate(flow, context));
    }
}
