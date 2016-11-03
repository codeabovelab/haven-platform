package com.codeabovelab.dm.common.meter;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.annotation.Metric;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MeterTest.TestConfiguration.class)
public class MeterTest {
    private static final String TEST_METHOD_METER = "testMethodMeter";
    private static final String TEST_METHOD_TIMER = "testMethodTimer";

    @EnableAutoConfiguration
    @Import(MeterConfiguration.class)
    @ComponentScan(basePackageClasses = {TestBean.class})
    public static class TestConfiguration {
        @Autowired
        void awareMetricRegistry(MetricRegistry metricRegistry) {
            ConsoleReporter
              .forRegistry(metricRegistry)
              .build()
              .start(10, TimeUnit.SECONDS);
        }
    }

    @Component
    public static class TestBean {

        @Metered(name = TEST_METHOD_METER, absolute = true)
        // throughput in count per sec
        @WatchdogRule(expression = "meter.meanRate < 10 and meter.count > 10", unit = TimeUnit.SECONDS)
        public void method() throws InterruptedException {
            System.out.println("invoke 'method'");
            Thread.sleep(100);
        }

        @Metric(absolute = true, name = TEST_METHOD_TIMER)
        // max timeout greater than .5 min (time specified in nanos)
        @WatchdogRule(expression = "timer.snapshot.max > 5*10e6", unit = TimeUnit.SECONDS)
        Timer timer;

        @Timed(name = TEST_METHOD_TIMER, absolute = true)
        public void slowmethod() throws InterruptedException {
            System.out.println("invoke 'slowmethod'");
            Thread.sleep(1000);
        }
    }

    @Autowired
    Watchdog watchdog;

    @Autowired
    MetricRegistry registry;

    @Autowired
    TestBean testBean;

    @Test
    public void test() throws Exception {
        final int count = 20;
        for(int i = 0; i < count; ++i) {
            testBean.method();
        }
        Meter meter = registry.getMeters().get(TEST_METHOD_METER);
        assertEquals(count, meter.getCount());
        System.out.println(meter.getMeanRate());
        List<LimitExcess> excesses = watchdog.getTask(meter).getState().getExcesses();
        assertFalse(excesses.isEmpty());
    }

    @Test
    public void testExcess() throws Exception {
        final int count = 3;
        for(int i = 0; i < count; ++i) {
            testBean.slowmethod();
        }
        Timer meter = registry.getTimers().get(TEST_METHOD_TIMER);
        assertEquals(count, meter.getCount());
        System.out.println(meter.getSnapshot().getMax());
        List<LimitExcess> excesses = watchdog.getTask(meter).getState().getExcesses();
        assertFalse(excesses.isEmpty());
    }
}
