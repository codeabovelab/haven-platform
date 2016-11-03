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

import com.codeabovelab.dm.cluman.job.JobInfo;
import com.codeabovelab.dm.cluman.job.JobInstance;
import com.codeabovelab.dm.cluman.job.JobParameters;
import com.codeabovelab.dm.cluman.job.JobStatus;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Model of ui representation of {@link com.codeabovelab.dm.cluman.job.JobInstance }
 */
@Data
public class UiJob {
    private String id;
    private String title;
    private JobStatus status;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean running;
    private JobParameters parameters;

    public static UiJob toUi(JobInstance ji) {
        UiJob uj = new UiJob();
        JobInfo jh = ji.getInfo();
        uj.setId(jh.getId());
        String title = jh.getTitle();
        if(!StringUtils.hasText(title)) {
            title = jh.getType();
        }
        uj.setTitle(title);
        uj.setCreateTime(jh.getCreateTime());
        uj.setStartTime(jh.getStartTime());
        uj.setEndTime(jh.getEndTime());
        JobStatus status = jh.getStatus();
        uj.setStatus(status);
        uj.setRunning(status == JobStatus.STARTED);
        return uj;
    }

}
