package com.codeabovelab.dm.cluman.cluster.registry;

import com.codeabovelab.dm.cluman.cluster.filter.Filter;
import com.codeabovelab.dm.cluman.cluster.registry.aws.AwsService;
import com.codeabovelab.dm.cluman.cluster.registry.data.ImageCatalog;
import com.codeabovelab.dm.cluman.cluster.registry.data.SearchResult;
import com.codeabovelab.dm.cluman.cluster.registry.data.Tags;
import com.codeabovelab.dm.cluman.ds.SwarmsConfig;
import com.codeabovelab.dm.common.kv.KeyValueStorage;
import com.codeabovelab.dm.common.kv.mapping.KvClassMapper;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import com.codeabovelab.dm.common.json.JacksonConfiguration;
import com.codeabovelab.dm.common.mb.MessageBus;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RegistryRepositoryTest.Config.class)
@TestPropertySource(properties = {
        "dm.registries.syncInit=true",

        "dm.registries.awsRegistry[0].accessKey=",
        "dm.registries.awsRegistry[0].secretKey=",
        "dm.registries.awsRegistry[0].region=",

        "dm.registries.hubRegistry[0].username=",
        "dm.registries.hubRegistry[0].password=",

        "dm.registries.privateRegistry[0].url=https://ni1.codeabovelab.com",
        "dm.registries.privateRegistry[0].username=codeabovelab_test_user",
        "dm.registries.privateRegistry[0].password=codeabovelab_test_password"
})
@Ignore("depends on remote services")
public class RegistryRepositoryTest {

    private static final String PRIVATE_REGISTRY = "ni1.codeabovelab.com";
    private static final String AWS_REGISTRY = "893974199991.dkr.ecr.us-west-2.amazonaws.com";

    @Autowired
    RegistryRepository registryRepository;

    @Test
    public void checkHeath() throws Exception {
        RegistryService dhub = registryRepository.getByName(null);
        Assert.isTrue(dhub.checkHealth());

        RegistryService pdhub = registryRepository.getByName("codeabovelab");
        Assert.isTrue(pdhub.checkHealth());

        RegistryService awshub = registryRepository.getByName(AWS_REGISTRY);
        Assert.isTrue(awshub.checkHealth());

        RegistryService privHub = registryRepository.getByName(PRIVATE_REGISTRY);
        Assert.isTrue(privHub.checkHealth());
    }

    @Test
    public void getCatalog() throws Exception {
        List<ImageCatalog> catalog = registryRepository.getCatalog(Collections.singletonList(AWS_REGISTRY));
        Assert.notNull(catalog);

        List<ImageCatalog> catalogni1Full = registryRepository.getCatalog(Collections.singletonList(PRIVATE_REGISTRY));
        Assert.notNull(catalogni1Full);
    }

    @Test
    public void getTags() throws Exception {

        RegistryService privHub = registryRepository.getByName(PRIVATE_REGISTRY);
        Tags ni1Tags = privHub.getTags("cluster-manager");
        Assert.notNull(ni1Tags);
        Assert.isTrue(ni1Tags.getTags().size() > 0);

        RegistryService awshub = registryRepository.getByName(AWS_REGISTRY);
        Tags tagsAmazone = awshub.getTags("cluster-manager");
        Assert.notNull(tagsAmazone);
        Assert.isTrue(tagsAmazone.getTags().size() > 0);

        RegistryService pdhub = registryRepository.getByName("codeabovelab");
        Tags tags = pdhub.getTags("cluster-manager");
        Assert.notNull(tags);
        Assert.isTrue(tags.getTags().size() > 0);

        RegistryService dhub = registryRepository.getByName(null);
        Tags tagsTutum = dhub.getTags("tutum/ubuntu");
        Assert.notNull(tagsTutum);
        Assert.isTrue(tagsTutum.getTags().size() > 0);

        dhub = registryRepository.getByName(null);
        Tags tagsUbuntu = dhub.getTags("ubuntu");
        Assert.notNull(tagsUbuntu);
        Assert.isTrue(tagsUbuntu.getTags().size() > 0);

    }

