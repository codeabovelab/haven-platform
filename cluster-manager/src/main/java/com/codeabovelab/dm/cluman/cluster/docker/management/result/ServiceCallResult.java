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

package com.codeabovelab.dm.cluman.cluster.docker.management.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * Result of call service
 */
@Data
public class ServiceCallResult {
    private String message;
    private ResultCode code;
    /**
     * We must not publish this field, because it part of internal api. Used for fine handling of docker errors.
     */
    @JsonIgnore
    private HttpStatus status;

    public ServiceCallResult code(ResultCode code) {
        setCode(code);
        return this;
    }

    public ServiceCallResult message(String message) {
        setMessage(message);
        return this;
    }

    public static ServiceCallResult unsupported() {
        ServiceCallResult scr = new ServiceCallResult();
        scr.setCode(ResultCode.ERROR);
        scr.setMessage("Not supported operation.");
        return scr;
    }

    public static ServiceCallResult unmodified() {
        ServiceCallResult scr = new ServiceCallResult();
        scr.setCode(ResultCode.NOT_MODIFIED);
        scr.setMessage("Not modified.");
        return scr;
    }
}
