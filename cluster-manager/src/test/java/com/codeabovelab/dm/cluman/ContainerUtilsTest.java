package com.codeabovelab.dm.cluman;

import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContainerUtilsTest {

    @Test
    public void testGetRegistryName() throws Exception {
        String registryName = ContainerUtils.getRegistryPrefix("registry.com:8080/stub-core:172");
        assertEquals("registry.com:8080", registryName);

    }
}