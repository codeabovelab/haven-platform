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

/**
 * Entity for serializing to logstash JSON
 *
 */
class LogstashEntity extends UUIDLogRecord {
    public static final String TYPE = "log";
    private String user;
    private String loggerName;
    private String level;
    private String threadName;
    private String throwable;

    public LogstashEntity() {
        setType(TYPE);
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getThrowable() {
        return throwable;
    }

    public void setThrowable(String throwable) {
        this.throwable = throwable;
    }

    @Override
    public String toString() {
        return "LogstashEntity{" +
                "user='" + user + '\'' +
                ", loggerName='" + loggerName + '\'' +
                ", level='" + level + '\'' +
                ", threadName='" + threadName + '\'' +
                ", throwable='" + throwable + '\'' +
                "} " + super.toString();
    }
}
