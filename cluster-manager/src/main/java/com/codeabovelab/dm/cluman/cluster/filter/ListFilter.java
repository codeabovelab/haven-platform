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

import com.codeabovelab.dm.cluman.model.Named;
import com.google.common.base.MoreObjects;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

public class ListFilter extends AbstractFilter<Named> {

    public static final String PROTO = "list";
    private final List<String> names;
    private final String expr;

    public ListFilter(String listNames) {
        this.expr = PROTO + ":" + listNames;
        Assert.notNull(listNames, "NamePattern must not be null");
        names = Arrays.asList(listNames.split(","));
    }

    @Override
    public String getExpression() {
        return expr;
    }

    @Override
    protected boolean innerTest(Named ifc) {
        return names.contains(ifc.getName());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("names", names)
                .add("expr", expr)
                .toString();
    }
}
