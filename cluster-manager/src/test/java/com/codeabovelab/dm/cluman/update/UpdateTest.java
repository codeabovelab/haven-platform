package com.codeabovelab.dm.cluman.update;

import com.codeabovelab.dm.cluman.DockerServiceMock;
import com.codeabovelab.dm.cluman.batch.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetContainersArg;
import com.codeabovelab.dm.cluman.cluster.docker.model.CreateContainerCmd;
import com.codeabovelab.dm.cluman.cluster.docker.model.HostConfig;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.configs.container.ConfigProvider;
import com.codeabovelab.dm.cluman.configs.container.DefaultParser;
import com.codeabovelab.dm.cluman.configs.container.Parser;
import com.codeabovelab.dm.cluman.ds.SwarmClusterContainers;
import com.codeabovelab.dm.cluman.ds.container.*;
import com.codeabovelab.dm.cluman.ds.swarm.NetworkManager;
import com.codeabovelab.dm.cluman.job.*;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.source.ContainerSourceFactory;
import com.codeabovelab.dm.cluman.ui.health.HealthCheckService;
import com.codeabovelab.dm.cluman.ui.update.UpdateContainersConfiguration;
import com.codeabovelab.dm.cluman.ui.update.UpdateContainersUtil;
import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import com.codeabovelab.dm.common.healthcheck.ServiceHealthCheckResultImpl;
import com.codeabovelab.dm.common.utils.Consumers;
import com.codeabovelab.dm.common.utils.Uuids;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(classes = UpdateTest.AppConfiguration.class)
public class UpdateTest {

    static final String TESTIMAGE = "testimage";
    static final String SRC_VERSION = "1";
    static final String TARGET_VERSION = "2";
    static final String IMAGE_SRC = TESTIMAGE + ":" + SRC_VERSION;
    static final String IMAGE_SRC_ID = "sha256:6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b";
    static final String IMAGE_TARGET = TESTIMAGE + ":" + TARGET_VERSION;
    static final String IMAGE_TARGET_ID = "sha256:d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35";
    static final String TESTCLUSTER = "testcluster";
    static final String IMAGE_ID = "sha256:4355a46b19d348dc2f57c046f8ef63d4538ebb936000f3c9ee954a27460dd865";
    private static final String C_IGNORE = "ignore-container";

    @Import({JobConfiguration.class, UpdateContainersConfiguration.class})
    @Configuration
    @EnableAutoConfiguration(exclude = EndpointWebMvcAutoConfiguration.class)
    public static class AppConfiguration {

        @Bean(name = "com.codeabovelab.dm.cluman.configs.DefaultParser")
        Parser parser() {
            return new DefaultParser();
        }

        @Bean
        NodeRegistry nodeRegistry() {
            return mock(NodeRegistry.class);
        }

        @Bean
        ConfigProvider configProvider() {
            ConfigProvider confProv = mock(ConfigProvider.class);
            when(confProv.resolveProperties(anyString(), anyObject(), anyString(), anyObject()))
              .then((i) -> i.getArgumentAt(3, Object.class));
            return confProv;
        }

        @Bean
        ContainerStorage containerStorage() {
            ContainerStorage contStorage = mock(ContainerStorage.class);
            when(contStorage.updateAndGetContainer(anyObject(), anyString())).thenReturn(mock(ContainerRegistration.class));
            return contStorage;
        }

        @Bean
        DiscoveryStorage discoveryStorage() {
            DiscoveryStorage mock = mock(DiscoveryStorage.class);

            return mock;
        }

        @Bean
        ContainerSourceFactory containerSourceFactory(ObjectMapper objectMapper) {
            return new ContainerSourceFactory(objectMapper);
        }

        @Bean
        RegistryRepository registryRepository() {
            return mock(RegistryRepository.class);
        }

        @Bean
        NetworkManager networkManager() {
            return mock(NetworkManager.class);
        }

        @Bean
        ContainersNameService containersNameService() {
            return new ContainersNameService(new ContainerNamesSupplier());
        }

        @Bean
        ContainerCreator containerManager(DiscoveryStorage discoveryStorage,
                NodeRegistry nodeRegistry,
                ConfigProvider configProvider,
                ContainersNameService containersNameService,
                ContainerStorage containerStorage,
                ContainerSourceFactory containerSourceFactory) {
            return new ContainerCreator(discoveryStorage, nodeRegistry, configProvider,
                    containersNameService, containerStorage, containerSourceFactory);
        }

        @Bean
        HealthCheckService healthCheckService() throws InterruptedException {
            //note that transactional attribute on service will broke mock
            HealthCheckService s = mock(HealthCheckService.class);
            when(s.checkContainer(anyString(), anyString(), anyLong())).thenReturn(ServiceHealthCheckResultImpl.builder()
              .healthy(true)
              .build());

            return s;
        }

        @Bean
        Validator validator() {
            return Validation.buildDefaultValidatorFactory().getValidator();
        }


    }

    @Autowired
    private JobsManager jobsManager;

    @Autowired
    private DiscoveryStorage discoveryStorage;

    @Autowired
    private ContainerCreator containerCreator;

