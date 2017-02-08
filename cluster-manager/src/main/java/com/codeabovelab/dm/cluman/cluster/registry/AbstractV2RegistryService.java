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

package com.codeabovelab.dm.cluman.cluster.registry;

import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerConfig;
import com.codeabovelab.dm.cluman.cluster.registry.data.*;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryAdapter;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryConfig;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryCredentials;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.common.utils.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.springframework.web.util.UriComponentsBuilder.newInstance;

/**
 */
abstract class AbstractV2RegistryService implements RegistryService {
    protected static final Logger log = LoggerFactory.getLogger(AbstractV2RegistryService.class);
    private final RegistryAdapter adapter;
    private final LoadingCache<String[], ImageDescriptor> descriptorCache;
    private Consumer<RegistryEvent> eventConsumer;

    AbstractV2RegistryService(RegistryAdapter adapter) {
        this.adapter = adapter;
        // we use non expired cache, because imageId is descriptor hash, and it cannot be modified
        this.descriptorCache = CacheBuilder.<String[], ImageDescriptor>newBuilder()
                .build(new CacheLoader<String[], ImageDescriptor>() {
                    @Override
                    public ImageDescriptor load(String[] key) throws Exception {
                        Assert.isTrue(key.length == 2, "key array must have tho items");
                        return getDescriptor(key[0], key[1]);
                    }
                });
    }

    public Consumer<RegistryEvent> getEventConsumer() {
        return eventConsumer;
    }

