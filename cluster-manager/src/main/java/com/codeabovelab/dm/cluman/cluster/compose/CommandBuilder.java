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

public final class CommandBuilder {

    public static String launchTask(String fileName) {
        return prepareCommand(fileName) + " up -d";
    }

    public static String pullImages(String fileName) {
        return prepareCommand(fileName) + " pull";
    }

    public static String stopTask(String fileName) {
        return prepareCommand(fileName) + " stop";
    }

    public static String downTask(String fileName) {
        return prepareCommand(fileName) + " down";
    }

    public static String getContainerIds(String fileName) {
        return prepareCommand(fileName) + " ps -q";
    }

    /**
     * prepare command line
     * @param fileName
     * @return
     */
    private static String prepareCommand(String fileName) {
        return "docker-compose -f " + fileName;
    }

}
