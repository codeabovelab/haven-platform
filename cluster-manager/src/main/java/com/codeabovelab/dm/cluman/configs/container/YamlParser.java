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

import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Parser for yaml configs
 */
@Component
@Slf4j
public class YamlParser extends AbstractParser {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Override
    public void parse(String fileName, ContainerCreationContext context) {
        parse(fileName, context, ".yml");
    }

    @Override
    public void parse(File file, ContainerCreationContext context) {
        try {
            ContainerSource configuration = mapper.readValue(file, ContainerSource.class);
            List<String> include = configuration.getInclude();
            include.forEach(a -> parse(new File(file.getParent(), a), context));
            context.addCreateContainerArg(configuration);
        } catch (IOException e) {
            log.error("can't parse configuration", e);
        }
    }

    @Override
    public void parse(Map<String, Object> map, ContainerCreationContext context) {
        return;
    }

    @Override
    public void parse(Map<String, Object> map, ContainerSource arg) {
        return;
    }

}
