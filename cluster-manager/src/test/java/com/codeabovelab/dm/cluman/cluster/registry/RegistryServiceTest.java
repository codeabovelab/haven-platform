package com.codeabovelab.dm.cluman.cluster.registry;

import com.codeabovelab.dm.cluman.cluster.registry.data.ImageCatalog;
import com.codeabovelab.dm.cluman.cluster.registry.data.Manifest;
import com.codeabovelab.dm.cluman.cluster.registry.data.Tags;
import com.codeabovelab.dm.cluman.cluster.registry.model.PrivateRegistryConfig;
import com.codeabovelab.dm.common.json.JacksonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public class RegistryServiceTest {

    private RegistryServiceImpl service;

    @Before
    public void setUp() throws Exception {
        PrivateRegistryConfig config = new PrivateRegistryConfig();
        config.setUrl("https://registry.local");
        service = RegistryServiceImpl.builder()
                .adapter(new PrivateRegistryAdapter(config, (a) -> new RestTemplate()))
                .searchConfig(new SearchIndex.Config())
                .build();
    }

    @Test
    @Ignore
    public void testGetCatalog() throws Exception {
        ImageCatalog imageCatalog = service.getCatalog();
        Assert.assertNotNull(imageCatalog);
        Assert.assertFalse(imageCatalog.getImages().isEmpty());

    }

    @Test
    @Ignore
    public void testGetTags() throws Exception {
        ImageCatalog imageCatalog = service.getCatalog();
        List<String> repositories = imageCatalog.getImages();
        for (String repository : repositories) {
            Tags tags = service.getTags(repository);
            Assert.assertNotNull(tags);
            Assert.assertFalse(tags.getTags().isEmpty());
        }
    }

    @Test
    public void testGetImage() throws IOException {
        ObjectMapper mapper = JacksonUtils.objectMapperBuilder();

        InputStream resourceAsStream = RegistryServiceTest.class.getResourceAsStream("/manifest.json");
        Manifest manifest = mapper.readValue(resourceAsStream, Manifest.class);
        Assert.assertNotNull(manifest);
        Assert.assertNotNull(manifest.getConfig());
        Assert.assertNotNull(manifest.getConfig().getDigest());
        //TODO ImageDescriptor image = service.getImage(parseManifest(manifest);
        //Assert.assertNotNull(image);
        //Assert.assertNotNull(image.getContainerConfig());
    }
}