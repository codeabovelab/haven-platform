/*
 * Copyright 2017 Code Above Lab LLC
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

package com.codeabovelab.dm.cluman.ui;

import com.codeabovelab.dm.cluman.cluster.registry.DockerHubRegistry;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 */
public class ImagesApiTest {
    @Test
    public void filterRegistries() throws Exception {
        test(set(DockerHubRegistry.DEFAULT_NAME, "test.registry"), "", "", set(DockerHubRegistry.DEFAULT_NAME));
        test(set(DockerHubRegistry.DEFAULT_NAME, "test.registry"), "*", "", set(DockerHubRegistry.DEFAULT_NAME, "test.registry"));
        test(set("registry", "test.registry"), "", "", set());
        test(set("registry", "test.registry"), null, "registry/image", set("registry"));
    }

    static void test(Set<String> src, String registry, String query, Set<String> expected) {
        ImagesApi.filterRegistries(registry, query, src);
        assertEquals(expected, src);
    }

    static Set<String> set(String ... str) {
        return new HashSet<>(Arrays.asList(str));
    }
}