package com.codeabovelab.dm.common.utils;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 */
public class UuidsTest {
    @Test
    public void testValidate() throws Exception {
        Uuids.validate("94528104-2121-46d3-ad47-a9f0b296ed1f");
        Uuids.validate("94528104-2121-46d3-aD47-a9f0b296ed1f");
        try {
            Uuids.validate("94528104-2121-46d3-ad47a9f0b296ed1f");
            fail();
        } catch (IllegalArgumentException e) {

        }
        try {
            Uuids.validate("94528104-2121-46d3-aR47a9f0b296ed1f");
            fail();
        } catch (IllegalArgumentException e) {

        }
    }
}
