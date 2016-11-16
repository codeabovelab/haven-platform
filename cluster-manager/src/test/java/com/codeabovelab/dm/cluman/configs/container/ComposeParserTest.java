package com.codeabovelab.dm.cluman.configs.container;

import com.codeabovelab.dm.cluman.cluster.compose.model.ComposeModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ComposeParserTest {

    @Test
    public void testLoadFile() throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, ComposeModel> configuration = mapper.readValue(ComposeParserTest.class.getResourceAsStream("/docker-compose.yml"),
                mapper.getTypeFactory().constructMapType(HashMap.class, String.class, ComposeModel.class));
        Assert.assertNotNull(configuration);
        Assert.assertFalse(configuration.isEmpty());

    }

}