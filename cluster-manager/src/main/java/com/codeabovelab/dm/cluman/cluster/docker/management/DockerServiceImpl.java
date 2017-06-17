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

package com.codeabovelab.dm.cluman.cluster.docker.management;

import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfig;
import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfigImpl;
import com.codeabovelab.dm.cluman.cluster.docker.HttpAuthInterceptor;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.model.*;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.*;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.model.Port;
import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import com.codeabovelab.dm.common.utils.Consumers;
import com.codeabovelab.dm.common.utils.SingleValueCache;
import com.codeabovelab.dm.common.utils.StringUtils;
import com.codeabovelab.dm.common.utils.Throwables;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.codeabovelab.dm.cluman.cluster.docker.management.DockerUtils.RESTART;
import static com.codeabovelab.dm.cluman.cluster.docker.management.DockerUtils.setCode;
import static com.google.common.base.MoreObjects.firstNonNull;
import static org.springframework.web.util.UriComponentsBuilder.newInstance;

/**
 * Implementation or docker service
 */
@Slf4j
public class DockerServiceImpl implements DockerService {

    @Data
    public static class Builder {
        private ClusterConfig config;
        private String cluster;
        private String node;
        private ObjectMapper objectMapper;
        private AsyncRestTemplate restTemplate;
        private NodeInfoProvider nodeInfoProvider;
        private Consumer<DockerServiceEvent> eventConsumer;
        /**
         * At this interceptor you may modify building of {@link DockerServiceInfo}
         */
        private Consumer<DockerServiceInfo.Builder> infoInterceptor;

        public Builder node(String node) {
            setNode(node);
            return this;
        }

        public Builder cluster(String cluster) {
            setCluster(cluster);
            return this;
        }

        public Builder config(ClusterConfig config) {
            setConfig(config);
            return this;
        }

        public Builder restTemplate(AsyncRestTemplate restTemplate) {
            setRestTemplate(restTemplate);
            return this;
        }

        public Builder nodeInfoProvider(NodeInfoProvider nodeInfoProvider) {
            setNodeInfoProvider(nodeInfoProvider);
            return this;
        }

        public Builder eventConsumer(Consumer<DockerServiceEvent> dockerServiceBus) {
            setEventConsumer(dockerServiceBus);
            return this;
        }

        public Builder infoInterceptor(Consumer<DockerServiceInfo.Builder> infoInterceptor) {
            setInfoInterceptor(infoInterceptor);
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            setObjectMapper(objectMapper);
            return this;
        }

        public DockerServiceImpl build() {
            return new DockerServiceImpl(this);
        }
    }

    private static final String SUFF_JSON = "/json";
    private static final long FAST_TIMEOUT = 10_000;
    private final AsyncRestTemplate restTemplate;
    private final ClusterConfig clusterConfig;
    //do not use this value, it need only for event generation
    private volatile DockerServiceInfo oldInfo;
    private final AtomicReference<OfflineCause> offlineRef = new AtomicReference<>(OfflineCause.INITIAL);
    private final SingleValueCache<DockerServiceInfo> infoCache;
    private final NodeInfoProvider nodeInfoProvider;
    private final ProcessEventProcessor frameStreamProcessor = new ProcessEventProcessor();
    private final JsonStreamProcessor<DockerEvent> eventStreamProcessor = new JsonStreamProcessor<>(DockerEvent.class);
    private final JsonStreamProcessor<Statistics> statisticsProcessor = new JsonStreamProcessor<>(Statistics.class);
    private final Consumer<DockerServiceEvent> eventConsumer;
    private final Consumer<DockerServiceInfo.Builder> infoInterceptor;
    private final ObjectMapper objectMapper;
    private final String node;
    private final String cluster;
    private final String id;
    private final long maxTimeout;
    private final URI uri;

    @SuppressWarnings("unchecked")
    public DockerServiceImpl(Builder b) {
        this.node = b.getNode();
        this.cluster = b.getCluster();
        // != below is not an error because we check that them both is not null
        Assert.isTrue((this.node == null || this.cluster == null) && this.node != this.cluster,
          "Invalid config of service: cluster=" + this.cluster + " node=" + node + " service must has only one non null value.");
        this.id = DockerService.super.getId();//cache id
        this.clusterConfig = ClusterConfigImpl.of(b.config).validate();
        this.restTemplate = b.restTemplate;
        Assert.notNull(this.restTemplate, "restTemplate is null");
        this.nodeInfoProvider = b.nodeInfoProvider;
        Assert.notNull(this.nodeInfoProvider, "nodeInfoProvider is null");
        this.eventConsumer = b.eventConsumer;
        Assert.notNull(this.eventConsumer, "eventConsumer is null");
        this.infoInterceptor = b.infoInterceptor;
        this.objectMapper = b.objectMapper;
        Assert.notNull(this.objectMapper, "objectMapper is null");

        this.uri = createUri();

        this.maxTimeout = Math.max(TimeUnit.SECONDS.toMillis(clusterConfig.getDockerTimeout()), FAST_TIMEOUT * 10);
        this.infoCache = SingleValueCache.builder(this::getInfoForCache)
                .timeAfterWrite(TimeUnit.SECONDS, this.clusterConfig.getCacheTimeAfterWrite())
                .build();
    }

