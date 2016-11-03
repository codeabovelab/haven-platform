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

import org.springframework.util.Assert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFilter extends AbstractTextFilter {

    public static final String PROTO = "regex";
    private final Pattern namePattern;
    private final String expr;

    public RegexFilter(String pattern) {
        Assert.notNull(pattern, "pattern must not be null");
        this.expr = PROTO + ":" + pattern;
        this.namePattern = Pattern.compile(pattern);
    }

    @Override
    public String getExpression() {
        return expr;
    }

    @Override
    protected boolean innerTest(CharSequence text) {
        Matcher matcher = namePattern.matcher(text);
        return matcher.matches();
    }

}
