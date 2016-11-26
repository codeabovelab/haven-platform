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
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Parser for properties like configs
 * TODO: migrate from CLI to compose API
 */
@Deprecated
@Component
public class PropertiesParser extends AbstractParser {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesParser.class);

    private final static String ENV = "environment";
    private final static String LABELS = "labels";

    @Override
    public void parse(String fileName, ContainerCreationContext context) {
        parse(fileName, context, ".properties");
    }

    @Override
    public void parse(File file, ContainerCreationContext context) {
        try (FileInputStream stream = new FileInputStream(file)) {
            Properties prop = new Properties();
            prop.load(stream);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = new HashMap<>((Map) prop);
            parse(map, context);
        } catch (IOException e) {
            LOG.error("can't parse Properties", e);
        }
    }

    @Override
    public void parse(Map<String, Object> map, ContainerCreationContext context) {
        ContainerSource arg = new ContainerSource();
        context.addCreateContainerArg(arg);
        parse(map, arg);
    }

    public void parse(Map<String, Object> map, ContainerSource arg) {
        for (Map.Entry<String, Object> el : map.entrySet()) {
            String propKey = el.getKey();
            Object elValue = el.getValue();
            if (elValue instanceof String) {
                String val = (String) elValue;
                String[] split = StringUtils.split(propKey, ".");
                if (split != null && StringUtils.hasText(val)) {
                    String key = split[1];
                    switch (split[0]) {
                        case ENV:
                            arg.getEnvironment().add(key + "=" + val);
                            break;
                        case LABELS:
                            arg.getLabels().put(key, val);
                            break;
                    }
                } else {
                    try {
                        Object value;
                        if (val.contains(":")) {
                            value = parseMap(val);
                        } else if (val.contains(",")) {
                            value = parseList(val);
                        } else {
                            value = el.getValue();
                        }
                        BeanUtils.setProperty(arg, propKey, value);
                    } catch (Exception e) {
                        LOG.error("can't set value {} to property {}", propKey, el.getValue());
                    }
                }
            }
        }
        LOG.info("Result of parsing {}", arg);
    }

    private List<String> parseList(String value) {
        List<String> list = new ArrayList<>();
        String[] split = value.split(",");
        Collections.addAll(list, split);
        return list;
    }

    private Map<String, String> parseMap(String value) {
        Map<String, String> map = new HashMap<>();
        String[] split = value.split(",");
        for (String s : split) {
            String[] m = s.split(":");
            if (m.length == 2) {
                map.put(m[0], m[1]);
            }
        }
        return map;
    }

}
