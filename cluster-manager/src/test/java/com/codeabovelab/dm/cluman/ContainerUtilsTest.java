package com.codeabovelab.dm.cluman;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContainerUtilsTest {

    @Test
    public void testParseMemorySettings() throws Exception {
        Long aLong = ContainerUtils.parseMemorySettings("1024");
        assertEquals(1024L, aLong.longValue());
        Long withSuffK = ContainerUtils.parseMemorySettings("1K");
        assertEquals(1024, withSuffK.longValue());
        Long withSuffM = ContainerUtils.parseMemorySettings("1M");
        assertEquals(1024 * 1024L, withSuffM.longValue());

    }

    @Test
    public void testGetRegistryName() throws Exception {
        String registryName = ContainerUtils.getRegistryName("registry.com:8080/stub-core:172");
        assertEquals("registry.com:8080", registryName);

    }
}