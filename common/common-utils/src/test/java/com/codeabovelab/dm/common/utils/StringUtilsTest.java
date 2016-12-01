/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.common.utils;

import com.google.common.base.Splitter;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

/**
 */
public class StringUtilsTest {

    @Test
    public void testReplace() {
        String src = "$AAA$ some\ns$AAA$b\n$AAA$\n$ SSS$AAA$AAA$\n${B}$";
        String exp = "1 some\ns2b\n3\n$ SSS4AAA$\n${B}$\n";
        AtomicInteger counter = new AtomicInteger();
        StringBuilder sb = new StringBuilder();
        for(String line: Splitter.on("\n").split(src)) {
            sb.append(StringUtils.replace(Pattern.compile("\\$[\\w\\{\\}]+\\$"), line, (s) -> {
                if(!"$AAA$".equals(s)) {
                    return s;
                }
                return Integer.toString(counter.incrementAndGet());
            })).append('\n');
        }
        assertEquals(exp, sb.toString());
    }
}
