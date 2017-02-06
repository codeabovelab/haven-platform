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

import com.codeabovelab.dm.cluman.cluster.docker.management.result.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.*;
import com.codeabovelab.dm.cluman.model.Port;
import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfig;
import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfigImpl;
import com.codeabovelab.dm.cluman.cluster.docker.HttpAuthInterceptor;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.*;
import com.codeabovelab.dm.cluman.cluster.docker.model.*;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.common.utils.Consumers;
import com.codeabovelab.dm.common.utils.SingleValueCache;
import com.codeabovelab.dm.common.utils.StringUtils;
import com.codeabovelab.dm.common.utils.Throwables;
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
    private final String node;
    private final String cluster;
    private final String id;
    private final long maxTimeout;

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

        this.maxTimeout = Math.max(TimeUnit.SECONDS.toMillis(clusterConfig.getDockerTimeout()), FAST_TIMEOUT * 10);
        this.infoCache = SingleValueCache.builder(this::getInfoForCache)
                .timeAfterWrite(TimeUnit.SECONDS, this.clusterConfig.getCacheTimeAfterWrite())
                .build();
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
        if(updated && old == null) {
            fireEvent(new DockerServiceEvent(this, StandardActions.OFFLINE));
        }
    }

    @Override
    public List<ImageItem> getImages(GetImagesArg arg) {
        try {
            UriComponentsBuilder builder = makeUrl("images/" + SUFF_JSON);
            builder.queryParam("all", arg.isAll() ? "1" : "0");
            // 'filters' have too complex format, we need to implement high level filtering api for it
            // filters – a JSON encoded value of the filters (a map[string][]string) to process on the images list. Available filters:
            //   dangling=true
            //   label=key or label="key=value" of an image label
            //builder.queryParam("filters", arg.getFilters());

            //filter - support only full image name with repo, not mask or substring
            builder.queryParam("filter", arg.getName());
            ResponseEntity<ImageItem[]> entity = getFast(() -> restTemplate.getForEntity(builder.toUriString(), ImageItem[].class));
            return Arrays.asList(entity.getBody());
        } catch (HttpClientErrorException e) {
            processStatusCodeException(e, new ServiceCallResult());
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
        try {
            UriComponentsBuilder ub = makeUrl("images/")
                    .path(ContainerUtils.buildImageName(cmd.getRepository(), cmd.getImageName(), cmd.getCurrentTag())).path("/tag");
            ResponseEntity<String> res = getSlow(() -> restTemplate.exchange(ub
                            .queryParam("force", cmd.getForce())
                            .queryParam("repo", cmd.getRepository() + "/" + cmd.getImageName())
                            .queryParam("tag", cmd.getNewTag())
                            .toUriString(), HttpMethod.POST,
                    null, String.class));
            if (Boolean.TRUE.equals(cmd.getRemote())) {
                HttpAuthInterceptor.setCurrentName(cmd.getRepository());
                restTemplate.exchange(makeUrl("images/").path(ContainerUtils.buildImageName(cmd.getRepository(), cmd.getImageName(), null))
                        .path("/push").queryParam("tag", cmd.getNewTag())
                        .toUriString(), HttpMethod.POST, null, String.class);
            }
            return DockerUtils.getServiceCallResult(res);
        } catch (HttpStatusCodeException e) {
            ServiceCallResult res = new ServiceCallResult();
            processStatusCodeException(e, res);
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
        try {
            ResponseEntity<ContainerDetails> containers = getFast(() -> restTemplate.getForEntity(getUrlContainer(id, SUFF_JSON).toUriString(), ContainerDetails.class));
            return containers.getBody();
        } catch (HttpMessageNotReadableException e) {
            //with container was removed,request can receive incorrect JSON data with correct HTTP Code and content type
            log.error("On get container '{}' we got error: {}", id, e.getMessage());
            return null;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public ServiceCallResult getStatistics(GetStatisticsArg arg) {
        Assert.notNull(arg.getId(), "id is null");
        String url = getUrlContainer(arg.getId(), "stats").queryParam("stream", arg.isStream()).toUriString();
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
            processStatusCodeException(e, callResult);
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
                processStatusCodeException((HttpStatusCodeException) cause, callResult);
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
        dib.offContainers(0);
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
        String url = ub.toUriString();
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
            return resp;
        } catch (HttpStatusCodeException e) {
            log.warn("Failed to execute POST on {}, due to {}", url, e.toString());
            resp = factory.get();
            processStatusCodeException(e, resp);
            return resp;
        }
    }

    private ServiceCallResult deleteAction(UriComponentsBuilder ub) {
        String url = ub.toUriString();
        try {
            ResponseEntity<String> res = getSlow(() -> {
                return restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
            });
            return DockerUtils.getServiceCallResult(res);
        } catch (HttpStatusCodeException e) {
            log.warn("Failed to execute delete on {}, due to {}", url, e.toString());
            ServiceCallResult callResult = new ServiceCallResult();
            processStatusCodeException(e, callResult);
            return callResult;
        }
    }

    //containers/4fa6e0f0c678/logs?stderr=1&stdout=1&timestamps=1&follow=1&tail=10&since=1428990821
    @Override
    public ServiceCallResult getContainerLog(GetLogContainerArg arg) {

        ServiceCallResult callResult = new ServiceCallResult();

        final Consumer<ProcessEvent> watcher = firstNonNull(arg.getWatcher(), Consumers.<ProcessEvent>nop());
        try {
            boolean stderr = arg.isStderr();
            boolean stdout = arg.isStdout();
            if (!stderr && !stdout) {
                // we need at least one stream (but usually need both )
                stderr = stdout = true;
            }
            String url = getUrlContainer(arg.getId(), "logs")
                    .queryParam("stderr", stderr)
                    .queryParam("stdout", stdout)
                    .queryParam("follow", arg.isFollow())
                    .queryParam("since", arg.getSince())
                    .queryParam("tail", arg.getTail())
                    .queryParam("timestamps", arg.isTimestamps()).toUriString();
            ListenableFuture<Object> future = restTemplate.execute(url, HttpMethod.GET, null, response -> {
                StreamContext<ProcessEvent> context = new StreamContext<>(response.getBody(), watcher);
                context.getInterrupter().setFuture(arg.getInterrupter());
                frameStreamProcessor.processResponseStream(context);
                return null;
            });
            waitFuture(callResult, future);
        } catch (HttpStatusCodeException e) {
            processStatusCodeException(e, callResult);
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
        try {
            UriComponentsBuilder ucb = makeUrl("events");
            if(arg.getSince() != null) {
                ucb.queryParam("since", arg.getSince());
            }
            if(arg.getUntil() != null) {
                ucb.queryParam("until", arg.getUntil());
            }
            ListenableFuture<Object> future = restTemplate.execute(ucb.build().toUri(), HttpMethod.GET, null, response -> {
                online();// may be we need schedule it into another thread
                StreamContext<DockerEvent> context = new StreamContext<>(response.getBody(), arg.getWatcher());
                context.getInterrupter().setFuture(arg.getInterrupter());
                eventStreamProcessor.processResponseStream(context);
                return null;
            });
            waitFuture(callResult, future);
        } catch (HttpStatusCodeException e) {
            processStatusCodeException(e, callResult);
        }
        return callResult;
    }

    private void processStatusCodeException(HttpStatusCodeException e, ServiceCallResult res) {
        setCode(e.getStatusCode(), res);
        try {
            res.setMessage(MessageFormat.format("Response from server: {0} {1}\n {2}",
                    e.getStatusCode().value(),
                    e.getStatusText(),// getResponseBodyAsString - below
                    org.springframework.util.StringUtils.trimWhitespace(e.getResponseBodyAsString())));
            // we log message as debug because consumer code must log error too, but with high level,
            // when we log it as warn then error will cause to many duplicate lines in log
            log.debug("result: {}", res.getMessage());
        } catch (Exception ex) {
            res.setMessage(e.getStatusText());
            log.error("Can not format exception {}", e, ex);
        }
    }

    @Override
    public ServiceCallResult restartContainer(StopContainerArg arg) {
        return stopBasedAction(arg.getId(), arg, RESTART);
    }

    @Override
    public CreateContainerResponse createContainer(CreateContainerCmd cmd) {
        try {
            UriComponentsBuilder ub = makeUrl("/containers/create").queryParam("name", cmd.getName());
            ResponseEntity<CreateContainerResponse> res = getSlow(() -> restTemplate.exchange(ub.toUriString(), HttpMethod.POST, wrapEntity(cmd), CreateContainerResponse.class));
            CreateContainerResponse response = res.getBody();
            DockerUtils.setCode(res, response);
            return response;
        } catch (HttpStatusCodeException e) {
            CreateContainerResponse res = new CreateContainerResponse();
            processStatusCodeException(e, res);
            return res;
        }
    }

    private <T> HttpEntity<T> wrapEntity(T cmd) {
        HttpHeaders headers = new HttpHeaders();
        installContentType(headers);
        HttpEntity<T> entity = new HttpEntity<>(cmd, headers);
        return entity;
    }

    private void installContentType(HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    public ServiceCallResult updateContainer(UpdateContainerCmd cmd) {
        try {
            UriComponentsBuilder ub = getUrlContainer(cmd.getId(), "update");
            ResponseEntity<UpdateContainerResponse> res = getSlow(() -> restTemplate.exchange(ub.toUriString(), HttpMethod.POST,
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
            processStatusCodeException(e, res);
            return res;
        }
    }

    @Override
    public ServiceCallResult renameContainer(String id, String newName) {
        try {
            UriComponentsBuilder ub = getUrlContainer(id, "rename").queryParam("name", newName);
            ResponseEntity<String> res = getSlow(() -> restTemplate.postForEntity(ub.toUriString(), null, String.class));
            return DockerUtils.getServiceCallResult(res);
        } catch (HttpStatusCodeException e) {
            ServiceCallResult res = new ServiceCallResult();
            processStatusCodeException(e, res);
            return res;
        }
    }

    @Override
    public CreateNetworkResponse createNetwork(CreateNetworkCmd createNetworkCmd) {
        UriComponentsBuilder ub = makeBaseUrl().pathSegment("networks", "create");
        return postAction(ub, createNetworkCmd, CreateNetworkResponse.class, null);
    }

    @Override
    public Network getNetwork(String id) {
        ResponseEntity<Network> res = getFast(() -> restTemplate.getForEntity(makeBaseUrl().pathSegment("networks", id).toUriString(), Network.class));
        return res.getBody();
    }

    @Override
    public ServiceCallResult deleteNetwork(String id) {
        UriComponentsBuilder url = makeBaseUrl().pathSegment("networks", id);
        return deleteAction(url);
    }

    @Override
    public PruneNetworksResponse pruneNetworks(PruneNetworksArg arg) {
        PruneNetworksResponse resp;
        UriComponentsBuilder ub = makeBaseUrl().pathSegment("networks", "prune");
        if(!arg.getFilters().isEmpty()) {
            ub.queryParam("filters", toJson(arg.getFilters()));
        }
        try {
            ResponseEntity<PruneNetworksResponse> res = getSlow(() -> restTemplate.exchange(ub.toUriString(), HttpMethod.POST, null, PruneNetworksResponse.class));
            resp = res.getBody();
            log.info("networks was pruned {}", resp);
            DockerUtils.getServiceCallResult(res, resp);
        } catch (HttpStatusCodeException e) {
            resp = new PruneNetworksResponse();
            processStatusCodeException(e, resp);
            log.error("can't prune networks due to {}", e.getMessage());
        }
        return resp;
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

    private Object toJson(Object obj) {
        // it need to pass ObjectMapper into this class
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public List<Network> getNetworks() {
        ResponseEntity<Network[]> networks = getFast(() -> restTemplate.getForEntity(makeBaseUrl().pathSegment("networks").toUriString(), Network[].class));
        Network[] body = networks.getBody();
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
        try {
            ResponseEntity<String> res = getSlow(() -> restTemplate.postForEntity(ub.toUriString(), null, String.class));
            return DockerUtils.getServiceCallResult(res);
        } catch (HttpStatusCodeException e) {
            log.warn("In {}, can't \"{}\" container: {}, code: {}, message: {}", getId(), action, id, e.getStatusCode(), e.getResponseBodyAsString());
            ServiceCallResult callResult = new ServiceCallResult();
            processStatusCodeException(e, callResult);
            return callResult;
        }
    }

    @Override
    public ServiceCallResult killContainer(KillContainerArg arg) {
        try {
            Assert.notNull(arg.getId(), "id is null");
            UriComponentsBuilder ub = getUrlContainer(arg.getId(), "kill");
            KillContainerArg.Signal signal = arg.getSignal();
            if (signal != null) {
                ub.queryParam("signal", signal);
            }
            ResponseEntity<String> res = getSlow(() -> restTemplate.postForEntity(ub.toUriString(), null, String.class));
            return DockerUtils.getServiceCallResult(res);
        } catch (HttpStatusCodeException e) {
            log.error("can't delete container: " + arg, e);
            ServiceCallResult callResult = new ServiceCallResult();
            processStatusCodeException(e, callResult);
            return callResult;
        }

    }

    @Override
    public RemoveImageResult removeImage(RemoveImageArg arg) {
        RemoveImageResult rir = new RemoveImageResult();
        rir.setImage(arg.getImageId());
        try {
            UriComponentsBuilder builder = makeUrl("images/" + arg.getImageId())
                    .queryParam("force", arg.getForce())
                    .queryParam("noprune", arg.getNoPrune());
            ResponseEntity<String> res = getSlow(() -> restTemplate.exchange(builder.toUriString(), HttpMethod.DELETE, null, String.class));
            log.info("image was deleted {}", arg);
            return DockerUtils.getServiceCallResult(res, rir);
        } catch (HttpStatusCodeException e) {
            processStatusCodeException(e, rir);
            log.error("can't delete image: {} due to {}", arg.getImageId(), rir.getMessage());
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
        Image image;
        URI url = makeUrl("images/" + name + "/" + SUFF_JSON).build().toUri();
        try {
            image = getFast(() -> restTemplate.getForEntity(url, Image.class)).getBody();
            log.info("image info fetched {}", image);
        } catch (HttpStatusCodeException e) {
            if(e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw new RuntimeException("image info can not be fetched due error from url " + url + " reason:" + e.getResponseBodyAsString(), e);
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
            String address = getAddress();
            UriComponentsBuilder ucb = newInstance().scheme("http");
            String arr[] = StringUtils.splitLast(address, ':');
            if(arr == null) {
                return ucb.host(address);
            }
            int port = Integer.parseInt(arr[1]);
            return ucb.host(arr[0]).port(port);
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
        ResponseEntity<SwarmInspectResponse> configResult = getFast(() -> restTemplate.getForEntity(makeUrl("/swarm").toUriString(), SwarmInspectResponse.class));
        return configResult.getBody();
    }

    @Override
    public SwarmInitResult initSwarm(SwarmInitCmd cmd) {
        Assert.notNull(cmd, "cmd is null");
        SwarmInitResult res = new SwarmInitResult();
        try {
            ResponseEntity<String> e = getSlow(() -> {
                return restTemplate.postForEntity(makeUrl("/swarm/init").toUriString(), wrapEntity(cmd), String.class);
            });
            res.setNodeId(e.getBody());
            res.code(ResultCode.OK);
        } catch (HttpStatusCodeException e) {
            log.error("can't init swarm, result: {} \n cmd:{}", cmd, e);
            processStatusCodeException(e, res);
        }
        return res;
    }

    @Override
    public ServiceCallResult joinSwarm(SwarmJoinCmd cmd) {
        Assert.notNull(cmd, "cmd is null");
        ServiceCallResult res = new ServiceCallResult();
        try {
            ResponseEntity<String> e = getSlow(() -> {
                return restTemplate.postForEntity(makeUrl("/swarm/join").toUriString(), wrapEntity(cmd), String.class);
            });
            DockerUtils.getServiceCallResult(e, res);
        } catch (HttpStatusCodeException e) {
            processStatusCodeException(e, res);
            log.error("can't join swarm, result: {} \n cmd:{}", res.getMessage(), cmd, e);
        }
        return res;
    }

    @Override
    public ServiceCallResult leaveSwarm(SwarmLeaveArg arg) {
        Assert.notNull(arg, "arg is null");
        ServiceCallResult res = new ServiceCallResult();
        try {
            UriComponentsBuilder ucb = makeUrl("/swarm/leave");
            Boolean force = arg.getForce();
            if(force != null) {
                ucb.queryParam("force", force.toString());
            }
            ResponseEntity<String> e = getSlow(() -> {
                return restTemplate.postForEntity(ucb.toUriString(), null, String.class);
            });
            DockerUtils.getServiceCallResult(e, res);
        } catch (HttpStatusCodeException e) {
            processStatusCodeException(e, res);
            log.error("can't leave swarm, result: {} \n arg:{}", res.getMessage(), arg, e);
        }
        return res;
    }

    @Override
    public List<SwarmNode> getNodes(GetNodesArg cmd) {
        ResponseEntity<SwarmNode[]> configResult = getFast(() -> restTemplate.getForEntity(makeUrl("/nodes").toUriString(), SwarmNode[].class));
        return Arrays.asList(configResult.getBody());
    }

    @Override
    public ServiceCallResult removeNode(RemoveNodeArg arg) {
        Assert.notNull(arg, "arg is null");
        ServiceCallResult res = new ServiceCallResult();
        try {
            UriComponentsBuilder ucb = makeUrl("/nodes/").path(arg.getNodeId());
            Boolean force = arg.getForce();
            if(force != null) {
                ucb.queryParam("force", force.toString());
            }
            getSlow(() -> {
                return restTemplate.delete(ucb.toUriString());
            });
            res.code(ResultCode.OK);
        } catch (HttpStatusCodeException e) {
            processStatusCodeException(e, res);
            log.error("can't remove node, result: {} \n arg:{}", res.getMessage(), arg, e);
        }
        return res;
    }

    @Override
    public List<Service> getServices(GetServicesArg arg) {
        Assert.notNull(arg, "arg is null");
        ResponseEntity<Service[]> configResult = getFast(() -> restTemplate.getForEntity(makeUrl("/services").toUriString(), Service[].class));
        return Arrays.asList(configResult.getBody());
    }

    @Override
    public Service getService(String service) {
        Assert.hasText(service, "service is null or empty");
        UriComponentsBuilder ucb = makeUrl("/services/").path(service);
        ResponseEntity<Service> configResult = getFast(() -> restTemplate.getForEntity(ucb.toUriString(), Service.class));
        return configResult.getBody();
    }

    @Override
    public ServiceCreateResult createService(CreateServiceArg arg) {
        Assert.notNull(arg, "arg is null");
        try {
            HttpHeaders headers = new HttpHeaders();
            installContentType(headers);
            AuthConfig.install(headers, arg.getRegistryAuth());
            HttpEntity<Service.ServiceSpec> req = new HttpEntity<>(arg.getSpec(), headers);
            ResponseEntity<ServiceCreateResult> entity = getSlow(() -> {
                return restTemplate.postForEntity(makeUrl("/services/create").toUriString(), req, ServiceCreateResult.class);
            });
            ServiceCreateResult res = entity.getBody();
            res.setCode(ResultCode.OK);
            return res;
        } catch (HttpStatusCodeException e) {
            ServiceCreateResult res = new ServiceCreateResult();
            processStatusCodeException(e, res);
            log.error("can't create service, result: {} \n arg:{}", res.getMessage(), arg, e);
            return res;
        }
    }

    @Override
    public ServiceUpdateResult updateService(UpdateServiceArg arg) {
        Assert.notNull(arg, "arg is null");
        try {
            UriComponentsBuilder ucb = makeUrl("/services/");
            String service = arg.getService();
            Assert.hasText(service, "arg.service is null or empty");
            ucb.path(service);
            ucb.path("/update");
            ucb.queryParam("version", arg.getVersion());
            HttpHeaders headers = new HttpHeaders();
            installContentType(headers);
            AuthConfig.install(headers, arg.getRegistryAuth());
            HttpEntity<Service.ServiceSpec> req = new HttpEntity<>(arg.getSpec(), headers);
            ResponseEntity<ServiceUpdateResult> entity = getSlow(() -> {
                return restTemplate.postForEntity(ucb.toUriString(), req, ServiceUpdateResult.class);
            });
            ServiceUpdateResult res = entity.getBody();
            res.setCode(ResultCode.OK);
            return res;
        } catch (HttpStatusCodeException e) {
            ServiceUpdateResult res = new ServiceUpdateResult();
            processStatusCodeException(e, res);
            log.error("can't create service, result: {} \n arg:{}", res.getMessage(), arg, e);
            return res;
        }
    }

    @Override
    public ServiceCallResult deleteService(String service) {
        Assert.hasText(service, "service is null or empty");
        UriComponentsBuilder ucb = makeUrl("/services/").path(service);
        ServiceCallResult res = new ServiceCallResult();
        try {
            getSlow(() -> restTemplate.delete(ucb.toUriString()));
        } catch (HttpStatusCodeException e) {
            processStatusCodeException(e, res);
            log.error("can't delete service '{}', result: {} ", service, res.getMessage(), e);
        }
        return res;
    }


    @Override
    public List<Task> getTasks(GetTasksArg arg) {
        ResponseEntity<Task[]> configResult = getFast(() -> restTemplate.getForEntity(makeUrl("/tasks").toUriString(), Task[].class));
        return Arrays.asList(configResult.getBody());
    }

    @Override
    public Task getTask(String taskId) {
        Assert.hasText(taskId, "task is null or empty");
        UriComponentsBuilder ucb = makeUrl("/tasks/").path(taskId);
        ResponseEntity<Task> configResult = getFast(() -> restTemplate.getForEntity(ucb.toUriString(), Task.class));
        return configResult.getBody();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + getClusterConfig().getHost() + "}";
    }
}
