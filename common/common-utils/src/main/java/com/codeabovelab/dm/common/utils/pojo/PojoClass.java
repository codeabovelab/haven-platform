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

package com.codeabovelab.dm.common.utils.pojo;

import java.util.Map;
import java.util.Set;

/**
 * representation of java pojo class with it`s properties
 * <p/>
 */
public final class PojoClass {
    private final Class<?> type;
    private final Map<String, Property> properties;

    public PojoClass(Class<?> type) {
        this.type = type;
        this.properties = PojoUtils.load(this.type);
    }

    /**
     * type of pojo
     *
     * @return
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * type property <p/>
     * if property not exist then runtime exception will be thrown
     *
     * @param name
     * @return
     */
    public Property getProperty(String name) {
        final Property property = properties.get(name);
        if (property == null) {
            throw new RuntimeException("can not find property '" + name + "' in " + this.type);
        }
        return property;
    }

    /**
     * retru property or null if it not exist
     *
     * @param name
     * @return
     */
    public Property getPropertyOrNull(String name) {
        return properties.get(name);
    }

    /**
     * unmodifiable set of all type properties
     *
     * @return
     */
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    /**
     * unmodifiable map of properties
     *
     * @return
     */
    public Map<String, Property> getProperties() {
        return properties;
    }
}