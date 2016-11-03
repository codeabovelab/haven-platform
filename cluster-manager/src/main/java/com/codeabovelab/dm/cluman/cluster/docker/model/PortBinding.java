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

import com.codeabovelab.dm.cluman.cluster.docker.model.Ports.Binding;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * In a {@link PortBinding}, a network socket on the Docker host, expressed as a {@link Binding}, is bound to an
 * {@link ExposedPort} of a container. A {@link PortBinding} corresponds to the <code>--publish</code> (<code>-p</code>)
 * option of the <code>docker run</code> (and similar) CLI command for adding port bindings to a container.
 * <p>
 * <i>Note: This is an abstraction used for creating new port bindings. It is not to be confused with the abstraction
 * used for querying existing port bindings from a container configuration in {@link NetworkSettings#getPorts()} and
 * {@link HostConfig#getPortBindings()}. In that context, a <code>Map&lt;ExposedPort, Binding[]&gt;</code> is used.</i>
 */
public class PortBinding {
    private final Binding binding;

    private final ExposedPort exposedPort;

    public PortBinding(Binding binding, ExposedPort exposedPort) {
        this.binding = binding;
        this.exposedPort = exposedPort;
    }

    public Binding getBinding() {
        return binding;
    }

    public ExposedPort getExposedPort() {
        return exposedPort;
    }

    public static PortBinding parse(String serialized) throws IllegalArgumentException {
        try {
            String[] parts = StringUtils.splitByWholeSeparator(serialized, ":");
            switch (parts.length) {
                case 3:
                    // 127.0.0.1:80:8080/tcp
                    return createFromSubstrings(parts[0] + ":" + parts[1], parts[2]);
                case 2:
                    // 80:8080 // 127.0.0.1::8080
                    return createFromSubstrings(parts[0], parts[1]);
                case 1:
                    // 8080
                    return createFromSubstrings("", parts[0]);
                default:
                    throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing PortBinding '" + serialized + "'", e);
        }
    }

    private static PortBinding createFromSubstrings(String binding, String exposedPort) throws IllegalArgumentException {
        return new PortBinding(Binding.parse(binding), ExposedPort.parse(exposedPort));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PortBinding) {
            PortBinding other = (PortBinding) obj;
            return new EqualsBuilder().append(binding, other.getBinding()).append(exposedPort, other.getExposedPort())
                    .isEquals();
        } else
            return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(binding).append(exposedPort).toHashCode();
    }

    @Override
    public String toString() {
        return "PortBinding{" +
                "binding=" + binding +
                ", exposedPort=" + exposedPort +
                '}';
    }
}
