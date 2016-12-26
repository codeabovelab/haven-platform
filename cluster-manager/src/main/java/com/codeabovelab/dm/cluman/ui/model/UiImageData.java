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

import com.codeabovelab.dm.cluman.cluster.registry.ImageNameComparator;
import lombok.Data;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

/**
 */
@Data
public class UiImageData implements Comparable<UiImageData> {
    private final String id;
    private Date created;
    private long size;
    private final TreeSet<String> tags = new TreeSet<>(ImageNameComparator.getTagsComparator());
    private final Set<String> nodes = new TreeSet<>();

    public UiImageData(String id) {
        this.id = id;
    }

    /**
     * Deployed images have higher priority
     * @param o
     * @return
     */
    @Override
    public int compareTo(UiImageData o) {
        int compare = Integer.compare(nodes.size(), o.getNodes().size());
        if (compare == 0) {
            Date lc = created;
            Date rc = o.getCreated();
            if (lc == null || rc == null) {
                return lc != null ? 1 : (rc != null ? -1 : 0);
            }
            compare = lc.compareTo(rc);
        }
        return compare;
    }
}
