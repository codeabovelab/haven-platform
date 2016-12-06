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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class DataSizeTest {

    @Test
    public void test() {
        long srcs[] = new long[]{
          -1, 0, 1,
          -1000, 1000,
          -1024, 1024,
          -999997, 999997,
          -1024 * 1024, 1024 * 1024,
          -1080033, 1080033,
          -1024 * 1024 * 1024, 1024 * 1024 * 1024,
          -1105954078, 1105954078,
        };
        for(long src: srcs) {
            String s = DataSize.toString(src);
            assertNotNull(s);
            long res = DataSize.fromString(s);
            System.out.println("Convert " + src + " to string: " + s + " and parse: " + res);
            assertEquals(src, res);
        }
    }

}