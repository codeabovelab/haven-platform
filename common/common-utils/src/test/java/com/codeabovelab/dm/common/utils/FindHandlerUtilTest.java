package com.codeabovelab.dm.common.utils;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class FindHandlerUtilTest {

    public interface Marker {}
    public static class OneType implements Marker {}
    public static class SecondType extends OneType {}

    public static class CallbackImpl<T> implements Callback<T> {
        private final Class<T> clazz;

        public CallbackImpl(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public void call(T arg) {
            this.clazz.cast(arg);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test() {
        Map<Class<?>, Callback<?>> map = new HashMap<>();
        map.put(Marker.class, new CallbackImpl<>(Marker.class));


        ((Callback) FindHandlerUtil.findByClass(Marker.class, map)).call(new OneType());
        ((Callback) FindHandlerUtil.findByClass(Marker.class, map)).call(new SecondType());
        ((Callback) FindHandlerUtil.findByClass(SecondType.class, map)).call(new OneType());
        ((Callback) FindHandlerUtil.findByClass(SecondType.class, map)).call(new SecondType());

        //map.put(OneType.class, new CallbackImpl<>(OneType.class));
        //map.put(SecondType.class, new CallbackImpl<>(SecondType.class));
    }
}