    private URI createUri() {
        String address = getAddress();
        if(address.startsWith("http://") || address.startsWith("https://")) {
            return URI.create(address);
        }
        String arr[] = StringUtils.splitLast(address, ':');
        String host;
        int port = 80;
        if(arr == null) {
            host = address;
        } else {
            host = arr[0];
            port = Integer.parseInt(arr[1]);
        }
        try {
            return new URI("http", null, host, port, null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getCluster() {
        return cluster;
    }

    @Override
    public String getNode() {
        return node;
    }

    @Override
    public List<DockerContainer> getContainers(GetContainersArg arg) {
        Assert.notNull(arg, "arg is null");
        UriComponentsBuilder builder = makeUrl("containers/" + SUFF_JSON);
        builder.queryParam("all", arg.isAll() ? "1" : "0");
        ResponseEntity<Container[]> containers = getFast(() -> restTemplate.getForEntity(builder.toUriString(), Container[].class));
        ImmutableList.Builder<DockerContainer> lb = ImmutableList.builder();
        for (Container c : containers.getBody()) {
            DockerContainer.Builder dcb = DockerContainer.builder();
            toDockerContainer(c, dcb);
            lb.add(dcb.build());
        }
        return lb.build();
    }

    private <T> T getFast(Supplier<Future<T>> future) {
        // readonly ops interpreted as fast and use reduced timeout
        long timeout = FAST_TIMEOUT;
        if(getCluster() != null) {
            // this is cluster service and may consume more time than single node
            timeout *= 3; //we simply multiple it, but also may considering sount of nodes
        }
        timeout = Math.min(timeout, maxTimeout);
        return get(timeout, future);
    }

    private <T> T getSlow(Supplier<Future<T>> future) {
        return get(maxTimeout, future);
    }


    private <T> T get(long timeout, Supplier<Future<T>> supplier) {
        OfflineCause offlineCause = offlineRef.get();
        if(offlineCause != null) {
            offlineCause.throwIfActual(this);
        }
        try {
            if(timeout == 0) {
                timeout = maxTimeout;
            }
            Future<T> future = supplier.get();
            T val = future.get(timeout, TimeUnit.MILLISECONDS);
            online();
            return val;
        } catch (ExecutionException | TimeoutException e) {
            Throwable cause = (e instanceof ExecutionException)? e.getCause() : e;
            checkOffline(cause);
            throw Throwables.asRuntime(cause);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isOnline() {
        return this.offlineRef.get() == null;
    }

    private void online() {
        OfflineCause old = offlineRef.getAndSet(null);
        if(old != null) {
            fireEvent(new DockerServiceEvent(this, StandardActions.ONLINE));
        }
    }

    private void checkOffline(Throwable th) {
        if(!(th instanceof SocketException) && !(th instanceof TimeoutException)) {
            return;
        }
        long timeout = FAST_TIMEOUT;
        OfflineCause old = null;
        boolean updated = false;
        while(!updated) {
            old = offlineRef.get();
            if(old != null && !old.isActual()) {
                // we increase timeout at repeated errors,
                // but do not allow it to exceed maxTimeout
                // and we get timeout only from _not_ actual OC
                timeout = Math.min(old.getTimeout() * 2L, maxTimeout);
            }
            updated = offlineRef.compareAndSet(old, new OfflineCause(timeout, th));
        }
        if(old == null) {
            fireEvent(new DockerServiceEvent(this, StandardActions.OFFLINE));
        }
    }

    @Override
    public List<ImageItem> getImages(GetImagesArg arg) {
        UriComponentsBuilder builder = makeUrl("images/" + SUFF_JSON);
        builder.queryParam("all", arg.isAll() ? "1" : "0");
        // 'filters' have too complex format, we need to implement high level filtering api for it
        // filters – a JSON encoded value of the filters (a map[string][]string) to process on the images list. Available filters:
        //   dangling=true
        //   label=key or label="key=value" of an image label
        //builder.queryParam("filters", arg.getFilters());

        //filter - support only full image name with repo, not mask or substring
        builder.queryParam("filter", arg.getName());
        URI uri = builder.build().toUri();
        try {
            ResponseEntity<ImageItem[]> entity = getFast(() -> restTemplate.getForEntity(uri, ImageItem[].class));
            return Arrays.asList(entity.getBody());
        } catch (HttpClientErrorException e) {
            processStatusCodeException(e, new ServiceCallResult(), uri);
            throw e;
        }
    }


    /**
     * POST /images/(name)/tag
     * <p>
     * Tag the image name into a repository
     * <p>
     * Example request:
     * <p>
     * POST /images/test/tag?repo=myrepo&force=0&tag=v42 HTTP/1.1
     * <p>
     * Example response:
     * <p>
     * HTTP/1.1 201 Created
     *
     * @param cmd
     * @return
     */
    @Override
    public ServiceCallResult createTag(TagImageArg cmd) {
        URI uri = null;
        try {
            URI localTagUri;
            {
                UriComponentsBuilder ub = makeUrl("images/")
                        .path(ContainerUtils.buildImageName(cmd.getRepository(), cmd.getImageName(), cmd.getCurrentTag())).path("/tag");
                localTagUri = uri = ub.queryParam("force", cmd.getForce())
                  .queryParam("repo", cmd.getRepository() + "/" + cmd.getImageName())
                  .queryParam("tag", cmd.getNewTag())
                  .build().toUri();
            }
            ResponseEntity<String> res = getSlow(() -> restTemplate.exchange(localTagUri, HttpMethod.POST, null, String.class));
            if (Boolean.TRUE.equals(cmd.getRemote())) {
                HttpAuthInterceptor.setCurrentName(cmd.getRepository());
                uri = makeUrl("images/").path(ContainerUtils.buildImageName(cmd.getRepository(), cmd.getImageName(), null))
                  .path("/push").queryParam("tag", cmd.getNewTag())
                  .build().toUri();
                restTemplate.exchange(uri, HttpMethod.POST, null, String.class);
            }
            return DockerUtils.getServiceCallResult(res);
        } catch (HttpStatusCodeException e) {
            ServiceCallResult res = new ServiceCallResult();
            processStatusCodeException(e, res, uri);
            return res;
        }
    }

    private void toDockerContainer(Container c, DockerContainer.Builder dcb) {
        dcb.setId(c.getId());
        dcb.setImage(c.getImage());
        String imageId = c.getImageId();
        if (imageId == null) {
            log.warn("'ImageID' for '{}' container is null, it may error or old version of docker/swarm.", c.getId());
        }
        dcb.setImageId(imageId);
        dcb.setCommand(c.getCommand());
        dcb.setCreated(c.getCreated() * 1000L);
        List<com.codeabovelab.dm.cluman.cluster.docker.model.Port> ports = c.getPorts();
        if(ports != null) {
            ports.forEach(p -> {
                dcb.getPorts().add(new Port(p.getPrivatePort(), p.getPublicPort(), p.getType()));
            });
        }
        dcb.setLabels(c.getLabels());
        dcb.setStatus(c.getStatus());
        dcb.setState(DockerContainer.State.fromString(c.getState()));

        resolveNameAndNode(c, dcb);

        dcb.setImage(ContainerUtils.getFixedImageName(dcb));
    }

    private void resolveNameAndNode(Container c, DockerContainer.Builder dcb) {
        String nodeName = getNode();//it not null only for node services
        String containerName = null;
        {
            CharSequence nameseq = null;
            for (String name : c.getNames()) {
                // name is start from slash, so we need skip first occurrence
                int slashPos = name.indexOf("/", 1);
                if (slashPos > 0) {
                    if (nodeName == null) {
                        //TODO: fix names=[/postgresql, /jira/postgres] case
                        nodeName = name.substring(1, slashPos);
                    }
                    name = name.substring(slashPos + 1);
                } else {
                    // because name start with '/'
                    name = name.substring(1);
                }
                if (nameseq == null) {
                    nameseq = name;
                } else if (nameseq.equals(name)) {
                    //do nothing, its equals
                } else {
                    // we must join all names into single string
                    StringBuilder sb;
                    if (!(nameseq instanceof StringBuilder)) {
                        sb = new StringBuilder(nameseq);
                        nameseq = sb;
                    } else {
                        sb = (StringBuilder) nameseq;
                    }
                    sb.append(", ").append(name);
                }
            }

            if (nameseq != null) {
                containerName = nameseq.toString();
            }
        }

        dcb.setName(containerName);
        Assert.notNull(nodeName, "Can not resolve node name for: " + c);
        dcb.setNode(nodeName);
    }

    @Override
    public ContainerDetails getContainer(String id) {
        Assert.notNull(id, "id is null");
        return getOrNullAction(getUrlContainer(id, SUFF_JSON), ContainerDetails.class);
    }

    @Override
    public ServiceCallResult getStatistics(GetStatisticsArg arg) {
        Assert.notNull(arg.getId(), "id is null");
        URI url = getUrlContainer(arg.getId(), "stats").queryParam("stream", arg.isStream()).build().toUri();
        ServiceCallResult callResult = new ServiceCallResult();
        try {
            ListenableFuture<Object> future = restTemplate.execute(url, HttpMethod.GET, null, response -> {
                StreamContext<Statistics> context = new StreamContext<>(response.getBody(), arg.getWatcher());
                context.getInterrupter().setFuture(arg.getInterrupter());
                statisticsProcessor.processResponseStream(context);
                return null;
            });
            waitFuture(callResult, future);
        } catch (HttpStatusCodeException e) {
            processStatusCodeException(e, callResult, url);
        }
        return callResult;
    }

    private void waitFuture(ServiceCallResult callResult, ListenableFuture<Object> future) {
        //wait response
        try {
            // we need call get in any way, else response extractor will newer called
            // also, we can not use timeout here, because it must wait until client disconnect or interruption.
            future.get();
            online();
            callResult.setCode(ResultCode.OK);
        } catch (InterruptedException e) {
            callResult.setCode(ResultCode.ERROR);
            callResult.setMessage("Interrupted");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            checkOffline(cause);
            if (cause instanceof HttpStatusCodeException) {
                throw (HttpStatusCodeException)cause;
            } else {
                throw Throwables.asRuntime(cause);
            }
        }
    }

    @Override
    public DockerServiceInfo getInfo() {
        DockerServiceInfo dsi = infoCache.get();
        Assert.notNull(dsi, "info is null");
        return dsi;
    }

    private DockerServiceInfo getInfoForCache() {
        Info info = getFast(() -> restTemplate.getForEntity(makeBaseUrl().path("/info").build().toUri(), Info.class)).getBody();
        DockerServiceInfo.Builder dib = DockerInfoParser.parse(info);
        ListIterator<NodeInfo> i = dib.getNodeList().listIterator();
        while (i.hasNext()) {
            NodeInfo dockerNode = i.next();
            //we replace node if it present in services (this mean that it register with our agent and provide additional info)
            NodeInfo agentNode = nodeInfoProvider.getNodeInfo(dockerNode.getName());
            // we place merged health in agent node
            if (agentNode != null) {
                NodeMetrics resultHeath = NodeMetrics.builder()
                  .from(agentNode.getHealth())
                  .fromNonNull(dockerNode.getHealth())
                  .build();
                agentNode = NodeInfoImpl.builder().from(agentNode).health(resultHeath).build();
                i.set(agentNode);
            }
        }
        dib.offNodeCount(0);
        if(this.infoInterceptor != null) {
            this.infoInterceptor.accept(dib);
        }
        DockerServiceInfo serviceInfo = dib.build();
        // it reduce count of identical messages
        if(!Objects.equals(this.oldInfo, serviceInfo)) {
            fireEvent(DockerServiceEvent.onServiceInfo(this, serviceInfo));
        }
        this.oldInfo = serviceInfo;
        return serviceInfo;
    }

    private void fireEvent(DockerServiceEvent dockerServiceEvent) {
        eventConsumer.accept(dockerServiceEvent);
    }

    @Override
    public ServiceCallResult startContainer(String id) {
        return simpleContainerAction(id, "start");
    }

    @Override
    public ServiceCallResult pauseContainer(String id) {
        return simpleContainerAction(id, "pause");
    }

    @Override
    public ServiceCallResult unpauseContainer(String id) {
        return simpleContainerAction(id, "unpause");
    }

    private ServiceCallResult simpleContainerAction(String id, String op) {
        Assert.notNull(id, "id is null");
        log.info("trying to '{}' container {}", op, id);
        UriComponentsBuilder ucb = getUrlContainer(id, op);
        return postAction(ucb, null);
    }

    private ServiceCallResult postAction(UriComponentsBuilder ub, Object cmd) {
        return postAction(ub, cmd, ServiceCallResult.class, null);
    }

    private <T extends ServiceCallResult> T postAction(UriComponentsBuilder ub, Object cmd, Class<T> responseType, Supplier<T> factory) {
        if(factory == null) {
            factory = () -> BeanUtils.instantiate(responseType);
        }
        URI url = ub.build().toUri();
        T resp;
        try {
            ResponseEntity<T> entity = getSlow(() -> {
                HttpEntity<?> req = null;
                if(cmd != null) {
                    req = wrapEntity(cmd);
                }
                return restTemplate.postForEntity(url, req, responseType);
            });
            resp = entity.getBody();
            if(resp == null) {
                resp = factory.get();
            }
            resp.setCode(ResultCode.OK);
            resp.setStatus(entity.getStatusCode());
            return resp;
        } catch (HttpStatusCodeException e) {
            resp = factory.get();
            processStatusCodeException(e, resp, url);
            return resp;
        }
    }

    private <T> T postOrNullAction(UriComponentsBuilder ub, Object cmd, Class<T> responseType) {
        String url = ub.toUriString();
        try {
            ResponseEntity<T> entity = getSlow(() -> {
                HttpEntity<?> req = null;
                if(cmd != null) {
                    req = wrapEntity(cmd);
                }
                return restTemplate.postForEntity(url, req, responseType);
            });
            return entity.getBody();
        } catch (HttpStatusCodeException e) {
            log.warn("Failed to execute POST on {}, due to {}", url, formatHttpException(e));
            if(e.getStatusCode().is4xxClientError()) {
                return null;
            }
            throw e;
        }
    }

    private <T extends ServiceCallResult> T getAction(UriComponentsBuilder ub, Class<T> responseType, Supplier<T> factory) {
        if(factory == null) {
            factory = () -> BeanUtils.instantiate(responseType);
        }
        URI uri = ub.build().toUri();
        T resp;
        try {
            ResponseEntity<T> entity = getFast(() -> {
                return restTemplate.getForEntity(uri, responseType);
            });
            resp = entity.getBody();
            if(resp == null) {
                resp = factory.get();
            }
            resp.setCode(ResultCode.OK);
            return resp;
        } catch (HttpStatusCodeException e) {
            log.warn("Failed to execute GET on {}, due to {}", uri, e.toString());
            resp = factory.get();
            processStatusCodeException(e, resp, uri);
            return resp;
        }
    }

    private <T> T getOrNullAction(UriComponentsBuilder ub, Class<T> responseType) {
        String url = ub.toUriString();
        try {
            ResponseEntity<T> entity = getFast(() -> {
                return restTemplate.getForEntity(url, responseType);
            });
            return entity.getBody();
        } catch (HttpMessageNotReadableException e) {
            //sometime we can receive incorrect JSON data with correct HTTP Code and content type
            log.error("On GET container '{}' we got error: {}", url, e.getMessage());
            return null;
        } catch (HttpStatusCodeException e) {
            log.warn("Failed to execute GET on {}, due to {}", url, formatHttpException(e));
            if(e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    private ServiceCallResult deleteAction(UriComponentsBuilder ub) {
        URI url = ub.build().toUri();
        try {
            ResponseEntity<String> res = getSlow(() -> {
                return restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
            });
            return DockerUtils.getServiceCallResult(res);
        } catch (HttpStatusCodeException e) {
            ServiceCallResult callResult = new ServiceCallResult();
            processStatusCodeException(e, callResult, url);
            return callResult;
        }
    }

    //containers/4fa6e0f0c678/logs?stderr=1&stdout=1&timestamps=1&follow=1&tail=10&since=1428990821
    @Override
    public ServiceCallResult getContainerLog(GetLogContainerArg arg) {

        ServiceCallResult callResult = new ServiceCallResult();

        final Consumer<ProcessEvent> watcher = firstNonNull(arg.getWatcher(), Consumers.<ProcessEvent>nop());
        boolean stderr = arg.isStderr();
        boolean stdout = arg.isStdout();
        if (!stderr && !stdout) {
            // we need at least one stream (but usually need both )
            stderr = stdout = true;
        }
        URI url = getUrlContainer(arg.getId(), "logs")
                .queryParam("stderr", stderr)
                .queryParam("stdout", stdout)
                .queryParam("follow", arg.isFollow())
                .queryParam("since", arg.getSince())
                .queryParam("tail", arg.getTail())
                .queryParam("timestamps", arg.isTimestamps()).build().toUri();
        try {
            ListenableFuture<Object> future = restTemplate.execute(url, HttpMethod.GET, null, response -> {
                StreamContext<ProcessEvent> context = new StreamContext<>(response.getBody(), watcher);
                context.getInterrupter().setFuture(arg.getInterrupter());
                frameStreamProcessor.processResponseStream(context);
                return null;
            });
            waitFuture(callResult, future);
        } catch (HttpStatusCodeException e) {
            processStatusCodeException(e, callResult, url);
        }
        return callResult;
    }

    @Override
    public ServiceCallResult stopContainer(StopContainerArg arg) {
        return stopBasedAction(arg.getId(), arg, "stop");
    }

    @Override
    public ServiceCallResult subscribeToEvents(GetEventsArg arg) {
        ServiceCallResult callResult = new ServiceCallResult();
        UriComponentsBuilder ucb = makeUrl("events");
        if(arg.getSince() != null) {
            ucb.queryParam("since", arg.getSince());
        }
        if(arg.getUntil() != null) {
            ucb.queryParam("until", arg.getUntil());
        }
        URI uri = ucb.build().toUri();
        try {
            ListenableFuture<Object> future = restTemplate.execute(uri, HttpMethod.GET, null, response -> {
                online();// may be we need schedule it into another thread
                StreamContext<DockerEvent> context = new StreamContext<>(response.getBody(), arg.getWatcher());
                context.getInterrupter().setFuture(arg.getInterrupter());
                eventStreamProcessor.processResponseStream(context);
                return null;
            });
            waitFuture(callResult, future);
        } catch (HttpStatusCodeException e) {
            processStatusCodeException(e, callResult, uri);
        }
        return callResult;
    }

    private void processStatusCodeException(HttpStatusCodeException e, ServiceCallResult res, URI uri) {
        setCode(e.getStatusCode(), res);
        String msg = null;
        try {
            if(MediaType.APPLICATION_JSON.includes(e.getResponseHeaders().getContentType())) {
                objectMapper.reader().withValueToUpdate(res).readValue(e.getResponseBodyAsString());
                msg = res.getMessage();
            }
        } catch (Exception ex) {
            log.error("Can not process status code exception message.", ex);
        }
        // in some cases msg may be null on success reading value, but anyway we need to make non null value manually
        if(msg == null) {
            msg = formatHttpException(e);
            res.setMessage(msg);
        }
        // we log message as debug because consumer code must log error too, but with high level,
        // when we log it as warn then error will cause to many duplicate lines in log
        log.warn("Fail to execute '{}' due to error: {}", uri, msg);
    }

    private String formatHttpException(HttpStatusCodeException e) {
        try {
            return MessageFormat.format("Response from server: {0} {1}\n {2}",
              e.getStatusCode().value(),
              e.getStatusText(),// getResponseBodyAsString - below
              org.springframework.util.StringUtils.trimWhitespace(e.getResponseBodyAsString()));
        } catch (Exception ex) {
            log.error("Can not format exception {}", e, ex);
        }
        return e.getStatusText();
    }

    @Override
    public ServiceCallResult restartContainer(StopContainerArg arg) {
        return stopBasedAction(arg.getId(), arg, RESTART);
    }

    @Override
    public CreateContainerResponse createContainer(CreateContainerCmd cmd) {
        UriComponentsBuilder ub = makeUrl("/containers/create").queryParam("name", cmd.getName());
        return postAction(ub, cmd, CreateContainerResponse.class, null);
    }

    private <T> HttpEntity<T> wrapEntity(T cmd) {
        HttpHeaders headers = new HttpHeaders();
        installContentType(headers);
        return new HttpEntity<>(cmd, headers);
    }

    private void installContentType(HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    public ServiceCallResult updateContainer(UpdateContainerCmd cmd) {
        String id = cmd.getId();
        Assert.hasText(id, "id is null or empty");
        UriComponentsBuilder ub = getUrlContainer(id, "update");
        URI uri = ub.build().toUri();
        try {
            ResponseEntity<UpdateContainerResponse> res = getSlow(() -> restTemplate.exchange(uri, HttpMethod.POST,
                  wrapEntity(cmd), UpdateContainerResponse.class));
            UpdateContainerResponse body = res.getBody();
            ServiceCallResult scr = DockerUtils.getServiceCallResult(res);
            if(body != null) {
                String msg = null;
                List<String> warnings = body.getWarnings();
                if(warnings != null) {
                    msg = Joiner.on(", ").join(warnings);
                }
                scr.setMessage(msg);
            }
            return scr;
        } catch (HttpStatusCodeException e) {
            ServiceCallResult res = new ServiceCallResult();
            processStatusCodeException(e, res, uri);
            return res;
        }
    }

    @Override
    public ServiceCallResult renameContainer(String id, String newName) {
        UriComponentsBuilder ub = getUrlContainer(id, "rename").queryParam("name", newName);
        return postAction(ub, null);
    }

    @Override
    public CreateNetworkResponse createNetwork(CreateNetworkCmd createNetworkCmd) {
        UriComponentsBuilder ub = makeBaseUrl().pathSegment("networks", "create");
        return postAction(ub, createNetworkCmd, CreateNetworkResponse.class, null);
    }

    @Override
    public Network getNetwork(String id) {
        return getOrNullAction(makeBaseUrl().pathSegment("networks", id), Network.class);
    }

    @Override
    public ServiceCallResult deleteNetwork(String id) {
        UriComponentsBuilder url = makeBaseUrl().pathSegment("networks", id);
        return deleteAction(url);
    }

    @Override
    public PruneNetworksResponse pruneNetworks(PruneNetworksArg arg) {
        UriComponentsBuilder ub = makeBaseUrl().pathSegment("networks", "prune");
        if(!arg.getFilters().isEmpty()) {
            ub.queryParam("filters", toJson(arg.getFilters()));
        }
        return postAction(ub, null, PruneNetworksResponse.class, null);
    }


    @Override
    public ServiceCallResult connectNetwork(ConnectNetworkCmd cmd) {
        UriComponentsBuilder ub = makeBaseUrl().pathSegment("networks", cmd.getNetwork(), "connect");
        return postAction(ub, cmd);
    }

    @Override
    public ServiceCallResult disconnectNetwork(DisconnectNetworkCmd cmd) {
        UriComponentsBuilder ub = makeBaseUrl().pathSegment("networks", cmd.getNetwork(), "disconnect");
        return postAction(ub, cmd);
    }

    private String toJson(Object obj) {
        if(obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Can not serialize to json.", e);
        }
    }

    @Override
    public List<Network> getNetworks() {
        Network[] body = getOrNullAction(makeBaseUrl().pathSegment("networks"), Network[].class);
        if (body != null) {
            return Arrays.asList(body);
        }
        return Collections.emptyList();
    }

    private ServiceCallResult stopBasedAction(String id, StopContainerArg arg, String action) {
        Assert.notNull(id, "id is null");
        UriComponentsBuilder ub = getUrlContainer(id, action);
        int time = arg.getTimeBeforeKill();
        if (time > 0) {
            ub.queryParam("t", time);
        }
        URI uri = ub.build().toUri();
        try {
            ResponseEntity<String> res = getSlow(() -> restTemplate.postForEntity(uri, null, String.class));
            return DockerUtils.getServiceCallResult(res);
        } catch (HttpStatusCodeException e) {
            log.warn("In {}, can't \"{}\" container: {}, code: {}, message: {}", getId(), action, id, e.getStatusCode(), e.getResponseBodyAsString());
            ServiceCallResult callResult = new ServiceCallResult();
            processStatusCodeException(e, callResult, uri);
            return callResult;
        }
    }

    @Override
    public ServiceCallResult killContainer(KillContainerArg arg) {
        Assert.notNull(arg.getId(), "id is null");
        UriComponentsBuilder ub = getUrlContainer(arg.getId(), "kill");
        KillContainerArg.Signal signal = arg.getSignal();
        if (signal != null) {
            ub.queryParam("signal", signal);
        }
        return postAction(ub, null);

    }

    @Override
    public RemoveImageResult removeImage(RemoveImageArg arg) {
        RemoveImageResult rir = new RemoveImageResult();
        rir.setImage(arg.getImageId());
        UriComponentsBuilder builder = makeUrl("images/" + arg.getImageId())
                .queryParam("force", arg.getForce())
                .queryParam("noprune", arg.getNoPrune());
        URI uri = builder.build().toUri();
        try {
            ResponseEntity<String> res = getSlow(() -> restTemplate.exchange(uri, HttpMethod.DELETE, null, String.class));
            log.info("image was deleted {}", arg);
            return DockerUtils.getServiceCallResult(res, rir);
        } catch (HttpStatusCodeException e) {
            processStatusCodeException(e, rir, uri);
            return rir;
        }
    }

    @Override
    public ServiceCallResult deleteContainer(DeleteContainerArg arg) {
        Assert.notNull(arg.getId(), "id is null");
        UriComponentsBuilder ub = getUrlContainer(arg.getId(), null);
        if (arg.isDeleteVolumes()) {
            ub.queryParam("v", "1");
        }
        if (arg.isKill()) {
            ub.queryParam("force", "1");
        }
        ServiceCallResult res = deleteAction(ub);
        if(res.getCode() != ResultCode.OK) {
            log.error("can't delete container: {} {}", arg, res.getMessage());
        }
        return res;
    }

    @Override
    public ImageDescriptor pullImage(String name, final Consumer<ProcessEvent> watcher) {
        ProcessEvent.watch(watcher, "puling image {0}", name);
        HttpAuthInterceptor.setCurrentName(ContainerUtils.getRegistryPrefix(name));
        URI uri = makeUrl("images/create").queryParam("fromImage", name).build().toUri();
        Supplier<Future<Object>> puller = () -> restTemplate.execute(uri, HttpMethod.POST, null, response -> {
            try (Reader r = new InputStreamReader(response.getBody(), StandardCharsets.UTF_8)) {
                CharBuffer b = CharBuffer.allocate(1024);
                while (r.read(b) >= 0) {
                    b.flip();
                    ProcessEvent.watch(watcher, b.toString());
                    b.clear();
                }
            }
            return null;
        });
        getSlow(puller);//wait while image begin pulled from repo
        ProcessEvent.watch(watcher, "trying to get image info {0}", name);
        ImageDescriptor image = getImage(name);
        ProcessEvent.watch(watcher, "image info fetched {0}", image);
        return image;
    }

    @Override
    public ImageDescriptor getImage(String name) {
        UriComponentsBuilder ucb = makeUrl("images/" + name + "/" + SUFF_JSON);
        Image image = getOrNullAction(ucb, Image.class);
        if(image == null) {
            return null;
        }
        // Image - is docker DTO, it structure randomly changed by docker developers, so we must not to
        //    publish in out from DcokerService
        ContainerConfig cc = image.getContainerConfig();
        return ImageDescriptorImpl.builder()
          .id(image.getId())
          .containerConfig(cc)
          .created(image.getCreated())
          .labels(cc.getLabels())
          .build();
    }

    @Override
    public String getAddress() {
        String address = clusterConfig.getHost();
        Assert.hasText(address, "host in config has null or empty address");
        return address;
    }

    private UriComponentsBuilder makeUrl(String part) {
        return makeBaseUrl().path("/" + part);
    }

    private UriComponentsBuilder makeBaseUrl() {
        try {
            UriComponentsBuilder ucb = newInstance();
            ucb.uri(this.uri);
            return ucb;
        } catch (Exception e) {
            log.error("error during creating rest request to docker " + clusterConfig.toString(), e);
            throw Throwables.asRuntime(e);
        }
    }

    @Override
    public ClusterConfig getClusterConfig() {
        return clusterConfig;
    }

    private UriComponentsBuilder getUrlContainer(String id, String suffix) {
        if (id.contains("/")) {
            throw new IllegalArgumentException("Bad id format: '" + id + "'");
        }
        StringBuilder sb = new StringBuilder("containers/");
        sb.append(id);
        if (suffix != null) {
            sb.append("/").append(suffix);
        }
        return makeUrl(sb.toString());
    }

    @Override
    public SwarmInspectResponse getSwarm() {
        return getOrNullAction(makeBaseUrl().path("swarm"), SwarmInspectResponse.class);
    }

    @Override
    public SwarmInitResult initSwarm(SwarmInitCmd cmd) {
        Assert.notNull(cmd, "cmd is null");
        URI uri = makeUrl("/swarm/init").build().toUri();
        SwarmInitResult res = new SwarmInitResult();
        try {
            ResponseEntity<String> e = getSlow(() -> restTemplate.postForEntity(uri, wrapEntity(cmd), String.class));
            res.setNodeId(e.getBody());
            res.code(ResultCode.OK);
        } catch (HttpStatusCodeException e) {
            log.error("can't init swarm, result: {} \n cmd:{}", cmd, e);
            processStatusCodeException(e, res, uri);
        }
        return res;
    }

    @Override
    public ServiceCallResult joinSwarm(SwarmJoinCmd cmd) {
        Assert.notNull(cmd, "cmd is null");
        return postAction(makeUrl("/swarm/join"), cmd);
    }

    @Override
    public ServiceCallResult leaveSwarm(SwarmLeaveArg arg) {
        Assert.notNull(arg, "arg is null");
        UriComponentsBuilder ucb = makeUrl("/swarm/leave");
        Boolean force = arg.getForce();
        if(force != null) {
            ucb.queryParam("force", force.toString());
        }
        return postAction(ucb, null);
    }

    @Override
    public List<SwarmNode> getNodes(GetNodesArg cmd) {
        SwarmNode[] nodes = getOrNullAction(makeUrl("/nodes"), SwarmNode[].class);
        if(nodes == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(nodes);
    }

    @Override
    public ServiceCallResult removeNode(RemoveNodeArg arg) {
        Assert.notNull(arg, "arg is null");
        UriComponentsBuilder ucb = makeUrl("/nodes/").path(arg.getNodeId());
        Boolean force = arg.getForce();
        if(force != null) {
            ucb.queryParam("force", force.toString());
        }
        return deleteAction(ucb);
    }

    @Override
    public ServiceCallResult updateNode(UpdateNodeCmd cmd) {
        Assert.notNull(cmd, "cmd is null");
        String nodeId = cmd.getNodeId();
        Assert.hasText(nodeId, "nodeId is null");
        UriComponentsBuilder ucb = makeUrl("/nodes/").path(nodeId).path("/update");
        ucb.queryParam("version", cmd.getVersion());
        return postAction(ucb, cmd);
    }



    @Override
    public List<Service> getServices(GetServicesArg arg) {
        Assert.notNull(arg, "arg is null");
        Service[] services = getOrNullAction(makeUrl("/services"), Service[].class);
        if(services == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(services);
    }

    @Override
    public Service getService(String service) {
        Assert.hasText(service, "service is null or empty");
        UriComponentsBuilder ucb = makeUrl("/services/").path(service);
        return getOrNullAction(ucb, Service.class);
    }

    @Override
    public ServiceCreateResult createService(CreateServiceArg arg) {
        Assert.notNull(arg, "arg is null");
        URI uri = makeUrl("/services/create").build().toUri();
        try {
            HttpHeaders headers = new HttpHeaders();
            installContentType(headers);
            AuthConfig.install(headers, arg.getRegistryAuth());
            HttpEntity<Service.ServiceSpec> req = new HttpEntity<>(arg.getSpec(), headers);
            ResponseEntity<ServiceCreateResult> entity = getSlow(() -> restTemplate.postForEntity(uri, req, ServiceCreateResult.class));
            ServiceCreateResult res = entity.getBody();
            res.setCode(ResultCode.OK);
            return res;
        } catch (HttpStatusCodeException e) {
            ServiceCreateResult res = new ServiceCreateResult();
            processStatusCodeException(e, res, uri);
            log.error("can't create service, result: {} \n arg:{}", res.getMessage(), arg, e);
            return res;
        }
    }

    @Override
    public ServiceUpdateResult updateService(UpdateServiceArg arg) {
        Assert.notNull(arg, "arg is null");
        UriComponentsBuilder ucb = makeUrl("/services/");
        String service = arg.getService();
        Assert.hasText(service, "arg.service is null or empty");
        ucb.path(service);
        ucb.path("/update");
        ucb.queryParam("version", arg.getVersion());
        URI uri = ucb.build().toUri();
        try {
            HttpHeaders headers = new HttpHeaders();
            installContentType(headers);
            AuthConfig.install(headers, arg.getRegistryAuth());
            HttpEntity<Service.ServiceSpec> req = new HttpEntity<>(arg.getSpec(), headers);
            ResponseEntity<ServiceUpdateResult> entity = getSlow(() -> {
                return restTemplate.postForEntity(uri, req, ServiceUpdateResult.class);
            });
            ServiceUpdateResult res = entity.getBody();
            res.setCode(ResultCode.OK);
            return res;
        } catch (HttpStatusCodeException e) {
            ServiceUpdateResult res = new ServiceUpdateResult();
            processStatusCodeException(e, res, uri);
            log.error("can't create service, result: {} \n arg:{}", res.getMessage(), arg, e);
            return res;
        }
    }

    @Override
    public ServiceCallResult deleteService(String service) {
        Assert.hasText(service, "service is null or empty");
        UriComponentsBuilder ucb = makeUrl("/services/").path(service);
        return deleteAction(ucb);
    }


    @Override
    public List<Task> getTasks(GetTasksArg arg) {
        Task[] tasks = getOrNullAction(makeUrl("/tasks"), Task[].class);
        if(tasks == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(tasks);
    }

    @Override
    public Task getTask(String taskId) {
        Assert.hasText(taskId, "task is null or empty");
        UriComponentsBuilder ucb = makeUrl("/tasks/").path(taskId);
        return getOrNullAction(ucb, Task.class);
    }

    @Override
    public List<Volume> getVolumes(GetVolumesArg arg) {
        UriComponentsBuilder ucb = makeBaseUrl().path("volumes");
        if(arg.getFilters() != null) {
            ucb.queryParam("filters", toJson(arg.getFilters()));
        }
        GetVolumesResponse res = getOrNullAction(ucb, GetVolumesResponse.class);
        List<Volume> volumes = null;
        if(res != null) {
            volumes = res.getVolumes();
        }
        if(volumes == null) {
            volumes = Collections.emptyList();
        }
        return volumes;
    }

    @Override
    public Volume createVolume(CreateVolumeCmd cmd) {
        return postOrNullAction(makeBaseUrl().path("volumes/create"), cmd, Volume.class);
    }

    @Override
    public ServiceCallResult removeVolume(RemoveVolumeArg arg) {
        UriComponentsBuilder ucb = makeBaseUrl().path("volumes/");
        ucb.path(arg.getName());
        Boolean force = arg.getForce();
        if(force != null) {
            ucb.queryParam("force", force);
        }
        return deleteAction(ucb);
    }

    @Override
    public ServiceCallResult deleteUnusedVolumes(DeleteUnusedVolumesArg arg) {
        UriComponentsBuilder ucb = makeBaseUrl().path("volumes/prune");
        if(arg.getFilters() != null) {
            ucb.queryParam("filters", toJson(arg.getFilters()));
        }
        return postAction(ucb, null);
    }

    @Override
    public Volume getVolume(String name) {
        return getOrNullAction(makeBaseUrl().path("volumes/").path(name), Volume.class);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + getClusterConfig().getHost() + "}";
    }
}
