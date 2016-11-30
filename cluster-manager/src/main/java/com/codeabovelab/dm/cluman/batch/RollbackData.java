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

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.job.*;
import com.codeabovelab.dm.cluman.model.DiscoveryStorage;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Data for rollback. <p/>
 * We MUST NOT use here references to any beans, because it object must be serializable to JSON.
 */
@JobIterationComponent
public class RollbackData {

    @Data
    @AllArgsConstructor(onConstructor = @_(@JsonCreator))
    private static class RollbackHandleImpl implements RollbackHandle {
        private final RollbackData data;

        @Override
        public void rollback(RollbackContext rc) {
            // note that it doing in another jobContext
            // and we must again transfer rollback data to new context
            rc.setBean(data);
            DockerService ds = rc.getBean(DiscoveryStorage.class).getService(data.cluster);
            Assert.notNull(ds, "Can not find cluster: " + data.cluster);
            rc.setBean(ds, DockerService.class);
            RollbackTasklet tasklet = rc.getBean(RollbackTasklet.class);
            tasklet.rollback();
        }
    }

    @Autowired
    private void init(JobContext jobContext) {
        if(jobContext.getAttributes().values().contains(this)) {
            // we in rollback job
            return;
        }
        Assert.isNull(jobContext.getRollback(), "Context already has rollback");
        jobContext.setRollback(new RollbackHandleImpl(this));
    }

    public enum Action {
        CREATE, STOP, DELETE
    }

    public final static class Record {
        final ProcessedContainer container;
        final Action action;

        public Record(ProcessedContainer container, Action action) {
            this.container = container;
            this.action = action;
        }

        @Override
        public String toString() {
            return "Record{" +
              "action=" + action +
              ", container=" + container +
              '}';
        }
    }

    @JobParam(value = BatchUtils.JP_CLUSTER, required = true)
    private String cluster;
    private final List<Record> records = new CopyOnWriteArrayList<>();

    /**
     * Record of attempt to modify container.
     * @param container
     */
    public void record(ProcessedContainer container, Action action) {
        if(action == Action.DELETE) {
            Assert.notNull(container.getSrc(), "Container source is null: we can not rollback it");
        }
        records.add(new Record(container, action));
    }

    List<Record> getRecords() {
        return records;
    }
}
