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

package com.codeabovelab.dm.cluman.ui.msg;

import com.codeabovelab.dm.cluman.cluster.filter.Filter;
import com.codeabovelab.dm.cluman.cluster.filter.FilterFactory;

/**
 */
class FilterCollector {
    private int counter = 0;
    private final String expr;
    private final Filter filter;

    FilterCollector(FilterFactory ff, String expr) {
        this.expr = expr;
        this.filter = ff.createFilter(expr);
    }

    void collect(Object o) {
        if(filter.test(o)) {
            counter++;
        }
    }

    int getResult() {
        return counter;
    }

    @Override
    public String toString() {
        return "FilterCollector{" +
          "expr='" + expr + '\'' +
          ", counter=" + counter +
          '}';
    }

    UiCountResult.FilteredResult toUi() {
        UiCountResult.FilteredResult ui = new UiCountResult.FilteredResult();
        ui.setCount(counter);
        ui.setFilter(expr);
        return ui;
    }
}
