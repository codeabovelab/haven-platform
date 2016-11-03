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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Tool for gathering application info
 *
 */
public class AppInfo {

    private static final Logger LOG = LoggerFactory.getLogger(AppInfo.class);
    private static final String name = "META-INF/MANIFEST.MF";
    private static final LazyInitializer<Manifest> MANIFEST_LAZY_INITIALIZER = new LazyInitializer<>(() -> {
        URL res;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if(cl instanceof URLClassLoader) {
            // we need to find resources from local jar
            URLClassLoader ucl = (URLClassLoader) cl;
            res = ucl.findResource(name);
        } else {
            res = cl.getResource(name);
        }
        if(res == null) {
            LOG.error("No appropriate manifests.");
            return null;
        } else {
            LOG.error("Found manifests at {}.", res);
        }
        try(InputStream is = res.openStream()) {
            return new Manifest(is);
        } catch (IOException e) {
            LOG.error("On resource:" + name, e);
        }
        return null;
    });

    /**
     * extract '$artifactId' from manifest (Implementation-Title) or other places.
     * @return
     */
    public static String getApplicationName() {
        return getApplicationName(MANIFEST_LAZY_INITIALIZER.get());
    }

    /**
     * extract '$version' from manifest (Implementation-Version) or other places.
     * @return
     */
    public static String getApplicationVersion() {
        Manifest manifest = MANIFEST_LAZY_INITIALIZER.get();
        if(manifest == null) {
            return null;
        }
        return manifest.getMainAttributes().getValue("Implementation-Version");
    }

    static String getApplicationName(Manifest manifest) {
        if(manifest == null) {
            return null;
        }
        Attributes attributes = manifest.getMainAttributes();
        return/* attributes.getValue("Implementation-Vendor-Id") + ":" +  /* disabled because not need */
               attributes.getValue("Implementation-Title");
    }
}
