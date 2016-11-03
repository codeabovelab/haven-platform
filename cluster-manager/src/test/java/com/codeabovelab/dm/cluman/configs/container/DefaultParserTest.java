package com.codeabovelab.dm.cluman.configs.container;

import com.codeabovelab.dm.cluman.job.JobConfiguration;
import com.codeabovelab.dm.cluman.job.JobParameters;
import com.codeabovelab.dm.cluman.model.ContainerSource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Map;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DefaultParserTest.TestConfiguration.class)
@TestPropertySource(properties = {
        "dm.job.predefined[job.sample].parameters.container.[ni1.codeabovelab.com/cluster-manager].environment.keyTest=keyValue",
        "dm.job.predefined[job.sample].parameters.container.[ni1.codeabovelab.com/cluster-manager].restart=always",
        "dm.job.predefined[job.sample].parameters.container.[ni1.codeabovelab.com/cluster-manager].blkioWeight=100"
        })
public class DefaultParserTest {


    @Autowired
    private JobConfiguration.JobsManagerConfiguration config;

    @SuppressWarnings("unchecked")
    @Test
    public void parse() throws Exception {
        DefaultParser defaultParser = new DefaultParser();
        ContainerCreationContext containerCreationContext = ContainerCreationContext.builder().build();
        Map<String, JobParameters.Builder> predefined = config.getPredefined();
        JobParameters.Builder builder = predefined.get("job.sample");
        Map<String, Object> map = (Map<String, Object>) builder.getParameters().get("container");
        Map<String, Object> args = (Map<String, Object>) map.get("ni1.codeabovelab.com/cluster-manager");
        defaultParser.parse(args, containerCreationContext);
        Assert.assertNotNull(containerCreationContext.getArgList());
        List<ContainerSource> argList = containerCreationContext.getArgList();
        ContainerSource createContainerArg = argList.get(0);
        Assert.assertEquals(new Integer(100), createContainerArg.getBlkioWeight());

    }

    @Configuration
    @EnableConfigurationProperties(JobConfiguration.JobsManagerConfiguration.class)
    public static class TestConfiguration {
    }
}