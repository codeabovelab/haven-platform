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

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * an object which generated at limit excess
 */
public class LimitExcess {

    public static class Builder {
        private String message;
        public String metric;

        public String getMessage() {
            return message;
        }

        public Builder message(String message) {
            setMessage(message);
            return this;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getMetric() {
            return metric;
        }

        public Builder metric(String metric) {
            setMetric(metric);
            return this;
        }

        public void setMetric(String metric) {
            this.metric = metric;
        }

        public LimitExcess build() {
            return new LimitExcess(this);
        }
    }

    private final String metric;
    private final String message;

    @JsonCreator
    public LimitExcess(Builder b) {
        this.message = b.message;
        this.metric = b.metric;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getMessage() {
        return message;
    }

    public String getMetric() {
        return metric;
    }

    @Override
    public String toString() {
        return "LimitExcess{" +
          "metric='" + metric + '\'' +
          ", message='" + message + '\'' +
          '}';
    }
}
