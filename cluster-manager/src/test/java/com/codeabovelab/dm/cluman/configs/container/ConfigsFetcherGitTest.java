package com.codeabovelab.dm.cluman.configs.container;

import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerConfig;
import com.codeabovelab.dm.cluman.cluster.docker.model.Image;
import com.codeabovelab.dm.cluman.configuration.DataLocationConfiguration;
import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ConfigsFetcherGitTest {

    @Test
    public void testResolveProperties() throws Exception {

        ConfigProvider configProvider = createConfigProvider();
        ContainerSource nc = new ContainerSource();
        nc.setBlkioWeight(512);

        ContainerConfig.ContainerConfigBuilder config = ContainerConfig.builder();
        config.labels(Collections.singletonMap("arg.publish", "8761:8761"));
        Image image = Image.builder().containerConfig(config.build()).build();

        ContainerSource result = configProvider.resolveProperties("dev", image, "cluster-manager:latest", nc);

        Assert.assertNotNull(result.getBlkioWeight());
        Assert.assertNotNull(result.getEnvironment());
        List<String> environment = result.getEnvironment();
        Map<String, String> envs = environment.stream().map(e -> e.split("=")).collect(Collectors.toMap(e -> e[0], e -> e[1]));
        Assert.assertNotNull(envs.get("MQ_HOST"));
    }

    static ConfigProvider createConfigProvider() {
        GitSettings gitSettings = new GitSettings();
        gitSettings.setUrl("https://github.com/codeabovelab/haven-example-container-configuration.git");
        DataLocationConfiguration dataLocationConfiguration = new DataLocationConfiguration();
        dataLocationConfiguration.setLocation(Files.createTempDir().getPath());
        List<Parser> parsers = Collections.singletonList(new YamlParser());
        List<ConfigsFetcher> fetchers = new ArrayList<ConfigsFetcher>() {{
            add(new ConfigsFetcherGit(gitSettings, dataLocationConfiguration, parsers));
            add(new ConfigsFetcherImage(parsers));
        }};

        return new ConfigProviderImpl(fetchers);
    }
}