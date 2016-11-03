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

package com.codeabovelab.dm.cluman.objprinter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

/**
 */
@Component
public class ObjectPrinterFactory {
    private ConversionService conversionService;

    @Autowired
    public ObjectPrinterFactory(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * Create object which print argument in human readable format in {@link Object#toString()}
     */
    public Object printer(Object src) {
        return new LazyObjectPrinter(this, src);
    }

    void print(Object val, StringBuilder to) {
        String res;
        try {
            res = conversionService.convert(val, String.class);
        } catch (Exception e) {
            res = val.toString();
        }
        to.append(res);
    }
}
