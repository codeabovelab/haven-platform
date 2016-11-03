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

package com.codeabovelab.dm.common.healthcheck;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.codeabovelab.dm.common.log.ApplicationInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base implementation of service health check result
 */
public class ServiceHealthCheckResultImpl implements ServiceHealthCheckResult {

    public static class Builder implements ServiceHealthCheckResult {
        private ApplicationInfo applicationInfo;
        private final List<HealthCheckResultData> results = new ArrayList<>();
        private boolean healthy;

        @Override
        public ApplicationInfo getApplicationInfo() {
            return applicationInfo;
        }

        public Builder applicationInfo(ApplicationInfo applicationInfo) {
            setApplicationInfo(applicationInfo);
            return this;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public void setApplicationInfo(ApplicationInfo applicationInfo) {
            this.applicationInfo = applicationInfo;
        }

        @Override
        public List<HealthCheckResultData> getResults() {
            return results;
        }

        public Builder results(List<HealthCheckResultData> results) {
            setResults(results);
            return this;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public void setResults(List<HealthCheckResultData> results) {
            this.results.clear();
            if(results != null) {
                this.results.addAll(results);
            }
        }

        @Override
        public boolean isHealthy() {
            return healthy;
        }

        public Builder healthy(boolean healthy) {
            setHealthy(healthy);
            return this;
        }

        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }

        /**
         * Copy all fields from specified object.
         * @param result
         * @return
         */
        public Builder from(ServiceHealthCheckResult result) {
            setApplicationInfo(result.getApplicationInfo());
            setHealthy(result.isHealthy());
            setResults(result.getResults());
            return this;
        }

        public ServiceHealthCheckResultImpl build() {
            return new ServiceHealthCheckResultImpl(this);
        }
    }

    private final ApplicationInfo applicationInfo;
    private final List<HealthCheckResultData> results;
    private final boolean healthy;

    @JsonCreator
    public ServiceHealthCheckResultImpl(Builder b) {
        this.applicationInfo = b.applicationInfo;
        this.healthy = b.healthy;
        this.results = Collections.unmodifiableList(new ArrayList<>(b.results));
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public List<HealthCheckResultData> getResults() {
        return results;
    }

    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceHealthCheckResultImpl)) {
            return false;
        }

        ServiceHealthCheckResultImpl that = (ServiceHealthCheckResultImpl) o;

        if (healthy != that.healthy) {
            return false;
        }
        if (applicationInfo != null ? !applicationInfo.equals(that.applicationInfo) : that.applicationInfo != null) {
            return false;
        }
        if (results != null ? !results.equals(that.results) : that.results != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = applicationInfo != null ? applicationInfo.hashCode() : 0;
        result = 31 * result + (results != null ? results.hashCode() : 0);
        result = 31 * result + (healthy ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ServiceHealthCheckResultImpl{" +
          "applicationInfo=" + applicationInfo +
          ", results=" + results +
          ", healthy=" + healthy +
          '}';
    }
}
