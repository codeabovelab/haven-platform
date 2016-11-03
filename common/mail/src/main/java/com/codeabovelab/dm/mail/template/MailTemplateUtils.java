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

package com.codeabovelab.dm.mail.template;

import java.util.*;
import java.util.function.UnaryOperator;

/**
 */
public class MailTemplateUtils {
    /**
     * Utility for processing mail header properties.
     * @param processor
     * @param arg
     * @return
     */
    public static Object process(UnaryOperator<Object> processor, Object arg) {
        if (arg instanceof String) {
            return processor.apply(arg);
        }
        if (arg instanceof List) {
            Collection<Object> res = new ArrayList<>();

            processCollection(processor, (Collection) arg, res);
            return res;
        }
        if (arg instanceof Set) {
            Collection<Object> res = new HashSet<>();
            processCollection(processor, (Collection) arg, res);
            return res;
        }
        return arg;
    }


    private static void processCollection(UnaryOperator<Object> processor, Collection<?> arg, Collection<Object> res) {
        // forEach more optimal than iterator
        arg.forEach((item) -> {
            Object resItem = processor.apply(item);
            res.add(resItem);
        });
    }


}
