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

package com.codeabovelab.dm.cluman.cluster.filter;

import com.google.common.base.MoreObjects;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;


public abstract class SpelFilter<T> extends AbstractFilter<T> {

    private static final SpelExpressionParser parser = new SpelExpressionParser();
    private final SpelExpression expr;

    public SpelFilter(String expr) {
        this.expr = parser.parseRaw(expr);
    }

    public SpelExpression getExpr() {
        return expr;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("expr", expr)
                .toString();
    }
}
