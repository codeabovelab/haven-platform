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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * Factory for expression limit checker
 */
final class ExpressionLimitCheckerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ExpressionLimitCheckerFactory.class);
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final Environment environment;

    public ExpressionLimitCheckerFactory(Environment environment) {
        this.environment = environment;
    }

    /**
     * Create {@link ExpressionLimitChecker } or return null if no limit expression
     * @param checkerSource
     * @return checker or null
     */
    public ExpressionLimitChecker create(ExpressionLimitCheckerSource checkerSource) {
        Assert.notNull(checkerSource, "checkerSource is null");
        checkerSource = configureSource(checkerSource);
        String expressionSource = checkerSource.getExpression();
        if(expressionSource == null) {
            return null;
        }
        Expression expression = parser.parseExpression(expressionSource);
        ExpressionLimitChecker limitChecker = new ExpressionLimitChecker(expression, checkerSource.getPeriod(), checkerSource.getTimeUnit());
        return limitChecker;
    }

    private ExpressionLimitCheckerSource configureSource(ExpressionLimitCheckerSource checkerSource) {
        final String checkerSourceString = environment.getProperty("meter[" + checkerSource.getMetricName() + "].watchdogRule");
        if(checkerSourceString == null) {
            return checkerSource;
        }
        // property contains expression, therefore we need to parse it and modify source
        try {
            Expression checkerSourceExpression = parser.parseExpression(checkerSourceString);
            checkerSourceExpression.getValue(checkerSource);
            return checkerSource;
        } catch (Exception e) {
            LOG.error("", e);
        }
        return checkerSource;
    }
}
