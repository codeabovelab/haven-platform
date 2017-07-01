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

package com.codeabovelab.dm.cluman.ui;

import com.codeabovelab.dm.cluman.cluster.application.ApplicationService;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.Node;
import com.codeabovelab.dm.cluman.ds.clusters.ClusterUtils;
import com.codeabovelab.dm.cluman.model.Application;
import com.codeabovelab.dm.cluman.model.ContainerBaseIface;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.cluman.ui.model.UIResult;
import com.codeabovelab.dm.cluman.ui.model.UiContainer;
import com.codeabovelab.dm.cluman.ui.model.UiContainerIface;
import com.codeabovelab.dm.cluman.ui.model.UiError;
import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import com.codeabovelab.dm.common.utils.Booleans;
import org.joda.time.LocalTime;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

/**
 * some utilities
 */
public final class UiUtils {

    static HttpStatus toStatus(ResultCode resultCode) {
        switch (resultCode) {
            case OK:
                return OK;
            case CONFLICT:
                return CONFLICT;
            case NOT_FOUND:
                return NOT_FOUND;
            case NOT_MODIFIED:
                return NOT_MODIFIED;
            default:
                return INTERNAL_SERVER_ERROR;
        }
    }

    static void convertCode(ServiceCallResult res, UiError error) {
        error.setCode(toStatus(res.getCode()).value());
    }

    static ResponseEntity<UIResult> createResponse(ServiceCallResult result) {
        ResultCode code = result.getCode();

        String message = code + " " + (result.getMessage() == null ? "" : result.getMessage());
        if (code == ResultCode.OK) {
            return okResponse(message);
        } else {
            return errResponse(code, message);
        }
    }

    static ResponseEntity<UIResult> errResponse(ResultCode code, String message) {
        UiError err = new UiError();
        err.setMessage(message);
        err.setCode(toStatus(code).value());
        return new ResponseEntity<>(err, toStatus(code));
    }

    static ResponseEntity<UIResult> okResponse(String message) {
        UIResult res = new UIResult();
        res.setMessage(message);
        res.setCode(OK.value());
        return new ResponseEntity<>(res, OK);
    }

    public static double convertToGB(long memory) {
        return Math.round(memory * 100 / (1024 * 1024 * 1024)) / 100d;
    }

    public static double convertToKb(long memory) {
        return Math.round(memory / 1024);
    }

    public static double convertToMb(long memory) {
        return Math.round(memory * 100 / (1024 * 1024)) / 100d;
    }

    public static double convertToMb(int memory) {
        return convertToMb((long)memory);
    }

    public static String convertToStringFromJiffies(Long jiffies) {
        LocalTime timeOfDay = LocalTime.fromMillisOfDay(jiffies / 1000_000L);
        return timeOfDay.toString("HH:mm:ss");
    }

    public static double convertToPercentFromJiffies(Long cpu, Long prevCpu, Long system, Long previousSystem, int cores) {

        double cpuPercent = 0d;
        // calculate the change for the cpu usage of the container in between readings
        double cpuDelta = cpu - prevCpu;
        // calculate the change for the entire system between readings
        double systemDelta = system - previousSystem;

        if (systemDelta > 0d && cpuDelta > 0d) {
            cpuPercent = (cpuDelta / systemDelta) * cores * 100;
        }
        return round(cpuPercent, 2);
    }

    static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public static String nodeTostring(Node node) {
        if (node == null) {
            // docker service instead of swarm does not return node
            return "";
        }
        return "Name: " + node.getName() + "; Address: " + node.getAddr();
    }

    public static void resolveContainerLock(UiContainerIface target, ContainerBaseIface src) {
        boolean ourContainer = ContainerUtils.isOurContainer(src);
        if (ourContainer || Booleans.valueOf(src.getLabels().get("lock"))) {
            target.setLock(true);
            if (ourContainer) {
                target.setLockCause("with this container we launched");
            } else {
                target.setLockCause("lock by appropriate label");
            }
        }
    }

    /**
     * Make app -> containerId map.
     * @param applicationService
     * @param cluster
     * @return
     */
    static Map<String, String> mapAppContainer(ApplicationService applicationService, NodesGroup cluster) {
        try {
            Map<String, String> containerApp = new HashMap<>();
            if(ClusterUtils.isDockerBased(cluster)) {
                addContainerMapping(applicationService, cluster.getName(), containerApp);
            } else {
                for(String child: cluster.getGroups()) {
                    addContainerMapping(applicationService, child, containerApp);
                }
            }
            return containerApp;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static void addContainerMapping(ApplicationService applicationService, String cluster, Map<String, String> map) {
        List<Application> applications = applicationService.getApplications(cluster);
        for (Application application : applications) {
            // container belongs to single application
            application.getContainers().forEach(container -> map.put(container, application.getName()));
        }
    }

    /**
     * Resolve public address of this app, if it not configured return null.
     * @param environment env
     * @return host:port or null
     */
    public static String getAppAddress(Environment environment) {
        String host = environment.getProperty("dm.server.host");
        if(host == null) {
            return null;
        }
        return host + ":" + environment.getProperty("dm.server.port");
    }

    static List<UiContainer> sortAndFilterContainers(List<UiContainer> list) {
        List<UiContainer> filteredContainers = filterEmptyContainers(list);
        Collections.sort(filteredContainers);
        return filteredContainers;
    }

    /**
     * workaround for preventing getting empty lines at UI
     * TODO: fix in https://github.com/codeabovelab/haven-platform/issues/56
     */
    static List<UiContainer> filterEmptyContainers(List<UiContainer> list) {
        return list.stream().filter(c -> StringUtils.hasText(c.getNode())).collect(Collectors.toList());
    }

}
