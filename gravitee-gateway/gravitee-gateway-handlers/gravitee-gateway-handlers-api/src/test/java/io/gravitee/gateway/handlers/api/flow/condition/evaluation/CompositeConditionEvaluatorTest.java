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
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.handlers.api.flow.condition.CompositeConditionEvaluator;
import io.gravitee.gateway.handlers.api.flow.condition.ConditionEvaluator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CompositeConditionEvaluatorTest {

    @Mock
    private ExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private Flow flow;

    @Test
    public void shouldEvaluate_noCondition() {
        final ConditionEvaluator evaluator = new CompositeConditionEvaluator();

        assertTrue(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_invalidSingleCondition() {
        final ConditionEvaluator condition1 = mock(ConditionEvaluator.class);
        final ConditionEvaluator evaluator = new CompositeConditionEvaluator(condition1);

        when(condition1.evaluate(flow, context)).thenReturn(false);

        assertFalse(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_validSingleCondition() {
        final ConditionEvaluator condition1 = mock(ConditionEvaluator.class);
        final ConditionEvaluator evaluator = new CompositeConditionEvaluator(condition1);

        when(condition1.evaluate(flow, context)).thenReturn(true);

        assertTrue(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_invalidMultipleCondition() {
        final ConditionEvaluator condition1 = mock(ConditionEvaluator.class);
        final ConditionEvaluator condition2 = mock(ConditionEvaluator.class);
        final ConditionEvaluator evaluator = new CompositeConditionEvaluator(condition1, condition2);

        when(condition1.evaluate(flow, context)).thenReturn(true);
        when(condition2.evaluate(flow, context)).thenReturn(false);

        assertFalse(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_validMultipleCondition() {
        final ConditionEvaluator condition1 = mock(ConditionEvaluator.class);
        final ConditionEvaluator condition2 = mock(ConditionEvaluator.class);
        final ConditionEvaluator evaluator = new CompositeConditionEvaluator(condition1, condition2);

        when(condition1.evaluate(flow, context)).thenReturn(true);
        when(condition2.evaluate(flow, context)).thenReturn(true);

        assertTrue(evaluator.evaluate(flow, context));
    }
}