    public void setEventConsumer(Consumer<RegistryEvent> eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    @Override
    public ImageCatalog getCatalog() {
        try {
            ImageCatalog catalog = getRestTemplate().getForObject(makeBaseUrl().path("_catalog").build().toUri(), ImageCatalog.class);
            online();
            return catalog;
        } catch (Exception e) {
            checkOnline(e);
            log.error("Can't fetch catalog from {}", getConfig(), e);
            return null;
        }
    }

    private void online() {
        toggleOnline(null);
    }

    /**
     * Do analysing of exception for connection errors, which is mean offline status
     *
     * @param e
     */
    private void checkOnline(Exception e) {
        ConnectException conn = Throwables.find(e, ConnectException.class);
        if (conn != null) {
            toggleOnline(conn.toString());
        }
    }

    @Override
    public boolean checkHealth() {
        String error = null;
        RegistryConfig config = getConfig();
        try {
            getRestTemplate().getForObject(makeBaseUrl().build().toUri(), String.class);
        } catch (Exception e) {
            log.error("Can't fetch catalog from {}", config, e);
            error = e.getMessage();
        }
        toggleOnline(error);
        return error == null;
    }

    private void toggleOnline(String error) {
        RegistryConfig config = getConfig();
        String oldMessage = config.getErrorMessage();
        boolean online = error == null;
        if (!Objects.equals(oldMessage, error)) {
            //error is changed, so we need to send event
            fireEvent(RegistryEvent.builder()
              .action(online ? StandardActions.ONLINE : StandardActions.OFFLINE)
              .severity(online ? Severity.INFO : Severity.ERROR)
              .message(error));
        }
        config.setErrorMessage(error);
    }

    void fireEvent(RegistryEvent.Builder reb) {
        if (eventConsumer == null) {
            return;
        }
        reb.setName(getConfig().getName());
        if (reb.getSeverity() == null) {
            reb.setSeverity(Severity.INFO);
        }
        eventConsumer.accept(reb.build());
    }

    private UriComponentsBuilder makeBaseUrl() {
        try {
            return newInstance().uri(new URI(adapter.getUrl())).path("/v2/");
        } catch (URISyntaxException e) {
            throw Throwables.asRuntime(e);
        }
    }

    @Override
    public Tags getTags(String name) {
        try {
            Tags tags = getRestTemplate().getForObject(forName(name).path("/tags/list").build().toUri(),
              Tags.class);
            online();
            List<String> tagList = tags.getTags();
            if (tagList != null) {
                tagList.sort(ImageNameComparator.getTagsComparator());
            }
            return tags;
        } catch (Exception e) {
            checkOnline(e);
            log.error("Can't fetch tags for {} from {}", name, getConfig(), e);
            return null;
        }
    }

    private UriComponentsBuilder forName(String name) {
        UriComponentsBuilder ucb = makeBaseUrl();
        String processed = adapter.adaptNameForUrl(toRelative(name));
        return ucb.path(processed);
    }

    @Override
    public String toRelative(String name) {
        String registryName = getConfig().getName();
        int len = registryName.length();
        if(name.length() > len + 1 && name.startsWith(registryName) && name.charAt(len) == '/') {
            // remove registry name + slash
            return name.substring(registryName.length() + 1);
        }
        return name;
    }

    //DELETE /v2/<name>/manifests/<reference>

    /**
     * @param name
     * @param reference must be digest!!!
     */
    @Override
    public void deleteTag(String name, String reference) {
        getRestTemplate().delete(forName(name).path("/manifests/").path(reference).build().toUri());
    }

    //"{protocol}://{host}:{port}/v2/{name}/manifests/{reference}
    private Manifest getManifest(String name, String reference) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(new MediaType("application", "vnd.docker.distribution.manifest.v2+json")));
        HttpEntity entity = new HttpEntity<>(headers);
        URI uri = forName(name).path("/manifests/").path(reference).build().toUri();
        try {
            ResponseEntity<Manifest> exchange = getRestTemplate().exchange(uri, HttpMethod.GET, entity, Manifest.class);
            return exchange.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            log.error("can't fetch manifest from {} by {}", uri, e.getMessage());
            throw e;
        }
    }

    //{protocol}://{host}:{port}/v2/{name}/blobs/{digest}
    private <T> T getBlob(String name, String digest, Class<T> type) {
        return getRestTemplate().getForObject(forName(name).path("/blobs/").path(digest).build().toUri(), type);
    }

    public ImageDescriptor getImage(String fullImageName) {
        ImageName parsed = ImageName.parse(fullImageName);
        return getImage(parsed.getName(), parsed.getTag());
    }
    @Override
    public ImageDescriptor getImage(String name, String reference) {
        String imageId = getImageId(name, reference);
        if (imageId == null) {
            return null;
        }
        try {
            return this.descriptorCache.get(new String[]{name, imageId});
        } catch (ExecutionException e) {
            throw Throwables.asRuntime(e.getCause());
        }
    }

    private ImageDescriptor getDescriptor(String name, String imageId) {
        ImageData imageData = getBlob(name, imageId, ImageData.class);
        ContainerConfig cc = imageData.getContainerConfig();
        return ImageDescriptorImpl.builder()
                .id(imageId)
                .containerConfig(cc)
                .created(imageData.getCreated())
                .labels(cc.getLabels())
                .build();
    }

    /**
     * Give image id for specified tag
     *
     * @param name
     * @param reference
     * @return
     */
    protected String getImageId(String name, String reference) {
        Manifest manifest = getManifest(name, reference);
        // it happen when image with this tag is not found
        if (manifest == null) {
            return null;
        }
        Manifest.Entry config = manifest.getConfig();
        if (manifest.getConfig() == null) {
            log.warn("Manifest has outdated version for {}: {}", name, reference);
            return null;
        }
        return config.getDigest();
    }

    @Override
    public abstract SearchResult search(String searchTerm, int page, int count);

    public RegistryConfig getConfig() {
        return adapter.getConfig();
    }

    RestTemplate getRestTemplate() {
        return adapter.getRestTemplate();
    }


    protected void processStatusCodeException(HttpStatusCodeException e) {
        String message;
        try {
            message = MessageFormat.format("Response from server: {0} {1}",
                    e.getStatusCode().value(),
                    e.getStatusText());
            //we do not read error body, because it contains html code in some cases
        } catch (Exception ex) {
            message = e.getStatusText();
        }
        log.error("Error from server: {}", message, e);

    }

    @Override
    public RegistryCredentials getCredentials() {
        return adapter.getCredentials();
    }
}
