package com.codeabovelab.dm.cluman.cluster.docker.management;

import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfig;
import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfigImpl;
import com.codeabovelab.dm.cluman.cluster.docker.HttpAuthInterceptor;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetContainersArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.TagImageArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.cluster.docker.model.CreateNetworkCmd;
import com.codeabovelab.dm.cluman.cluster.docker.model.Image;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.cluster.registry.model.PrivateRegistryConfig;
import com.codeabovelab.dm.cluman.configs.container.ConfigsFetcherImage;
import com.codeabovelab.dm.cluman.configs.container.ContainerCreationContext;
import com.codeabovelab.dm.cluman.configs.container.DefaultParser;
import com.codeabovelab.dm.cluman.model.DockerContainer;
import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import com.codeabovelab.dm.cluman.model.NodeInfoProvider;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DockerServiceImplTest {

    private DockerServiceImpl service;

    @Before
    public void prepare() throws Exception {
        service = dockerService();
    }

    @SuppressWarnings("unchecked")
    DockerServiceImpl dockerService() {
        ClusterConfig config = ClusterConfigImpl.builder()
                .addHost("localhost:2375").build();
        AsyncRestTemplate restTemplate = new AsyncRestTemplate();
        RegistryRepository registryRepository = mock(RegistryRepository.class);
        restTemplate.setInterceptors(
                Collections.singletonList(
                        new HttpAuthInterceptor(registryRepository)));
        return DockerServiceImpl.builder()
          .config(config)
          .cluster("test")
          .restTemplate(restTemplate)
          .nodeInfoProvider(mock(NodeInfoProvider.class))
          .eventConsumer(mock(MessageBus.class))
          .build();
    }

    @Test
    @Ignore
    public void test() {
        List<DockerContainer> containers = service.getContainers(new GetContainersArg(true));
        System.out.println(containers);

        for (DockerContainer container : containers) {
            ContainerDetails containerDetails = service.getContainer(container.getId());
            System.out.println(containerDetails);
            ImageDescriptor image = service.pullImage(containerDetails.getConfig().getImage(), null);
            System.out.println(image);
        }
    }

    @Test
    @Ignore
    public void testTag() {
        HttpAuthInterceptor.setCurrentName("ni1.codeabovelab.com");
        TagImageArg arg = TagImageArg.builder().remote(true)
                .imageName("cluster-manager")
                .repository("ni1.codeabovelab.com")
                .currentTag("latest")
                .newTag("testTag").build();

        ServiceCallResult tag = service.createTag(arg);
        Assert.assertNotNull(tag);
    }

    @Test
    @Ignore
    public void testNetwork() throws JsonProcessingException {
        CreateNetworkCmd createNetworkCmd = new CreateNetworkCmd();
        createNetworkCmd.setDriver("overlay");
        createNetworkCmd.setName("test-test");
        ServiceCallResult network = service.createNetwork(createNetworkCmd);
        System.out.println(network);

    }

/*
    @Test
    @Ignore
    public void testCreateContainer() throws JsonProcessingException {
        com.codeabovelab.dm.cluman.cluster.docker.management.argument.CreateContainerArg createContainerArg = new com.codeabovelab.dm.cluman.cluster.docker.management.argument.CreateContainerArg();
        createContainerArg.setImage("<replace_me>");
        createContainerArg.setContainerName("");
        createContainerArg.setCpuShares(512);
        createContainerArg.setEnvironment(Collections.<String, String>singletonMap("JAVA_OPTS", "-Xmx128m"));
        createContainerArg.setRestart("no");
        createContainerArg.setPublish("8080:8080");
        CreateContainerArg createContainerCmd = service.buildCreateContainer(createContainerArg, null);
        ObjectMapper objectMapper = JacksonUtils.objectMapperBuilder();
        String s = objectMapper.writeValueAsString(createContainerCmd);
        Assert.assertNotNull(createContainerCmd);
        System.out.println(s);

        ServiceCallResult containerRest = service.createContainer(createContainerArg);
        System.out.println(containerRest);
    }
*/
    @Test
    @Ignore
    public void testPullImage() throws JsonProcessingException {

        String imageName = "ni1.codeabovelab.com/cluster-manager:latest";
        ImageDescriptor image = service.pullImage(imageName, null);
        System.out.println(image);

        ContainerCreationContext context = ContainerCreationContext.builder().cluster("cluster").image(image)
                .imageName(imageName).build();
        ConfigsFetcherImage fetcherImage = new ConfigsFetcherImage(Collections.singletonList(new DefaultParser()));
        fetcherImage.resolveProperties(context);
        Assert.assertFalse(context.getArgList().isEmpty());
    }

}