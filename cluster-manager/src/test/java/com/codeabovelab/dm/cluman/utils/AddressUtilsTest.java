/*
 * Copyright 2017 Code Above Lab LLC
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

package com.codeabovelab.dm.cluman.utils;

import org.junit.Test;

import static com.codeabovelab.dm.cluman.utils.AddressUtils.setPort;
import static org.junit.Assert.*;

/**
 */
public class AddressUtilsTest {
    @Test
    public void setPortTest() {
        assertEquals("localhost:123", setPort("localhost", 123));
        assertEquals("localhost:123", setPort("localhost:1586", 123));
        assertEquals("[2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d]:123", setPort("[2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d]", 123));
        assertEquals("[2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d]:123", setPort("[2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d]:154542", 123));
    }
}