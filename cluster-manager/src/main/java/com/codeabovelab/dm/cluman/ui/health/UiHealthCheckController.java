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

package com.codeabovelab.dm.cluman.ui.health;

import com.codeabovelab.dm.common.healthcheck.ServiceHealthCheckResult;
import com.codeabovelab.dm.common.healthcheck.ServiceHealthCheckResultImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller which invoke health check
 */
@RestController
@RequestMapping("/ui/api/")
public class UiHealthCheckController {

    private final HealthCheckService service;

    @Autowired
    public UiHealthCheckController(HealthCheckService service) {
        this.service = service;
    }

    @RequestMapping(value = "/clusters/{cluster}/containers/{id}/check", method = RequestMethod.GET)
    public ServiceHealthCheckResult checkContainer(@PathVariable("cluster") String cluster, @PathVariable("id") String id) {
        //TODO use timeout from parameters
        ServiceHealthCheckResult result = service.checkContainer(cluster, id, 10_000L);
        if(result == null) {
            ServiceHealthCheckResultImpl.Builder b = new ServiceHealthCheckResultImpl.Builder();
            b.setHealthy(false);
            result = b;
        }
        return result;
    }

    @RequestMapping(value = "/healthcheck/check", method = RequestMethod.GET)
    public List<ServiceHealthCheckResult> check() {
        List<ServiceHealthCheckResult> list = new ArrayList<>();
        service.checkAll(list::add);
        return list;
    }


}
