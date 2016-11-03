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

import com.codeabovelab.dm.cluman.job.JobComponent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Data for rollback
 */
@JobComponent
public class RollbackData {

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


    private final List<Record> records = new CopyOnWriteArrayList<>();

    /**
     * Record of attempt to modify container.
     * @param container
     */
    public void record(ProcessedContainer container, Action action) {
        records.add(new Record(container, action));
    }

    List<Record> getRecords() {
        return records;
    }
}
