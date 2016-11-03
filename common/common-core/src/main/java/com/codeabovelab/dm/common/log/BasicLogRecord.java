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

import lombok.Data;

import java.util.Date;

/**
 * basic record of log message which can be transferred into logstash
 *
 */
@Data
public class BasicLogRecord implements ApplicationInfo {
    private String host;
    /**
     * @see com.codeabovelab.dm.common.utils.AppInfo#getApplicationName()
     */
    private String application;
    /**
     * @see com.codeabovelab.dm.common.utils.AppInfo#getApplicationVersion()
     */
    private String applicationVersion;
    /**
     * Type of record (like 'log' or 'metric'), it used
     * in analyzing software (like kibana or zabbix) for grouping messages.
     */
    private String type;
    private String message;
    /**
     * Additional description of log entry
     */
    private String description;
    /**
     * Timestamp of event
     */
    private Date date;

}
