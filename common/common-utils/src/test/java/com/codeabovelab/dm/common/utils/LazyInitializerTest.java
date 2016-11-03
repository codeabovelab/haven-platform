package com.codeabovelab.dm.common.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Callable;

public class LazyInitializerTest {


    @Test
    public void test() {
        // how we can test it in multithreading?
        LazyInitializer<Integer> lazy = new LazyInitializer<>(new Callable<Integer>() {
            private int  i = 0;
            @Override
            public Integer call() throws Exception {
                return i++;
            }
        });
        Assert.assertEquals((Integer)0, lazy.get());
        Assert.assertEquals((Integer)0, lazy.get());
    }
}