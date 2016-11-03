package com.codeabovelab.dm.common.kv;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 */
public class KvUtilsPredicateTest {

    @Test
    public void test() {
        String pattern = "/test/*";
        assertTrue(KvUtils.predicate(pattern, "/test"));
        assertFalse(KvUtils.predicate(pattern, "/testnotmatched"));
        assertFalse(KvUtils.predicate(pattern, "/testnotmatched/some"));
        assertFalse(KvUtils.predicate(pattern, "/testnotmatched/some/"));
        assertTrue(KvUtils.predicate(pattern, "/test/"));
        assertTrue(KvUtils.predicate(pattern, "/test/some"));
        assertTrue(KvUtils.predicate(pattern, "/test/some/another"));
        assertTrue(KvUtils.predicate(pattern, "/test/some/another"));
    }
}
