package com.codeabovelab.dm.cluman.cluster.docker.management;

import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfig;
import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfigImpl;
import com.codeabovelab.dm.cluman.cluster.docker.HttpAuthInterceptor;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.cluster.docker.model.CreateNetworkCmd;
import com.codeabovelab.dm.cluman.cluster.docker.model.Volume;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Service;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.SwarmInspectResponse;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Task;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.configs.container.ConfigsFetcherImage;
import com.codeabovelab.dm.cluman.configs.container.ContainerCreationContext;
import com.codeabovelab.dm.cluman.configs.container.DefaultParser;
import com.codeabovelab.dm.cluman.model.DockerContainer;
import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import com.codeabovelab.dm.cluman.model.NodeInfoProvider;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;

public class DockerServiceImplTest {

    private DockerServiceImpl service;

    @Before
    public void prepare() throws Exception {
        service = dockerService();
    }

    @SuppressWarnings("unchecked")
    DockerServiceImpl dockerService() {
        ClusterConfig config = ClusterConfigImpl.builder()
                .host("172.31.0.12:2375").build();
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
          .objectMapper(new ObjectMapper())
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
    public void testNetwork() {
        CreateNetworkCmd createNetworkCmd = new CreateNetworkCmd();
        createNetworkCmd.setDriver("overlay");
        createNetworkCmd.setName("test-test");
        ServiceCallResult network = service.createNetwork(createNetworkCmd);
        System.out.println(network);

    }

    @Test
    @Ignore
    public void testPullImage() {

        String imageName = "ni1.codeabovelab.com/cluster-manager:latest";
        ImageDescriptor image = service.pullImage(imageName, null);
        System.out.println(image);

        ContainerCreationContext context = ContainerCreationContext.builder().cluster("cluster")
                .containerName(Optional.empty()).image(image)
                .imageName(imageName).build();
        ConfigsFetcherImage fetcherImage = new ConfigsFetcherImage(Collections.singletonList(new DefaultParser()));
        fetcherImage.resolveProperties(context);
        Assert.assertFalse(context.getArgList().isEmpty());
    }

    @Test
    @Ignore
    public void testGetSwarm() {
        SwarmInspectResponse swarm = service.getSwarm();
        Assert.assertNotNull(swarm);
    }

    @Test
    @Ignore
    public void testGetServices() {
        List<Service> services = service.getServices(new GetServicesArg());
        System.out.println(services);
        Assert.assertNotNull(services);
    }

    @Test
    @Ignore
    public void testGetTasks() {
        List<Task> tasks = service.getTasks(new GetTasksArg());
        System.out.println(tasks);
        Assert.assertNotNull(tasks);
    }

    @Test
    @Ignore
    public void testGetVolumes() {
        List<Volume> volumes = service.getVolumes(new GetVolumesArg());
        System.out.println(volumes);
        Assert.assertNotNull(volumes);
    }



}