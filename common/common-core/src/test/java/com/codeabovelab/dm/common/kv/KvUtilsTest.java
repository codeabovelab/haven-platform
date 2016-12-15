package com.codeabovelab.dm.common.kv;

import com.google.common.base.Splitter;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 */
public class KvUtilsTest {

    public static final Splitter SPLITTER = Splitter.on('/').omitEmptyStrings();

    @Test
    public void testPredicate() {
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

    @Test
    public void testChild() {
        childTestsPack("/test", "/test/one/two/three/");
        childTestsPack("/test", "/test/one/two/three");
        childTestsPack("/test/", "/test/one/two/three");
    }

    private void childTestsPack(String prefix, String path) {
        int i = 0;
        for(String token: SPLITTER.split(path.substring(prefix.length()))) {
            assertEquals(token, KvUtils.child(prefix, path, i));
            i++;
        }
        assertEquals(null, KvUtils.child(prefix, path, i));
        assertEquals(null, KvUtils.child(prefix, path, i + 1));
    }
}
