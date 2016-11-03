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

import org.springframework.util.PatternMatchUtils;

/**
 * Uses {@link PatternMatchUtils }
 */
class PatternFilter extends AbstractTextFilter {
    public static final String PROTO = "pattern";
    private final String pattern;

    public PatternFilter(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String getExpression() {
        return PROTO + ":" + pattern;
    }

    @Override
    protected boolean innerTest(CharSequence text) {
        if(text == null) {
            //obviously that '*' math null strings too
            return "*".equals(pattern);
        }
        return PatternMatchUtils.simpleMatch(pattern, text.toString());
    }
}
