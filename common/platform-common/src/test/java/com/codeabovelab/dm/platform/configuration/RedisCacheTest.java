package com.codeabovelab.dm.patform.configuration;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/*
* @html http://docs.spring.io/spring/docs/current/spring-framework-reference/html/cache.html
**/
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CacheConfigurationTest.class)
public class RedisCacheTest {

    @Autowired CachedRepository cachedRepository;

    @Test
    @Ignore//TODO: fix it
    public void testCache() {
        // according SpEL in com.codeabovelab.dm.patform.configuration.CachedRepository.loadDataByParams():
        // condition = "#param1>0"
        int cacheDataParam = 1;
        int notCacheDataParam = -1;

        cachedRepository.setResult(true); // set result of invoking loadDataByParams method to 'true'

        //invoke method for caching result
        final boolean cachedData = cachedRepository.loadDataByParams(cacheDataParam, System.currentTimeMillis());

        cachedRepository.setResult(false); // change 'result'
        final boolean currentData = cachedRepository.isResult();

        // check cache
        Assert.assertEquals(cachedData, cachedRepository.loadDataByParams(cacheDataParam, System.currentTimeMillis()));
        Assert.assertNotEquals(currentData, cachedRepository.loadDataByParams(cacheDataParam, System.currentTimeMillis()));
        Assert.assertEquals(currentData, cachedRepository.loadDataByParams(notCacheDataParam, System.currentTimeMillis()));
    }

}
