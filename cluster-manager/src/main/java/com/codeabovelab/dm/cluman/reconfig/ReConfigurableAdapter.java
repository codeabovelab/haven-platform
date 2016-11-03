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

package com.codeabovelab.dm.cluman.reconfig;

/**
 * Object which support export/import of configuration.
 */
public interface ReConfigurableAdapter {

    /**
     * Produce object which can be serialized into config. Context provide some information about
     * current config format and etc.
     * @param ctx
     * @return
     */
    Object getConfig(ConfigWriteContext ctx);

    /**
     * Set readed config into object.
     * @param ctx
     * @param o object which retrieved from {@link #getConfig(ConfigWriteContext)}, serialized into config,
     *          then deserialized
     */
    void setConfig(ConfigReadContext ctx, Object o);
}