    @Test
    public void getTagsViaImageName() throws Exception {

        RegistryService privHub = registryRepository.getRegistryByImageName(PRIVATE_REGISTRY + "/cluster-manager");
        Tags ni1Tags = privHub.getTags("cluster-manager");
        Assert.notNull(ni1Tags);
        Assert.isTrue(ni1Tags.getTags().size() > 0);

        RegistryService awshub = registryRepository.getRegistryByImageName(AWS_REGISTRY+ "/cluster-manager");
        Tags tagsAmazone = awshub.getTags("cluster-manager");
        Assert.notNull(tagsAmazone);
        Assert.isTrue(tagsAmazone.getTags().size() > 0);

        RegistryService pdhub = registryRepository.getRegistryByImageName("codeabovelab"+ "/cluster-manager");
        Tags tags = pdhub.getTags("cluster-manager");
        Assert.notNull(tags);
        Assert.isTrue(tags.getTags().size() > 0);

        RegistryService dhub = registryRepository.getRegistryByImageName("tutum/ubuntu");
        Tags tagsTutum = dhub.getTags("ubuntu");
        Assert.notNull(tagsTutum);
        Assert.isTrue(tagsTutum.getTags().size() > 0);

        dhub = registryRepository.getRegistryByImageName("ubuntu");
        Tags tagsUbuntu = dhub.getTags("ubuntu");
        Assert.notNull(tagsUbuntu);
        Assert.isTrue(tagsUbuntu.getTags().size() > 0);

    }
//
//    @Test
//    public void removeTag() throws Exception {
//
//    }

    @Test
    public void getImage() throws Exception {
        ImageDescriptor image = registryRepository.getByName(PRIVATE_REGISTRY).getImage("cluster-manager", "latest");
        Assert.notNull(image);

        ImageDescriptor imageUbuntu = registryRepository.getByName(null).getImage("ubuntu", "latest");
        Assert.notNull(imageUbuntu);

//        ImageDescriptor tutumUbuntu = registryRepository.getImage("ubuntu", "latest", "tutum");
//        Assert.notNull(tutumUbuntu);
    }

    @Test
    public void getAvailableRegistries() throws Exception {
        Collection<String> availableRegistries = registryRepository.getAvailableRegistries();
        Assert.isTrue(!availableRegistries.isEmpty());
    }


    @Test
    public void testSearch() throws Exception {
        DockerHubRegistry defaultRegisrty = (DockerHubRegistry) registryRepository.getDefaultRegistry();
        SearchResult ubuntu = defaultRegisrty.search("ubuntu", 0, 1000);
        Assert.notNull(ubuntu.getResults());
        Assert.notNull(ubuntu);
    }

    @Test
    @Ignore
    public void deleteTag() throws Exception {
        RegistryService privHub = registryRepository.getByName(PRIVATE_REGISTRY);
        List<String> tags = privHub.getTags("balancer-web").getTags();
        Assert.isTrue(!CollectionUtils.isEmpty(tags));
        String tag = tags.get(0);
        ImageDescriptor ni1 = privHub.getImage("balancer-web", tag);
        Assert.notNull(ni1);
        privHub.deleteTag("balancer-web", ni1.getId());

    }

    @EnableAutoConfiguration
    @Configuration
    @Import({RegistriesConfiguration.class, JacksonConfiguration.class})
    @EnableConfigurationProperties({SwarmsConfig.class})
    public static class Config {

        @Bean
        @SuppressWarnings("unchecked")
        public KvMapperFactory kvMapperFactory() {
            KvMapperFactory mapper = mock(KvMapperFactory.class);
            KeyValueStorage storage = mock(KeyValueStorage.class);
            when(mapper.getStorage()).thenReturn(mock(KeyValueStorage.class));
            when(storage.getDockMasterPrefix()).thenReturn("prefix");
            when(mapper.createClassMapper(anyString(), any(Class.class))).thenReturn(mock(KvClassMapper.class));
            return mapper;
        }

        @Bean(name = "bus.cluman.log")
        public MessageBus messageBus() {
            return mock(MessageBus.class);
        }

        @Bean
        public AwsService awsService() {
            return new AwsService();
        }

    }

}