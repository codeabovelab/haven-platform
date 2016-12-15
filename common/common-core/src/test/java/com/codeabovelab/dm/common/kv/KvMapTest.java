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

package com.codeabovelab.dm.common.kv;

import com.codeabovelab.dm.common.kv.mapping.KvMap;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.common.kv.mapping.KvMapping;
import com.codeabovelab.dm.common.utils.ExecutorUtils;
import com.codeabovelab.dm.common.utils.Uuids;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import lombok.*;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import javax.validation.Validator;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

/**
 */
public class KvMapTest {

    private ExecutorUtils.DeferredExecutor executor = ExecutorUtils.deferred();

    @Data
    public static class Bean {
        private static AtomicInteger counter = new AtomicInteger();
        @KvMapping
        private String text;
        @KvMapping
        private int number;

        public Bean() {
            text = Uuids.longUid();
            number = counter.incrementAndGet();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
              .add("hash", Integer.toUnsignedString(System.identityHashCode(this), 32))
              .add("text", text)
              .add("number", number)
              .toString();
        }
    }

    @Test
    public void test() throws Exception {
        KvMap<Bean> map = KvMap.builder()
          .factory(factory())
          .path("/test/beans")
          .build(Bean.class);
        Assert.assertThat(map.list(), hasSize(0));
        final Bean one = new Bean();
        final String oneKey = "one";
        map.put(oneKey, one);
        final Bean two = new Bean();
        final String twoKey = "two";
        map.put(twoKey, two);
        executor.flush();
        Assert.assertThat(map.list(), contains(oneKey, twoKey));

        Bean newOne = new Bean();
        map.put(oneKey, newOne);
        executor.flush();
        {
            // values must be identity to
            Bean oneActual = map.get(oneKey);
            Assert.assertEquals(newOne, oneActual);
            // it fail, so kv send update event and we reload value,
            // but need to check that update was initiated by us
            Assert.assertTrue(newOne == oneActual);
            Bean twoActual = map.get(twoKey);
            Assert.assertEquals(two, twoActual);
            Assert.assertTrue(two == twoActual);
        }

        map.remove("one");
        executor.flush();
        {
            Bean oneActual = map.get(oneKey);
            Assert.assertNull(oneActual);
        }
        Assert.assertThat(map.list(), contains(twoKey));
    }

    private KvMapperFactory factory() {
        return new KvMapperFactory(new ObjectMapper(),
          InMemoryKeyValueStorage.builder().eventsExecutor(executor).build(),
          mock(TextEncryptor.class),
          mock(Validator.class));
    }
}
