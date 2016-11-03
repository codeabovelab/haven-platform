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

package com.codeabovelab.dm.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 */
@Slf4j
public class ProcessUtils {

    private static final Logger LOGGER =  LoggerFactory.getLogger(ProcessUtils.class);

    /**
     * Get system ProcessID of specified process. <p/>
     * Currently it method work only on unix systems.
     * @param process
     * @return PID or -1 on fail
     */
    public static int getPid(Process process) {
        Class<? extends Process> clazz = process.getClass();
        try {
            Field field = clazz.getDeclaredField("pid");
            if(field.isAccessible()) {
                field.setAccessible(true);
            }
            return (int)field.get(process);
        } catch (IllegalAccessException|NoSuchFieldException e) {
            return -1;
        }
    }

    public static int executeCommand(String command, ExecuteWatchdog watchdog, Map<String, String> env) {
        return executeCommand(command, watchdog, null, null, null, env);
    }

    public static ExecuteWatchdog createTimeoutWatchdog(TimeUnit timeunit, int timeout) {
        ExecuteWatchdog timeoutWatchdog = new ExecuteWatchdog(timeunit.toMillis(timeout));
        return timeoutWatchdog;
    }

    public static int executeCommand(String command,
                                     ExecuteWatchdog watchdog,
                                     OutputStream outputStream,
                                     OutputStream errorStream,
                                     InputStream inputStream,
                                     Map<String, String> env) {
        CommandLine cmdLine = CommandLine.parse(command);
        DefaultExecutor executor = new DefaultExecutor();
        if (outputStream == null) {
            outputStream = new LogOutputStream() {
                @Override
                protected void processLine(String s, int i) {
                    log.error(s);
                }
            };
        }
        if (errorStream == null) {
            errorStream = new LogOutputStream() {
                @Override
                protected void processLine(String s, int i) {
                    log.error(s);
                }
            };
        }
        executor.setStreamHandler(new PumpStreamHandler(outputStream, errorStream, inputStream));
        executor.setExitValues(new int[]{0, 1});
        if (watchdog != null) {
            executor.setWatchdog(watchdog);
        }
        int exitValue;
        try {
            exitValue = executor.execute(cmdLine, env);
        } catch (IOException e) {
            exitValue = 1;
            LOGGER.error("error executing command", e);
        }
        return exitValue;
    }


}
