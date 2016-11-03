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

package com.codeabovelab.dm.cluman.batch;

import com.codeabovelab.dm.cluman.job.JobBean;
import com.codeabovelab.dm.cluman.job.JobContext;
import com.codeabovelab.dm.cluman.job.JobParam;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

/**
 * Simple bean with "ping" job, which do nothing. It used for api sampling and testing.
 */
@JobBean("job.sample")
public class SampleJobBean implements Runnable {

    @Autowired
    private JobContext context;

    @JobParam
    private String inParam;

    @JobParam(out = true, in = false)
    private String outParam;

    @Override
    public void run() {
        context.fire("Job is worked.");
        this.outParam = LocalDateTime.now() + " [" + inParam + "]";
    }
}
