/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.common.meter;

import com.codahale.metrics.*;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.codeabovelab.dm.common.utils.AmqpUtils;
import com.codeabovelab.dm.common.utils.AppInfo;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import java.util.concurrent.TimeUnit;

/**
 * Spring configuration meter
 */
@Configuration
@EnableMetrics
@ComponentScan(basePackageClasses = MeterConfiguration.class)
public class MeterConfiguration extends MetricsConfigurerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MeterConfiguration.class);

    @Value("${meter.report.period:60}")
    private Integer reportPeriod;

    @Value("${meter.report.exchange:dm.metrics}")
    private String exchange;

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public WatchdogAnnotationPostProcessor watchdogAnnotationPostProcessor(MetricRegistry metricRegistry, Watchdog watchdog) {
        return new WatchdogAnnotationPostProcessor(factory(), watchdog, metricRegistry);
    }

    @Autowired
    private ObjectFactory<ConnectionFactory> connectionFactoryProvider;

    @Autowired
    private Environment environment;

    @Autowired
    private Watchdog watchdog;

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public ExpressionLimitCheckerFactory factory() {
        return new ExpressionLimitCheckerFactory(environment);
    }

    private final MetricRegistryListener metricsListener = new MetricRegistryListener.Base() {
        @Override
        public void onCounterAdded(String name, Counter counter) {
            configureCheckers(counter, name);
        }

        @Override
        public void onHistogramAdded(String name, Histogram histogram) {
            configureCheckers(histogram, name);
        }

        @Override
        public void onMeterAdded(String name, Meter meter) {
            configureCheckers(meter, name);
        }

        @Override
        public void onTimerAdded(String name, Timer timer) {
            configureCheckers(timer, name);
        }

        private void configureCheckers(Metric metric, String name) {
            ExpressionLimitChecker limitChecker = factory().create(new ExpressionLimitCheckerSource().metricName(name));
            if(limitChecker != null) {
                watchdog.registerTask(metric, name).addLimitChecker(limitChecker);
            }
        }
    };

    @Override
    public void configureReporters(MetricRegistry metricRegistry) {
        metricRegistry.addListener(metricsListener);

        metricRegistry.register("jvm.gc", new GarbageCollectorMetricSet());
        metricRegistry.register("jvm.memory", new MemoryUsageGaugeSet());
        metricRegistry.register("jvm.thread-states", new ThreadStatesGaugeSet());
        metricRegistry.register("jvm.fd.usage", new FileDescriptorRatioGauge());


        if(environment.acceptsProfiles(AmqpUtils.PROFILE)) {
            final String applicationId = AppInfo.getApplicationName();
            AmqpUtils.ExchangeFactory exchangeFactory = new AmqpUtils.ExchangeFactory();
            exchangeFactory.setExchangeName(exchange);
            AmqpReporter amqpReporter = AmqpReporter.forRegistry(metricRegistry)
              .connectionFactoryProvider(connectionFactoryProvider)
              .exchangeName(exchange)
              .routingKey(applicationId)
              .exchangeFactory(exchangeFactory)
              .build();
            amqpReporter.start(reportPeriod, TimeUnit.SECONDS);
            LOG.info("AmqpReporter enabled: applicationId: {} reportPeriod: {} seconds, exchange: {}",
                    applicationId, reportPeriod, exchange);
        } else {
            //sample configuration for metrics reported
            // https://dropwizard.github.io/metrics/3.1.0/manual/core/#man-core-reporters-console
            final Slf4jReporter reporter = Slf4jReporter.forRegistry(metricRegistry)
                    .outputTo(LoggerFactory.getLogger("com.codeabovelab.dm.metrics"))
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .build();
            reporter.start(reportPeriod, TimeUnit.SECONDS);
        }
    }

}
