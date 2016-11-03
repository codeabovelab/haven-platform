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

package com.codeabovelab.dm.common.utils;

import com.codeabovelab.dm.common.utils.pojo.PojoClass;
import com.codeabovelab.dm.common.utils.pojo.Property;
import org.apache.commons.beanutils.ConvertUtilsBean2;
import org.apache.commons.beanutils.PropertyUtilsBean;

import java.lang.reflect.Method;

public class PojoBeanUtils {

    private static final PropertyUtilsBean propertyUtilsBean = new PropertyUtilsBean();
    private static final ConvertUtilsBean2 convertUtilsBean = new ConvertUtilsBean2();

    public static Object getValue(Object bean, String name) {
        try {
            return propertyUtilsBean.getNestedProperty(bean, name);
        } catch (Exception e) {
            return null;
        }
    }


    public static Object convert(String value, Class type) {
        return convertUtilsBean.convert(value, type);
    }

    /**
     * Copy properties into lombok-style builders (it builder do not follow java bean convention)
     * @param src source bean object
     * @param dst destination builder object
     * @return dst object
     */
    public static <T> T copyToBuilder(Object src, T dst) {
        PojoClass srcpojo = new PojoClass(src.getClass());
        Class<?> builderClass = dst.getClass();
        Method[] methods = builderClass.getMethods();
        for(Method method: methods) {
            boolean isBuilderSetter = method.getReturnType().equals(builderClass) &&
              method.getParameterCount() == 1;
            if(!isBuilderSetter) {
                continue;
            }
            String propertyName = method.getName();
            Property property = srcpojo.getProperties().get(propertyName);
            if(property == null) {
                continue;
            }
            Object val = property.get(src);
            if(val == null) {
                continue;
            }
            try {
                method.invoke(dst, val);
            } catch (Exception e) {
                //nothing
            }
        }
        return dst;
    }
}
