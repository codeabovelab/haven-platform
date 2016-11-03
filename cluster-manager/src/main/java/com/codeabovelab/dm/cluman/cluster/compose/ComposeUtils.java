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

package com.codeabovelab.dm.cluman.cluster.compose;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class ComposeUtils {

    public static File applicationPath(String root, String cluster, String app, String fileName, boolean deleteIfExists) {
        fileName = fileName == null ? "docker-compose.yml" : fileName;
        Path path = Paths.get(root, "compose", "clusters", cluster, "apps", app, fileName);
        return createFile(path, deleteIfExists);
    }

    private static void createDirs(File file) {
        file.getParentFile().mkdirs();
    }

    public static File clusterPath(String root, String cluster, String fileName) {
        Path path = Paths.get(root, "compose", "clusters", cluster, fileName);
        return createFile(path, true);
    }

    private static File createFile(Path path, boolean deleteIfExists) {
        File file = path.toFile();
        if (deleteIfExists) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.error("Can't delete file", e);
            }
        }
        createDirs(file);
        return file;
    }
}
