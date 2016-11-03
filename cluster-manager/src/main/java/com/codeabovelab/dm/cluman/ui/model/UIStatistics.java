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

import com.codeabovelab.dm.cluman.cluster.docker.model.Statistics;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.codeabovelab.dm.cluman.ui.UiUtils.convertToMb;
import static com.codeabovelab.dm.cluman.ui.UiUtils.convertToPercentFromJiffies;
import static com.codeabovelab.dm.cluman.ui.UiUtils.convertToStringFromJiffies;


@Data
public class UIStatistics {

    private LocalDateTime time = LocalDateTime.now();

    private Map<String, Object> networks;
    private Map<String, Object> blkioStats;

    //memory_stats in MB
    //usage
    private Double memoryMBUsage; //usage
    private Double memoryMBMaxUsage; //max_usage
    private Double memoryMBLimit;
    private Double memoryPercentage;

    //cpu_stats
    //cpu_usage
    private String cpuTotalUsage; //total_usage
    private String cpuKernel;     //usage_in_kernelmode
    private String cpuUser;       //usage_in_usermode

    private String cpuSystem;     //system_cpu_usage

    private Double cpuTotalPercents; //total_usage

    @SuppressWarnings("unchecked")
    public static UIStatistics from(Statistics s) {
        UIStatistics uis = new UIStatistics();
        Map<String, Object> cpuStats = s.getCpuStats();
        Map<String, Object> cpuUsage = (Map<String, Object>) cpuStats.get("cpu_usage");
        Long totalUsage = toLong(cpuUsage.get("total_usage"));
        uis.setCpuTotalUsage(convertToStringFromJiffies(totalUsage));

        uis.setCpuKernel(convertToStringFromJiffies(toLong(cpuUsage.get("usage_in_kernelmode"))));
        uis.setCpuUser(convertToStringFromJiffies(toLong(cpuUsage.get("usage_in_usermode"))));
        Long systemCpuUsage = toLong(cpuStats.get("system_cpu_usage"));
        uis.setCpuSystem(convertToStringFromJiffies(systemCpuUsage / 1000_000L));

        Map<String, Object> precpuStats = s.getPrecpuStats();
        Map<String, Object> preCpuUsage = (Map<String, Object>) precpuStats.get("cpu_usage");
        Long prevTotalUsage = toLong(preCpuUsage.get("total_usage"));

        Long prevSystemUsage = toLong(precpuStats.get("system_cpu_usage"));

        Collection percpu_usage = (Collection) cpuUsage.get("percpu_usage");

        double percents = convertToPercentFromJiffies(totalUsage, prevTotalUsage, systemCpuUsage, prevSystemUsage, percpu_usage == null ? 0 : percpu_usage.size());
        uis.setCpuTotalPercents(percents);

        Map<String, Object> memoryStats = s.getMemoryStats();
        Long maxUsage = toLong(memoryStats.get("max_usage"));
        Long usage = toLong(memoryStats.get("usage"));
        Long limit = toLong(memoryStats.get("limit"));
        Double percentage = Math.round(usage / limit.doubleValue() * 100_00) / 100d;

        uis.setMemoryMBMaxUsage(convertToMb(maxUsage));
        uis.setMemoryMBUsage(convertToMb(usage));
        uis.setMemoryMBLimit(convertToMb(limit));
        uis.setMemoryPercentage(percentage);

        uis.setNetworks(s.getNetworks());
        uis.setBlkioStats(s.getBlkioStats());
        return uis;
    }


    private static Long toLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return new Long((Integer) value);
        } else {
            return 0L;
        }
    }
}
