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

import org.springframework.util.Assert;

import java.util.*;

/**
 * Used for storing different attributes of file
 */
public final class AttributeSupport {

    public interface Adapter<A> {
        A create();
        void setKey(A attr, String key);
        String getKey(A attr);
        void setValue(A attr, String value);
        String getValue(A attr);

        List<A> getAttributes();
    }

    public interface Repository<A> {
        <S extends A> List<S> save(Iterable<S> entities);
        void delete(Iterable<? extends A> entities);
    }

    /**
     * used for updating list of attributes
     * @param repository
     * @param adapter
     * @param patch
     * @param <A>
     */
    public static <A> void merge(Repository<A> repository, Adapter<A> adapter, Map<String, String> patch) {
        Map<String, String> map = new HashMap<>(patch);
        List<A> deleted = new ArrayList<>();
        List<A> properties = adapter.getAttributes();
        if(properties != null) {
            Iterator<A> i = properties.iterator();
            while(i.hasNext()) {
                A property = i.next();
                final String name = adapter.getKey(property);
                if(!map.containsKey(name)) {
                    continue;
                }
                String data = map.get(name);
                if(data == null) {
                    i.remove();
                    map.remove(name);
                    deleted.add(property);
                } else {
                    // patch make modification
                    if(!Objects.equals(data, adapter.getValue(property))) {
                        //update property
                        adapter.setValue(property, data);
                    }
                    // on anyway we must remove property - else it will be added
                    map.remove(name);
                }
            }
        }
        for(Map.Entry<String, String> e: map.entrySet()) {
            A property = adapter.create();
            String name = e.getKey();
            adapter.setKey(property, name);
            String value = e.getValue();
            Assert.notNull(value, name + " is null");
            adapter.setValue(property, value);
            properties.add(property);
        }
        repository.save(properties);
        if(!deleted.isEmpty()) {
            repository.delete(deleted);
        }
    }
}
