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

package com.codeabovelab.dm.cluman.configs.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class AbstractParser implements Parser {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractParser.class);

    protected void parse(String fileName, ContainerCreationContext context, String extension) {
        File initialFile = new File(fileName + extension);
        LOG.info("checking for existing file {}", initialFile);
        if (initialFile.exists()) {
                LOG.info("ok. parsing file {}", initialFile);
                parse(initialFile, context);
        }
    }

}
