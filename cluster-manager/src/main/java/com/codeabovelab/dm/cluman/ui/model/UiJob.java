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
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Model of ui representation of {@link com.codeabovelab.dm.cluman.job.JobInstance }
 */
@Value
@Builder
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public class UiJob {
    private final String id;
    private final String title;
    private final JobStatus status;
    private final LocalDateTime createTime;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final boolean running;
    private final boolean canRollback;
    private final JobParameters parameters;

    public static UiJobBuilder toUiBuilder(JobInstance ji) {
        JobInfo jh = ji.getInfo();
        String title = jh.getTitle();
        if (!StringUtils.hasText(title)) {
            title = jh.getType();
        }
        JobStatus status = jh.getStatus();
        return UiJob.builder()
                .id(jh.getId())
                .title(title)
                .createTime(jh.getCreateTime())
                .startTime(jh.getStartTime())
                .endTime(jh.getEndTime())
                .status(status)
                .canRollback(ji.getJobContext().getRollback() != null)
                .running(status == JobStatus.STARTED);
    }

    public static UiJob toUi(JobInstance ji) {
        return toUiBuilder(ji).build();
    }

}
