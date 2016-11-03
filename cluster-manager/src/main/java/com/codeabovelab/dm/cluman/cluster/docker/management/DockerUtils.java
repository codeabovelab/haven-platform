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

package com.codeabovelab.dm.cluman.cluster.docker.management;

import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerConfig;
import com.codeabovelab.dm.cluman.cluster.docker.model.UpdateContainerResponse;
import com.codeabovelab.dm.cluman.model.DockerServiceInfo;
import com.codeabovelab.dm.cluman.model.NodeInfo;
import com.google.common.base.Strings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Some utils
 */
public final class DockerUtils {

    public static final String RESTART = "restart";
    public static final String SCALABLE = "scalable";

    static ServiceCallResult getServiceCallResult(ResponseEntity<?> res) {
        return getServiceCallResult(res, new ServiceCallResult());
    }

    static <T extends ServiceCallResult> T getServiceCallResult(ResponseEntity<?> response, T callResult) {
        Assert.notNull(response, "ResponseEntity is null");
        Assert.notNull(callResult, "callResult is null");
        Object body = response.getBody();
        if (body != null) {
            callResult.setMessage(body.toString());
        }
        setCode(response, callResult);
        return callResult;
    }

    /**
     * Set code from service response entity
     * @param response
     * @param result
     */
    public static <T extends ServiceCallResult> void setCode(ResponseEntity<?> response, T result) {
        Assert.notNull(response, "ResponseEntity is null");
        setCode(response.getStatusCode(), result);
    }

    /**
     * Set code from httpCode
     * @param httpCode
     * @param result
     */
    public static <T extends ServiceCallResult> void setCode(HttpStatus httpCode, T result) {
        Assert.notNull(httpCode, "httpCode is null");
        Assert.notNull(result, "result is null");
        ResultCode code;
        switch (httpCode.value()) {
            case 200:
            case 201:
            case 202:
            case 204:
                code = ResultCode.OK;
                break;
            case 304:
                code = ResultCode.NOT_MODIFIED;
                break;
            case 404:
                code = ResultCode.NOT_FOUND;
                break;
            case 409:
                code = ResultCode.CONFLICT;
                break;
            default:
                //due doc 500 - is error, but any other than above codes is undefined we interpret this cases as error also.
                code = ResultCode.ERROR;
        }
        result.setCode(code);

    }

    /**
     * Return full host name
     *
     * @param config
     * @return
     */
    public static String getFullHostName(ContainerConfig config) {
        String domainname = config.getDomainName();
        String hostname = config.getHostName();
        if (Strings.isNullOrEmpty(hostname)) {
            return null;
        }
        if (Strings.isNullOrEmpty(domainname)) {
            return hostname;
        }
        return hostname + "." + domainname;
    }

    public static List<String> listNodes(DockerServiceInfo info) {

        List<String> list = new ArrayList<>();
        List<NodeInfo> nodeList = info.getNodeList();
        list.addAll(nodeList.stream().map(NodeInfo::getName).collect(Collectors.toList()));
        return list;
    }

}
