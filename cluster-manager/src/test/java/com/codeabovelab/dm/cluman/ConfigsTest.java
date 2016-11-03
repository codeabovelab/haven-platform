package com.codeabovelab.dm.cluman;

import com.codeabovelab.dm.cluman.model.ContainerSource;
import org.apache.commons.beanutils.BeanUtils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

public class ConfigsTest {

    @Test
    public void testMap() throws InvocationTargetException, IllegalAccessException {
        ContainerSource createContainerArg = new ContainerSource();

        BeanUtils.populate(createContainerArg, Collections.singletonMap("cpuShares", "23"));

        createContainerArg.getCpuShares();
        Assert.assertNotNull(createContainerArg.getCpuShares());

        ContainerSource createContainerArg2 = new ContainerSource();
        BeanUtils.populate(createContainerArg2, Collections.singletonMap("blkioWeight", "15"));

        BeanUtils.copyProperties(createContainerArg, createContainerArg2);

        Assert.assertNotNull(createContainerArg.getBlkioWeight());

    }


}
