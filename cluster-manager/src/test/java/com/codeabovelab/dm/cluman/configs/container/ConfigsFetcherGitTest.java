package com.codeabovelab.dm.cluman.configs.container;

import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerConfig;
import com.codeabovelab.dm.cluman.cluster.docker.model.Image;
import com.codeabovelab.dm.cluman.configuration.DataLocatinConfiguration;
import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ConfigsFetcherGitTest {

    @Test
    public void testResolveProperties() throws Exception {

        ConfigProvider configProvider = createConfigProvider();
        ContainerSource nc = new ContainerSource();
        nc.setBlkioWeight(512);

        ContainerConfig.Builder config = ContainerConfig.builder();
        config.labels(Collections.singletonMap("arg.publish", "8761:8761"));
        Image image = Image.builder().containerConfig(config.build()).build();

        ContainerSource result = configProvider.resolveProperties("dev", image, "cluster-manager:latest", nc);

        Assert.assertNotNull(result.getBlkioWeight());
        Assert.assertNotNull(result.getEnvironment());

    }

    public static ConfigProvider createConfigProvider() throws Exception {
        GitSettings gitSettings = new GitSettings();
        gitSettings.setUrl("https://github.com/codeabovelab/dockmaster-example-container-configuration.git");
        //test read only user for test repo
        List<Parser> parsers = new ArrayList<Parser>() {{
            add(new DefaultParser());
            add(new YamlParser());
            add(new PropertiesParser());
        }};
        DataLocatinConfiguration dataLocatinConfiguration = new DataLocatinConfiguration();
        dataLocatinConfiguration.setLocation(Files.createTempDir().getPath());
        ConfigsFetcherGit configsFetcherGit = new ConfigsFetcherGit(gitSettings, dataLocatinConfiguration, parsers);

        List<ConfigsFetcher> fetchers = new ArrayList<>();
        fetchers.add(configsFetcherGit);
        fetchers.add(new ConfigsFetcherImage(Collections.singletonList(new DefaultParser())));
        ConfigProvider configProvider = new ConfigProviderImpl(fetchers);
        return configProvider;
    }
}