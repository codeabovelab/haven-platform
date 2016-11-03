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

package com.codeabovelab.dm.common.log;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.codeabovelab.dm.common.utils.AppInfo;
import com.codeabovelab.dm.common.utils.OSUtils;

/**
 */
public class ApplicationInfoImpl implements ApplicationInfo {
    public static class Builder implements ApplicationInfo {
        private String host;
        private String application;
        private String applicationVersion;

        @Override
        public String getHost() {
            return host;
        }

        public Builder host(String host) {
            setHost(host);
            return this;
        }

        public void setHost(String host) {
            this.host = host;
        }

        @Override
        public String getApplication() {
            return application;
        }

        public Builder application(String application) {
            setApplication(application);
            return this;
        }

        public void setApplication(String application) {
            this.application = application;
        }

        @Override
        public String getApplicationVersion() {
            return applicationVersion;
        }

        public Builder applicationVersion(String applicationVersion) {
            setApplicationVersion(applicationVersion);
            return this;
        }

        public void setApplicationVersion(String applicationVersion) {
            this.applicationVersion = applicationVersion;
        }

        public ApplicationInfoImpl build() {
            return new ApplicationInfoImpl(this);
        }
    }

    private final String host;
    private final String application;
    private final String applicationVersion;

    @JsonCreator
    public ApplicationInfoImpl(Builder builder) {
        this.application = builder.application;
        this.applicationVersion = builder.applicationVersion;
        this.host = builder.host;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getApplication() {
        return application;
    }

    @Override
    public String getApplicationVersion() {
        return applicationVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationInfoImpl)) {
            return false;
        }

        ApplicationInfoImpl that = (ApplicationInfoImpl) o;

        if (application != null ? !application.equals(that.application) : that.application != null) {
            return false;
        }
        if (applicationVersion != null ? !applicationVersion.equals(that.applicationVersion) : that.applicationVersion != null) {
            return false;
        }
        if (host != null ? !host.equals(that.host) : that.host != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + (application != null ? application.hashCode() : 0);
        result = 31 * result + (applicationVersion != null ? applicationVersion.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ApplicationInfoImpl{" +
          "host='" + host + '\'' +
          ", application='" + application + '\'' +
          ", applicationVersion='" + applicationVersion + '\'' +
          '}';
    }

    /**
     * create default ApplicationInfo
     * @return
     */
    public static ApplicationInfoImpl createDefault() {
        return ApplicationInfoImpl.builder()
          .host(OSUtils.getHostName())
          .application(AppInfo.getApplicationName())
          .applicationVersion(AppInfo.getApplicationVersion())
          .build();
    }
}
