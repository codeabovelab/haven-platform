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

package com.codeabovelab.dm.common.utils;

import org.junit.Test;

import static com.codeabovelab.dm.common.utils.AddressUtils.*;
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

    @Test
    public void setGetHostPortTest() {
        assertEquals("localhost", getHostPort("localhost"));
        assertEquals("localhost", getHostPort("https://localhost"));
        assertEquals("localhost:123", getHostPort("https://localhost:123/"));
        assertEquals("[2001:0db8::765d]", getHostPort("[2001:0db8::765d]"));
        assertEquals("[2001:0db8::765d]:123", getHostPort("[2001:0db8::765d]:123"));
        assertEquals("[2001:0db8::765d]:123", getHostPort("https://[2001:0db8::765d]:123/"));
    }

    @Test
    public void setGetHostTest() {
        assertEquals("localhost", getHost("localhost"));
        assertEquals("localhost", getHost("https://localhost"));
        assertEquals("localhost", getHost("https://localhost/"));
        assertEquals("[2001:0db8::765d]", getHost("[2001:0db8::765d]"));
        assertEquals("[2001:0db8::765d]", getHost("[2001:0db8::765d]:123"));
        assertEquals("[2001:0db8::765d]", getHost("https://[2001:0db8::765d]:123/"));
    }

    @Test
    public void setSetHostTest() {
        assertEquals("test", setHost("localhost", "test"));
        assertEquals("https://test", setHost("https://localhost", "test"));
        assertEquals("https://test:123", setHost("https://localhost:123", "test"));
        assertEquals("https://test/", setHost("https://localhost/", "test"));
        assertEquals("test", setHost("[2001:0db8::765d]", "test"));
        assertEquals("test:123", setHost("[2001:0db8::765d]:123", "test"));
        assertEquals("https://test:123/", setHost("https://[2001:0db8::765d]:123/", "test"));
    }
}