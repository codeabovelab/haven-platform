/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.common.meter;

import org.springframework.expression.Expression;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

/**
 * limit checked which limits defined by expression
 */
public class ExpressionLimitChecker implements LimitChecker {

    private final long period;
    private final TimeUnit timeUnit;
    private final Expression expression;

    public ExpressionLimitChecker(Expression expression, long period, TimeUnit timeUnit) {
        Assert.notNull(expression);
        Assert.notNull(timeUnit);
        this.period = period;
        this.timeUnit = timeUnit;
        this.expression = expression;
    }

    @Override
    public LimitExcess check(LimitCheckContext context) {
        Object value = expression.getValue(new MetricExpressionRoot(context.getMetric()));
        if(value == null || value instanceof Boolean && !((Boolean) value)) {
            return null;
        }
        // TODO add diagnostic info
        return LimitExcess.builder()
          .message("'" + expression.getExpressionString() + "' is exceeded")
          .metric(context.getMetricId())
          .build();
    }

    @Override
    public long getPeriod() {
        return timeUnit.toMillis(this.period);
    }
}
