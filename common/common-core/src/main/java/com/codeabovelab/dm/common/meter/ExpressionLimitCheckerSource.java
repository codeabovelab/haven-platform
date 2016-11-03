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

import java.util.concurrent.TimeUnit;

/**
 * Source for {@link com.codeabovelab.dm.common.meter.ExpressionLimitChecker }
*/
public final class ExpressionLimitCheckerSource {
    private String metricName;
    private long period;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private String expression;

    /**
     * Name of checked metric
     * @return
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * Name of checked metric
     * @param metricName
     */
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    /**
     * Name of checked metric
     * @param metricName
     * @return
     */
    public ExpressionLimitCheckerSource metricName(String metricName) {
        setMetricName(metricName);
        return this;
    }

    /**
     * period between checks.
     * @see com.codeabovelab.dm.common.meter.LimitChecker#getPeriod()
     * @return
     */
    public long getPeriod() {
        return period;
    }

    /**
     * period between checks.
     * @see com.codeabovelab.dm.common.meter.LimitChecker#getPeriod()
     * @param period
     */
    public void setPeriod(long period) {
        this.period = period;
    }

    /**
     * period between checks.
     * @see com.codeabovelab.dm.common.meter.LimitChecker#getPeriod()
     * @param period
     */
    public ExpressionLimitCheckerSource period(long period) {
        setPeriod(period);
        return this;
    }

    /**
     * Time unit for {@link #getPeriod()}. Default value is {@link java.util.concurrent.TimeUnit#SECONDS }
     * @return
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * Time unit for {@link #getPeriod()}. Default value is {@link java.util.concurrent.TimeUnit#SECONDS }
     * @param timeUnit
     */
    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    /**
     * Time unit for {@link #getPeriod()}. Default value is {@link java.util.concurrent.TimeUnit#SECONDS }
     * @param timeUnit
     */
    public ExpressionLimitCheckerSource timeUnit(TimeUnit timeUnit) {
        setTimeUnit(timeUnit);
        return this;
    }

    /**
     * Limit checking expression
     * @return
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Limit checking expression
     * @param expression
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * Limit checking expression
     * @param expression
     */
    public ExpressionLimitCheckerSource expression(String expression) {
        setExpression(expression);
        return this;
    }
}
