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

/**
 */
public abstract class AbstractTextFilter implements Filter {
    @Override
    public boolean test(Object o) {
        CharSequence text;
        if(o == null) {
            text = null;
        } else if(o instanceof Named) {
            text = ((Named)o).getName();
        } else if(o instanceof CharSequence) {
            text = (CharSequence) o;
        } else {
            text = o.toString();
        }
        return innerTest(text);
    }

    protected abstract boolean innerTest(CharSequence text);
}
