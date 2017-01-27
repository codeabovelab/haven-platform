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

package com.codeabovelab.dm.cluman.cluster.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import lombok.Data;

/**
 * Container restart policy
 *
 * <dl>
 * <dt>no</dt>
 * <dd>Do not restart the container if it dies. (default)</dd>
 *
 * <dt>on-failure</dt>
 * <dd>Restart the container if it exits with a non-zero exit code. Can also accept an optional maximum restart count
 * (e.g. on-failure:5).
 * <dd>
 *
 * <dt>always</dt>
 * <dd>Always restart the container no matter what exit code is returned.
 * <dd>
 * </dl>
 *
 * @author marcus
 *
 */
@Data
public class RestartPolicy {

    public static final String NO = "no";
    public static final String ALWAYS = "always";
    public static final String ON_FAILURE = "on-failure";
    public static final String UNLESS_STOPPED = "unless-stopped";

    private final int maximumRetryCount;

    private final String name;

    private RestartPolicy(@JsonProperty("Name") String name,
                          @JsonProperty("MaximumRetryCount") int maximumRetryCount) {
        this.maximumRetryCount = maximumRetryCount;
        this.name = name;
    }

    /**
     * Do not restart the container if it dies. (default)
     */
    public static RestartPolicy noRestart() {
        return new RestartPolicy(NO, 0);
    }

    /**
     * Always restart the container no matter what exit code is returned.
     */
    public static RestartPolicy alwaysRestart() {
        return new RestartPolicy(ALWAYS, 0);
    }

    /**
     * Always restart the container regardless of the exit status, but do not start it on daemon startup if
     * the container has been put to a stopped state before.
     */
    public static RestartPolicy unlessStopped() {
        return new RestartPolicy(UNLESS_STOPPED, 0);
    }

    /**
     * Restart the container if it exits with a non-zero exit code.
     *
     * @param maximumRetryCount
     *            the maximum number of restarts. Set to <code>0</code> for unlimited retries.
     */
    public static RestartPolicy onFailureRestart(int maximumRetryCount) {
        return new RestartPolicy("on-failure", maximumRetryCount);
    }

    public Integer getMaximumRetryCount() {
        return maximumRetryCount;
    }

    public String getName() {
        return name;
    }

    /**
     * Parses a textual restart polixy specification (as used by the Docker CLI) to a {@link RestartPolicy}.
     *
     * @param serialized
     *            the specification, e.g. <code>on-failure:2</code>
     * @return a {@link RestartPolicy} matching the specification
     * @throws IllegalArgumentException
     *             if the specification cannot be parsed
     */
    public static RestartPolicy parse(String serialized) throws IllegalArgumentException {
        try {
            String[] parts = serialized.split(":");
            String name = parts[0];
            if (NO.equals(name)) {
                return noRestart();
            }
            if (ALWAYS.equals(name)) {
                return alwaysRestart();
            }
            if (UNLESS_STOPPED.equals(name)) {
                return unlessStopped();
            }
            if (ON_FAILURE.equals(name)) {
                int count = 0;
                if (parts.length == 2) {
                    count = Integer.parseInt(parts[1]);
                }
                return onFailureRestart(count);
            }
            throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing RestartPolicy '" + serialized + "'");
        }
    }

    /**
     * Returns a string representation of this {@link RestartPolicy}. The format is <code>name[:count]</code>, like the
     * argument in {@link #parse(String)}.
     *
     * @return a string representation of this {@link RestartPolicy}
     */
    @Override
    public String toString() {
        String result = name.isEmpty() ? "no" : name;
        return maximumRetryCount > 0 ? result + ":" + maximumRetryCount : result;
    }
}