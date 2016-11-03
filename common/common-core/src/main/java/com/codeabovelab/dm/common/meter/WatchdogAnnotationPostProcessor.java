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

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * bean post processor for {@link com.codeabovelab.dm.common.meter.WatchdogRule }
 */
class WatchdogAnnotationPostProcessor implements BeanPostProcessor, Ordered {
    private static final Logger LOG = LoggerFactory.getLogger(WatchdogAnnotationPostProcessor.class);

    private final Watchdog watchdog;
    private final MetricRegistry metricRegistry;
    private final ExpressionLimitCheckerFactory exprCheckerFactory;

    WatchdogAnnotationPostProcessor(ExpressionLimitCheckerFactory exprCheckerFactory, Watchdog watchdog, MetricRegistry metricRegistry) {
        this.watchdog = watchdog;
        this.metricRegistry = metricRegistry;
        this.exprCheckerFactory = exprCheckerFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, String beanName) throws BeansException {
        final Class<?> targetClass = AopUtils.getTargetClass(bean);
        ReflectionUtils.doWithFields(targetClass, field -> {
            ReflectionUtils.makeAccessible(field);
            Object fieldValue = ReflectionUtils.getField(field, bean);
            if(fieldValue instanceof Metric) {
                WatchdogRule annotation = field.getAnnotation(WatchdogRule.class);
                addLimitChecker((Metric) fieldValue, annotation, getName(targetClass, field, fieldValue));
            }
        }, field -> field.isAnnotationPresent(WatchdogRule.class));
        ReflectionUtils.doWithMethods(targetClass, method -> {
            WatchdogRule annotation = method.getAnnotation(WatchdogRule.class);
            //method can be annotated with many different annotation
            processMetric(targetClass, method, annotation, MetricAnnotationAccessor.METERED, MetricAdapter.METER);
            processMetric(targetClass, method, annotation, MetricAnnotationAccessor.TIMED, MetricAdapter.TIMER);
            processMetric(targetClass, method, annotation, MetricAnnotationAccessor.COUNTED, MetricAdapter.COUNTER);
        }, field -> field.isAnnotationPresent(WatchdogRule.class));
        processClassRule(targetClass);
        return bean;
    }

    private void processClassRule(Class<?> targetClass) {
        WatchdogRule classWatchdogRule = targetClass.getAnnotation(WatchdogRule.class);
        if(classWatchdogRule == null) {
            return;
        }
        final String metricId = MetricNameUtil.getName(targetClass, classWatchdogRule.metric(), classWatchdogRule.absolute());
        Assert.hasText(metricId, "metric is empty");
        Metric metric = getByName(metricId);
        if(metric == null) {
            throw new NullPointerException("Can not find '" + metricId + "' in metrics registry.");
        }
        addLimitChecker(metric, classWatchdogRule, metricId);
    }

    private Metric getByName(String metricId) {
        return this.metricRegistry.getMetrics().get(metricId);
    }

    private <A extends Annotation, T extends Metric> void processMetric(Class<?> targetClass,
                                                                        Method method,
                                                                        WatchdogRule rule,
                                                                        MetricAnnotationAccessor<A> annotationAccessor,
                                                                        MetricAdapter<T> metricAdapter) {
        A meteredAnn = method.getAnnotation(annotationAccessor.getAnnotationType());
        if(meteredAnn != null) {
            String name = MetricNameUtil.chooseName(annotationAccessor.getName(meteredAnn), annotationAccessor.isAbsoluteName(meteredAnn), targetClass, method);
            T meter = metricAdapter.getOrCreate(metricRegistry, name);
            if(meter == null) {
                LOG.warn("Can not find meter for name '{}'", name);
            } else {
                addLimitChecker(meter, rule, name);
            }
        }
    }

    private <E extends AnnotatedElement & Member> String getName(Class<?> clazz, E elem, Object fieldValue) {
        com.codahale.metrics.annotation.Metric annotation = elem.getAnnotation(com.codahale.metrics.annotation.Metric.class);
        if(annotation == null) {
            throw new IllegalArgumentException(elem + " mut be an annotated with " + com.codahale.metrics.annotation.Metric.class);
        }
        return MetricNameUtil.chooseName(annotation.name(), annotation.absolute(), clazz, elem);
    }

    private void addLimitChecker(Metric metric, WatchdogRule rule, String name) {
        WatchdogTask task = watchdog.getTask(metric);
        if(task != null && !task.isEmpty()) {
            return;
        }
        ExpressionLimitCheckerSource checkerSource = new ExpressionLimitCheckerSource();
        checkerSource.setMetricName(name);
        checkerSource.setExpression(rule.expression());
        checkerSource.setPeriod(rule.period());
        checkerSource.setTimeUnit(rule.unit());
        ExpressionLimitChecker limitChecker = this.exprCheckerFactory.create(checkerSource);
        watchdog.registerTask(metric, name).addLimitChecker(limitChecker);
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
