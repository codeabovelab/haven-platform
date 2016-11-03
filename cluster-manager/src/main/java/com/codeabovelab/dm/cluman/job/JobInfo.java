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

package com.codeabovelab.dm.cluman.job;

import lombok.Data;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

/**
 */
@Data
public class JobInfo implements JobEventCriteria {

    @Data
    public static class Builder {

        private String id;

        private String title;

        private String type;

        private JobStatus status = JobStatus.CREATED;

        private LocalDateTime endTime = LocalDateTime.MAX;

        private LocalDateTime startTime = LocalDateTime.MAX;

        private LocalDateTime createTime;

        public Builder id(String id) {
            setId(id);
            return this;
        }

        public Builder type(String type) {
            setType(type);
            return this;
        }

        public Builder title(String title) {
            setTitle(title);
            return this;
        }

        public Builder status(JobStatus status) {
            setStatus(status);
            return this;
        }

        public Builder endTime(LocalDateTime endTime) {
            setEndTime(endTime);
            return this;
        }

        public Builder startTime(LocalDateTime startTime) {
            setStartTime(startTime);
            return this;
        }

        public Builder createTime(LocalDateTime createTime) {
            setCreateTime(createTime);
            return this;
        }

        public JobInfo build() {
            return new JobInfo(this);
        }

        public Builder from(JobInfo f) {
            if(f != null) {
                setId(f.getId());
                setTitle(f.getTitle());
                setType(f.getType());
                setStatus(f.getStatus());
                setCreateTime(f.getCreateTime());
                setStartTime(f.getStartTime());
                setEndTime(f.getEndTime());
            }
            return this;
        }
    }

    /**
     * Identifier.
     * @return
     */
    private final String id;

    /**
     * Human readable short string.
     * @return
     */
    private final String title;

    private final String type;

    private final JobStatus status;

    /**
     * Time when job instance has been ended.
     * @return time or {@link LocalDateTime#MAX } if it is still continues execution.
     */
    private final LocalDateTime endTime;

    /**
     * Time when job instance has been started.
     * @return time or {@link LocalDateTime#MAX } if it not yet already started.
     */
    private final LocalDateTime startTime;

    /**
     * Time when job instance has been created.
     * @return time
     */
    private final LocalDateTime createTime;

    public JobInfo(Builder b) {
        this.id = b.id;
        Assert.notNull(this.id, "id is null");
        this.title = b.title;
        this.type = b.type;
        Assert.notNull(this.type, "type is null");
        this.status = b.status;
        Assert.notNull(this.status, "status is null");
        this.endTime = b.endTime;
        this.startTime = b.startTime;
        this.createTime = b.createTime;
        Assert.notNull(this.createTime, "createTime is null");
    }

    public static Builder builder() {
        return new Builder();
    }
}
