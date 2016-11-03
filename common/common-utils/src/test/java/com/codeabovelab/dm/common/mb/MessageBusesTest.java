package com.codeabovelab.dm.common.mb;

import org.junit.Assert;
import org.junit.Test;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 */
public class MessageBusesTest {

    private class ValueHolder<T> implements Consumer<T> {
        private int invocations = 0;
        private T value;

        @Override
        public void accept(T o) {
            invocations++;
            this.value = o;
        }

        public int getInvocations() {
            return invocations;
        }

        public T getValue() {
            return value;
        }
    }

    @Test
    public void test() {
        List<String> errors = new ArrayList<>();
        Consumer<ExceptionInfo> eic = (ei) -> {
            String failstring = MessageFormat.format("In bus {0} from {1} at {2} error {3}",
              ei.getBus().getId(),
              ei.getConsumer(),
              ei.getMessage(),
              ei.getThrowable());
            System.out.println(failstring);
            errors.add(failstring);
        };
        MessageBus<String> one = MessageBuses.create("one", String.class, eic);
        one.accept("haha");

        ValueHolder<String> holder = new ValueHolder<>();
        try(Subscription subs = one.openSubscription(holder)) {
            Assert.assertNull(holder.getValue());
        }
        // we do multiple subscription for check that it is idempotent
        one.subscribe(holder);
        one.subscribe(holder);
        try(Subscription subs = one.openSubscription(holder)) {
            String s = "test";
            one.accept("trash");
            one.accept(s);
            Assert.assertEquals(s, holder.getValue());
            Assert.assertEquals(2, holder.getInvocations());
            one.accept(null);
            Assert.assertNull(holder.getValue());
        }
        one.accept("fail");
        Assert.assertNull(holder.getValue());
        one.unsubscribe(holder);
        Assert.assertEquals(3, holder.getInvocations());

        if(!errors.isEmpty()) {
            fail(errors.toString());
        }
    }
}