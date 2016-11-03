package com.codeabovelab.dm.common.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class VersionComparatorTest {

    private final VersionComparator vc = VersionComparator.builder()
      .addLatest("latest").addLatest("nightly")
      .addSuffix("rc")
      .addSuffix("ga")
      .build();
    @Test
    public void test() {
        compare( 0,        "",        "");
        compare( 0,       "1",       "1");
        compare( 0,      null,      null);
        compare( 1,       "1",      null);
        compare( 1,       "1",        "");
        compare( 1,    "1.10",     "1.9");
        compare(-1,     "1.1",     "1.2");
        compare(-1, "1.1.123", "1.9.123");
        compare( 0, "1.9.123", "1.9.123");
        compare( 1, "1.9.124", "1.9.123");
        compare(-1, "1.9.124",  "latest");
        compare(-1,  "latest", "nightly");
        compare(-1,  "1.9_rc",     "1.9");
        compare( 1,  "1.9_ga",  "1.9_rc");
        compare( 0,  "1.9_ga",  "1.9_ga");
        compare(-1,  "1.9_ga",  "1.19_ga");
    }

    private void compare(int expected,  String left, String right) {
        String desc = "'" + left + "' - '" + right + "'";
        int compare = 0, invCompare = 0;
        try {
            compare = vc.compare(left, right);
            invCompare = vc.compare(right, left);
        } catch (Exception e) {
            throw new RuntimeException("on " + desc, e);
        }
        assertEquals("Test commutativity on " + desc, compare, -invCompare);
        assertEquals("Test  on " + desc, expected, compare);
    }
}