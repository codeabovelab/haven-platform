package com.codeabovelab.dm.cluman.cluster.filter;

import com.codeabovelab.dm.cluman.cluster.registry.ImageFilterContext;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RegexFilterTest {

    private final RegexFilter regexFilter = new RegexFilter(".*test.*");

    @Test
    public void test() {
        ImageFilterContext ifc = new ImageFilterContext(null);
        ifc.setName("ttttestttt");
        assertTrue(regexFilter.test(ifc));
        ifc.setName("ttttetstttt");
        assertFalse(regexFilter.test(ifc));

    }
}