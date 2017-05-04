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

package com.codeabovelab.dm.agent.proxy;

import com.codeabovelab.dm.common.utils.Closeables;

import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;

/**
 */
class ImmediateCloseConnection implements HttpUpgradeHandler {
    interface CloseableSupplier {
        AutoCloseable get() throws Exception;
    }

    @Override
    public void init(WebConnection wc) {
        close(wc::getInputStream);
        close(wc::getOutputStream);
    }

    private void close(CloseableSupplier closeable) {
        try {
            AutoCloseable ac = closeable.get();
            Closeables.close(ac);
        } catch (Exception e) {
            // we do not want to known what it happen here
        }
    }

    @Override
    public void destroy() {

    }
}
