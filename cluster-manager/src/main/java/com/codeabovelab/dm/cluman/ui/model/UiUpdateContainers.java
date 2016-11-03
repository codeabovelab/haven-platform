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

package com.codeabovelab.dm.cluman.ui.model;


import javax.validation.constraints.NotNull;


/**
 * POST Data for update containers command
 */
public class UiUpdateContainers {

    @NotNull
    private String service;
    @NotNull
    private String version;
    @NotNull
    private String strategy;
    private Float percentage;
    private boolean healthCheckEnabled;
    private boolean rollbackEnabled;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Float getPercentage() {
        return percentage;
    }

    public void setPercentage(Float percentage) {
        this.percentage = percentage;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public boolean isHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    public void setHealthCheckEnabled(boolean healthCheckEnabled) {
        this.healthCheckEnabled = healthCheckEnabled;
    }

    public boolean isRollbackEnabled() {
        return rollbackEnabled;
    }

    public void setRollbackEnabled(boolean rollbackEnabled) {
        this.rollbackEnabled = rollbackEnabled;
    }

    @Override
    public String toString() {
        return "UiUpdateContainers{" +
          "service='" + service + '\'' +
          ", version='" + version + '\'' +
          ", strategy=" + strategy +
          ", percentage=" + percentage +
          ", healthCheckEnabled=" + healthCheckEnabled +
          '}';
    }
}
