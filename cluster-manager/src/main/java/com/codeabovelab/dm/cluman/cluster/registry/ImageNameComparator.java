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

package com.codeabovelab.dm.cluman.cluster.registry;

import com.codeabovelab.dm.cluman.model.ImageName;
import com.codeabovelab.dm.common.utils.VersionComparator;

import java.util.Comparator;

/**
 */
public final class ImageNameComparator {

    /**
     * latest is default tag for docker, when you pull image w/o taf it looks for latest
     */
    private static final VersionComparator vc = VersionComparator.builder().addLatest("latest").build();
    public static final Comparator<String> STRING = (l, r) -> {
        ImageName inl = ImageName.parse(l);
        ImageName inr = ImageName.parse(r);
        return compareIN(inl, inr);
    };
    public static final Comparator<ImageName> OBJECT = ImageNameComparator::compareIN;

    private ImageNameComparator() {
    }

    /**
     * Use this method instead {@link VersionComparator#INSTANCE } in cases when you need compare image tags,
     * because we may configure it in future.
     * @return instance of {@link VersionComparator }
     */
    public static Comparator<String> getTagsComparator() {
        return vc;
    }

    private static int compareIN(ImageName inl, ImageName inr) {
        if(inl == null) {
            return inr == null ? 0 : -1;
        } else if(inr == null) {
            return 1;
        }
        int res = compareStrings(inl.getName(), inr.getName());
        if(res == 0) {
            res = vc.compare(inl.getTag(), inr.getTag());
        }
        if(res == 0) {
            res = compareStrings(inl.getRegistry(), inr.getRegistry());
        }
        return res;
    }

    private static int compareStrings(String l, String r) {
        if(l == null) {
            return r == null ? 0 : -1;
        } else if(r == null) {
            return 1;
        }
        return l.compareTo(r);
    }
}
