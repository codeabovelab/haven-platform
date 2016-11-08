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

package com.codeabovelab.dm.common.security.acl;

import com.codeabovelab.dm.common.security.dto.ObjectIdentityData;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class AclUtilsTest {
    @Test
    public void test() {
        ObjectIdentityData[] oids = {
          new ObjectIdentityData("1", 2),
          new ObjectIdentityData("bs", ""),
          new ObjectIdentityData("bs", "21:32"),
          new ObjectIdentityData("ba", 292029393939L),
        };
        for(ObjectIdentityData oid: oids) {
            System.out.println("OID: " + oid);
            String oidStr = AclUtils.toId(oid);
            System.out.println("OID String: " + oidStr);
            ObjectIdentityData readedOid = AclUtils.fromId(oidStr);
            assertEquals(oid,  readedOid);
        }
        ObjectIdentityData[] badOids = {
          new ObjectIdentityData("b:s", ""),
        };
        for(ObjectIdentityData oid: badOids) {
            System.out.println("OID: " + oid);
            try {
                String oidStr = AclUtils.toId(oid);
                fail("The " + oid + " must not be serialized.");
            } catch (IllegalArgumentException e) {
                // as expected
                System.out.println("Expected fail for: " + oid + " with error: " + e.getMessage());
            }
        }
    }

}