    private void addContainer(String name, String image) {
        names.add(name);
        CreateContainerCmd cc = new CreateContainerCmd();
        cc.setName(name);
        cc.setImage(image);
        HostConfig.HostConfigBuilder hc = HostConfig.builder();
        hc.blkioWeight(1);
        hc.cpuShares(1);
        hc.cpuPeriod(1);
        hc.cpuQuota(1);
        hc.cpusetCpus("");
        hc.cpusetMems("");
        cc.setHostConfig(hc.build());
        discoveryStorage.getService(TESTCLUSTER).createContainer(cc);
    }

    private final Set<String> names = Collections.synchronizedSet(new HashSet<>());

    @Before
    public void before() {

        initCluster();

        addContainer("one-container", IMAGE_SRC);
        addContainer("two-container", IMAGE_SRC);
        addContainer("three-container", IMAGE_SRC);
        addContainer(C_IGNORE, IMAGE_SRC);
        addContainer("buggy-container", IMAGE_ID);
    }

    private void initCluster() {
        DockerServiceMock ds = new DockerServiceMock(DockerServiceInfo.builder()
          .name(TESTCLUSTER)
          .build());
        ds.defineImage(DockerServiceMock.ImageStub.builder()
          .id(IMAGE_SRC_ID)
          .name(IMAGE_SRC)
          .labels(Collections.emptyMap())
          .build());
        ds.defineImage(DockerServiceMock.ImageStub.builder()
          .id(IMAGE_TARGET_ID)
          .name(IMAGE_TARGET)
          .labels(Collections.emptyMap())
          .build());
        when(discoveryStorage.getService(ds.getCluster())).thenReturn(ds);

        NodesGroup ng = mock(NodesGroup.class);
        when(ng.getDocker()).thenReturn(ds);

        SwarmClusterContainers scc = new SwarmClusterContainers(() -> ds, containerCreator);
        when(ng.getContainers()).thenReturn(scc);

        when(discoveryStorage.getCluster(ds.getCluster())).thenReturn(ng);
    }

    private void checkNames(DockerContainer container) {
        String name = container.getName();
        boolean contains = names.contains(name);
        if(!contains) {
            fail("Can not find '" + name + "' in name of created containers: " + names);
        }
    }

    private void checkContainers(String expectedVersion, Consumer<DockerContainer> consumer) {
        DockerService ds = discoveryStorage.getService(TESTCLUSTER);
        List<DockerContainer> containers = ds.getContainers(new GetContainersArg(true));
        for(DockerContainer dc: containers) {
            consumer.accept(dc);
            String image = dc.getImage();
            if(IMAGE_ID.equals(image)) {
                continue;
            }
            String currVersion = ContainerUtils.getImageVersion(image);
            if(dc.getName().equals(C_IGNORE)) {
                // it must never changed
                assertEquals(SRC_VERSION, currVersion);
            } else {
                assertEquals(expectedVersion, currVersion);
            }
        }
        assertEquals(names.size(), containers.size());
    }

    @Test
    public void testStopThenStartAll() throws Exception {
        JobInstance ji = doStrategy("stopThenStartAll");
        checkContainers(TARGET_VERSION, this::checkNames);
        testRollback(ji);
        checkContainers(SRC_VERSION, this::checkNames);
    }

    @Test
    public void testStartThenStopEach() throws Exception {
        JobInstance ji = doStrategy("startThenStopEach");
        checkContainers(TARGET_VERSION, Consumers.nop());
        testRollback(ji);
        checkContainers(SRC_VERSION, Consumers.nop());
    }

    @Test
    public void testStopThenStartEach() throws Exception {
        JobInstance ji = doStrategy("stopThenStartEach");
        checkContainers(TARGET_VERSION, this::checkNames);
        testRollback(ji);
        checkContainers(SRC_VERSION, this::checkNames);
    }

    private void testRollback(JobInstance ji) throws InterruptedException, java.util.concurrent.ExecutionException {
        JobInstance rollbackJob = jobsManager.create(RollbackHandle.rollbackParams(ji.getInfo().getId()).build());
        executeJobInstance(rollbackJob);
    }

    private JobInstance doStrategy(String strategy) throws Exception {
        JobParameters.Builder b = JobParameters.builder();
        b.type(UpdateContainersUtil.JOB_PREFIX + strategy);
        //b.parameter(LoadContainersOfImageTasklet.JP_PERCENTAGE, percentage);
        b.parameter(BatchUtils.JP_CLUSTER, TESTCLUSTER);
        b.parameter(LoadContainersOfImageTasklet.JP_IMAGE, ImagesForUpdate.builder()
          .addImage(TESTIMAGE, SRC_VERSION, TARGET_VERSION)
          .getExcluded().addContainer(C_IGNORE).end()
          .build());

        b.parameter(HealthCheckContainerTasklet.JP_HEALTH_CHECK_ENABLED, true);
        b.parameter("id", Uuids.liteRandom());
        JobParameters params = b.build();
        JobInstance jobInstance = jobsManager.create(params);
        executeJobInstance(jobInstance);
        return jobInstance;
    }

    private void executeJobInstance(JobInstance jobInstance) throws InterruptedException, java.util.concurrent.ExecutionException {
        jobsManager.getSubscriptions().subscribeOnKey((e) -> {
            log.info("TEST JOB {}", e);

        },  jobInstance.getInfo());
        log.info("Try start job: {}", jobInstance.getInfo().getId());
        jobInstance.start();
        //wait end
        jobInstance.atEnd().get();
        assertEquals(JobStatus.COMPLETED, jobInstance.getInfo().getStatus());
    }
}
