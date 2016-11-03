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

import com.codeabovelab.dm.common.utils.pojo.PojoClass;
import com.codeabovelab.dm.common.utils.pojo.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * Class implement {@link CharSequence} for workaround bug in converter: https://jira.spring.io/browse/SPR-14655
 */
class LazyObjectPrinter implements CharSequence {

    private final Object obj;
    private final ObjectPrinterFactory printerFactory;
    private String res;

    LazyObjectPrinter(ObjectPrinterFactory printerFactory, Object obj) {
        this.printerFactory = printerFactory;
        this.obj = obj;
    }

    @Override
    public int length() {
        return toString().length();
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    @Override
    public String toString() {
        if(res == null) {
            try {
                print();
            } catch (Exception e) {
                res = obj.toString();
            }
        }
        return res;
    }

    private void print() throws Exception {
        StringBuilder sb = new StringBuilder();
        PojoClass pojoClass = new PojoClass(obj.getClass());
        List<Property> props = new ArrayList<>(pojoClass.getProperties().values());
        props.sort((l, r) -> l.getName().compareTo(r.getName()));
        for(Property prop: props) {
            if(prop.getDeclaringClass() == Object.class) {
                continue;
            }
            ObjPrint opa = prop.getAnnotation(ObjPrint.class);
            if(opa != null && opa.ignore()) {
                continue;
            }
            if(sb.length() > 0) {
                sb.append("\n");
            }
            String name = prop.getName();
            sb.append("\t").append(name).append(" = \t");
            Object val = prop.get(obj);
            printerFactory.print(val, sb);
        }
        res = sb.toString();
    }
}
