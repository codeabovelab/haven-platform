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

package com.codeabovelab.dm.common.fc;

import lombok.Data;
import org.springframework.util.Assert;

import java.io.File;

/**
 */
@Data
public class FbStorage {

    @Data
    public static class Builder {
        /**
         * Maximum file size. Default 1GiB
         */
        private long maxFileSize = 1024L * 1024L * 1024L;
        /**
         * Max count of files in dir. Default 1024.
         */
        private int maxFiles = 1024;
        /**
         * path to storage
         */
        private String path;

        public Builder maxFileSize(long maxFileSize) {
            setMaxFileSize(maxFileSize);
            return this;
        }

        /**
         * Max count of files in dir.
         * @param maxFiles
         * @return
         */
        public Builder maxFiles(int maxFiles) {
            setMaxFiles(maxFiles);
            return this;
        }

        public Builder path(String path) {
            setPath(path);
            return this;
        }

        public FbStorage build() {
            return new FbStorage(this);
        }
    }

    private final long maxFileSize;
    private final int maxFiles;
    private final File storageDir;


    private FbStorage(Builder b) {
        this.maxFileSize = b.maxFileSize;
        Assert.isTrue(this.maxFileSize > 0, "maxFileSize is less than one");
        this.maxFiles = b.maxFiles;
        Assert.isTrue(this.maxFiles > 0, "maxFiles is less than one");
        this.storageDir = new File(b.path);
        makeAndCheckDir(this.storageDir);
    }

    public static Builder builder() {
        return new Builder();
    }

    static void makeAndCheckDir(File dir) {
        dir.mkdirs();
        String absolutePath = dir.getAbsolutePath();
        Assert.isTrue(dir.isDirectory(), absolutePath + " is not a directory.");
        Assert.isTrue(dir.canRead() && dir.canWrite(),
          "Cannot read/write to " + absolutePath);
    }

}
