package com.codeabovelab.dm.cluman;

import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfig;
import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfigImpl;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.*;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.*;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.model.Node;
import com.codeabovelab.dm.common.utils.PojoBeanUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 */
public class DockerServiceMock implements DockerService {

    static final int ID_LEN = 12;
    private final Map<String, ContainerHolder> containers = new HashMap<>();
    private final Map<String, NetworkHolder> networks = new ConcurrentHashMap<>();
    private final Map<String, ImageStub> images = new ConcurrentHashMap<>();
    private final ClusterConfig cc = ClusterConfigImpl.builder().build();
    private final DockerServiceInfo info;
    //we need to make list of nodes
    private final NodeInfo node = NodeInfoImpl.builder().name("test-node").build();
    public DockerServiceMock(DockerServiceInfo info) {
        this.info = info;
    }

    @Override
    public String getAddress() {
        // add default docker port to hostname
        return "127.0.0.1:2375";
    }

    @Override
    public String getCluster() {
        return info.getName();
    }

    @Override
    public String getNode() {
        return null;
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public List<DockerContainer> getContainers(GetContainersArg arg) {
        synchronized (containers) {
            Stream<ContainerHolder> stream = containers.values().stream();
            if(!arg.isAll()) {
                stream = stream.filter(ContainerHolder::isStarted);
            }
            return stream.map(ContainerHolder::asDockerContainer).collect(Collectors.toList());
        }
    }

    @Override
    public ContainerDetails getContainer(String id) {
        if (id == null) {
            return null;
        }
        ContainerHolder holder = getContainerHolder(id);
        if (holder == null) {
            return null;
        }
        ContainerDetails cd = new ContainerDetails();
        DockerContainer source = holder.asDockerContainer();
        BeanUtils.copyProperties(source, cd);
        // wait list of nodes
        cd.setNode(toDockerNode(this.node));
        //TODO clone below
        cd.setHostConfig(holder.getHostConfig());
        cd.setConfig(holder.getConfig());
        return cd;
    }

    private com.codeabovelab.dm.cluman.cluster.docker.model.Node toDockerNode(Node node) {
        return new com.codeabovelab.dm.cluman.cluster.docker.model.Node(node.getAddress(),
          1,
          node.getName(),
          node.getAddress(),
          node.getName(),
          6 * 1024 * 1024,
          new HashMap<>());
    }

    private ContainerHolder getContainerHolder(String id) {
        if(id == null) {
            return null;
        }
        synchronized (containers) {
            ContainerHolder ch = null;
            if (id.length() >= ID_LEN) {
                String sid = id;
                if (id.length() > ID_LEN) {
                    sid = sid.substring(ID_LEN);
                }
                ch = containers.get(sid);
            }
            if (ch == null) {
                ch = getContainerHolderByName(id);
            }
            return ch;
        }
    }

    private ContainerHolder getContainerHolderByName(String name) {
        if(name == null) {
            return null;
        }
        synchronized (containers) {
            Object[] objects = containers.values().stream().filter((c) -> name.equals(c.getName())).toArray();
            if(objects.length == 0) {
                return null;
            }
            if(objects.length > 1) {
                throw new IllegalStateException("Multiple containers with same name: " + Arrays.toString(objects));
            }
            return (ContainerHolder) objects[0];
        }
    }

    @Override
    public ServiceCallResult getStatistics(GetStatisticsArg arg) {
        String id = arg.getId();
        synchronized (containers) {
            ContainerHolder ch = getContainerHolder(id);
            if (ch == null) {
                return null;
            }
        }
        Statistics s = new Statistics();
        s.setRead(DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()));
        s.setBlkioStats(ImmutableMap.of());
        s.setCpuStats(ImmutableMap.of());
        s.setMemoryStats(ImmutableMap.of());
        s.setNetworks(ImmutableMap.of());
        arg.getWatcher().accept(s);
        return resultOk();
    }

    @Override
    public DockerServiceInfo getInfo() {
        DockerServiceInfo.Builder b = DockerServiceInfo.builder().from(info);
        return b.build();
    }

    @Override
    public ServiceCallResult startContainer(String id) {
        synchronized (containers) {
            ContainerHolder ch = getContainerHolder(id);
            if (ch == null) {
                return resultNotFound();
            }
            ch.start();
            return resultOk();
        }
    }

    @Override
    public ServiceCallResult pauseContainer(String id) {
        synchronized (containers) {
            ContainerHolder ch = getContainerHolder(id);
            if (ch == null) {
                return resultNotFound();
            }
            ch.pause();
            return resultOk();
        }
    }

    @Override
    public ServiceCallResult unpauseContainer(String id) {
        synchronized (containers) {
            ContainerHolder ch = getContainerHolder(id);
            if (ch == null) {
                return resultNotFound();
            }
            ch.unpause();
            return resultOk();
        }
    }

    @Override
    public ServiceCallResult stopContainer(StopContainerArg arg) {
        synchronized (containers) {
            ContainerHolder ch = getContainerHolder(arg.getId());
            if (ch == null) {
                return resultNotFound();
            }
            ch.stop();
            return resultOk();
        }
    }

    @Override
    public ServiceCallResult getContainerLog(GetLogContainerArg arg) {
        synchronized (containers) {
            ContainerHolder ch = getContainerHolder(arg.getId());
            if (ch == null) {
                return resultNotFound();
            }
            return resultOk();
        }
    }

    @Override
    public ServiceCallResult subscribeToEvents(GetEventsArg arg) {
        //TODO
        return resultOk();
    }

    @Override
    public ServiceCallResult restartContainer(StopContainerArg arg) {
        synchronized (containers) {
            ContainerHolder ch = getContainerHolder(arg.getId());
            if (ch == null) {
                return resultNotFound();
            }
            ch.restart();
            return resultOk();
        }
    }

    @Override
    public ServiceCallResult killContainer(KillContainerArg arg) {
        synchronized (containers) {
            ContainerHolder ch = getContainerHolder(arg.getId());
            if (ch == null) {
                return resultNotFound();
            }
            ch.stop();
            return resultOk();
        }
    }

    @Override
    public ServiceCallResult deleteContainer(DeleteContainerArg arg) {
        synchronized (containers) {
            ContainerHolder ch = getContainerHolder(arg.getId());
            if(ch == null) {
                return resultNotFound();
            }
            if(ch.isStarted() && !arg.isKill()) {
                return new ServiceCallResult().code(ResultCode.ERROR).message("TEST Container is started");
            }
            containers.remove(ch.getId());
            return resultOk();
        }
    }

    @Override
    public CreateContainerResponse createContainer(CreateContainerCmd cmd) {
        synchronized (containers) {
            String name = cmd.getName();
            Assert.notNull(name, "name can't be null in " + cmd);
            ContainerHolder ch = getContainerHolderByName(name);
            if(ch != null) {
                CreateContainerResponse r = new CreateContainerResponse();
                r.code(ResultCode.CONFLICT).message("Container with name '" + name + "' already exists.");
                return r;
            }
            String image = cmd.getImage();
            Assert.notNull(image, "image can't be null in " + cmd);
            DockerContainer dc = DockerContainer.builder()
              .id(makeId())
              .name(name)
              .image(image)
              .imageId(image)
              .node(getNode(cmd.getLabels()).getName())
              .build();
            ch = new ContainerHolder(dc);
            ch.setHostConfig(cmd.getHostConfig());
            ContainerConfig.ContainerConfigBuilder cc = ContainerConfig.builder();
            PojoBeanUtils.copyToBuilder(cmd, cc);
            ch.setConfig(cc.build());
            containers.put(ch.getId(), ch);
            CreateContainerResponse r = new CreateContainerResponse();
            r.setId(dc.getId());
            r.setCode(ResultCode.OK);
            return r;
        }
    }

    /**
     * in future node must be choose by some conditions placed in labels
     * @param labels
     * @return
     */
    private Node getNode(Map<String, String> labels) {
        return node;
    }

    private String makeId() {
        synchronized (containers) {
            while(true) {
                byte[] arr = new byte[ID_LEN/2];
                ThreadLocalRandom.current().nextBytes(arr);
                char[] encode = Hex.encode(arr);
                String id = new String(encode);
                // this is unlikely, but if happened we got strange errors, because we check it
                if(!containers.containsKey(id)) {
                    return id;
                }
            }
        }
    }

    @Override
    public ServiceCallResult updateContainer(UpdateContainerCmd cmd) {
        synchronized (containers) {
            ContainerHolder ch = getContainerHolder(cmd.getId());
            if (ch == null) {
                return resultNotFound();
            }
            //TODO
            return resultOk();
        }
    }

    @Override
    public ServiceCallResult renameContainer(String id, String newName) {
        synchronized (containers) {
            ContainerHolder ch = getContainerHolder(id);
            if (ch == null) {
                return resultNotFound();
            }
            ch.setName(newName);
            return resultOk();
        }
    }

    @Override
    public CreateNetworkResponse createNetwork(CreateNetworkCmd cmd) {
        NetworkHolder value = new NetworkHolder(cmd);
        NetworkHolder old = networks.putIfAbsent(cmd.getName(), value);
        CreateNetworkResponse res = new CreateNetworkResponse();
        if(old != null) {
            res.code(ResultCode.CONFLICT);
            return res;
        }
        res.setId(value.getName());
        res.code(ResultCode.OK);
        return res;
    }

    @Override
    public Network getNetwork(String id) {
        NetworkHolder nh = networks.get(id);
        if(nh == null) {
            return null;
        }
        return nh.asNetwork();
    }

    @Override
    public ServiceCallResult deleteNetwork(String id) {
        NetworkHolder nh = networks.remove(id);
        return nh == null? resultNotFound() : resultOk();
    }

    @Override
    public PruneNetworksResponse pruneNetworks(PruneNetworksArg arg) {
        PruneNetworksResponse res = new PruneNetworksResponse();
        return res;
    }

    @Override
    public ServiceCallResult connectNetwork(ConnectNetworkCmd cmd) {
        return resultOk();
    }

    @Override
    public ServiceCallResult disconnectNetwork(DisconnectNetworkCmd cmd) {
        return resultOk();
    }

    @Override
    public List<Network> getNetworks() {
        return networks.values().stream().map(NetworkHolder::asNetwork).collect(Collectors.toList());
    }

    @Override
    public ServiceCallResult createTag(TagImageArg cmd) {
        //TODO
        return resultOk();
    }

    public void defineImage(ImageStub imageStub) {
        images.put(imageStub.getId(), imageStub);
    }

    @Override
    public List<ImageItem> getImages(GetImagesArg arg) {
        return images.values().stream()
          .map(is -> {
              Date created = is.getCreated();
              return ImageItem.builder()
                .id(is.getId())
                .labels(is.getLabels())
                .created(created == null? 0L : created.getTime())
                .repoTags(ImmutableList.copyOf(getTags(is)))
                .build();
          })
          .collect(Collectors.toList());
    }

    private Collection<String> getTags(ImageStub is) {
        //TODO
        return Collections.emptyList();
    }

    @Override
    public ImageDescriptor pullImage(String name, Consumer<ProcessEvent> watcher) {
        return getImage(name);
    }

    @Override
    public ImageDescriptor getImage(String name) {
        ImageStub is = images.get(name);
        if(is == null) {
            is = images.values().stream().filter(i -> i.getName().equals(name)).findFirst().orElse(null);
        }
        return is;
    }

    @Override
    public ClusterConfig getClusterConfig() {
        return cc;
    }

    @Override
    public RemoveImageResult removeImage(RemoveImageArg arg) {
        //TODO
        return new RemoveImageResult();
    }

    private ServiceCallResult resultNotFound() {
        ServiceCallResult r = new ServiceCallResult();
        r.code(ResultCode.NOT_FOUND).message("TEST not found");
        return r;
    }

    private ServiceCallResult resultOk() {
        return new ServiceCallResult().code(ResultCode.OK);
    }

    private ServiceCallResult resultConflict() {
        return new ServiceCallResult().code(ResultCode.CONFLICT);
    }

    private ServiceCallResult resultNotSupported() {
        return new ServiceCallResult().code(ResultCode.ERROR).message("Not supported");
    }

    @Override
    public SwarmInspectResponse getSwarm() {
        return null;
    }

    @Override
    public SwarmInitResult initSwarm(SwarmInitCmd cmd) {
        return null;
    }

    @Override
    public ServiceCallResult joinSwarm(SwarmJoinCmd cmd) {
        return null;
    }

    @Override
    public ServiceCallResult leaveSwarm(SwarmLeaveArg arg) {
        return null;
    }

    @Override
    public List<SwarmNode> getNodes(GetNodesArg cmd) {
        return Collections.emptyList();
    }

    @Override
    public ServiceCallResult removeNode(RemoveNodeArg arg) {
        return resultNotSupported();
    }

    @Override
    public ServiceCallResult updateNode(UpdateNodeCmd cmd) {
        return resultNotSupported();
    }

    @Override
    public List<Service> getServices(GetServicesArg arg) {
        return Collections.emptyList();
    }

    @Override
    public ServiceCreateResult createService(CreateServiceArg arg) {
        return null;
    }

    @Override
    public ServiceUpdateResult updateService(UpdateServiceArg arg) {
        return null;
    }

    @Override
    public ServiceCallResult deleteService(String service) {
        return null;
    }

    @Override
    public Service getService(String service) {
        return null;
    }

    @Override
    public List<Task> getTasks(GetTasksArg arg) {
        return Collections.emptyList();
    }

    @Override
    public Task getTask(String taskId) {
        return null;
    }

    @Override
    public List<Volume> getVolumes(GetVolumesArg arg) {
        return Collections.emptyList();
    }

    @Override
    public Volume createVolume(CreateVolumeCmd cmd) {
        return null;
    }

    @Override
    public ServiceCallResult removeVolume(RemoveVolumeArg arg) {
        return resultOk();
    }

    @Override
    public ServiceCallResult deleteUnusedVolumes(DeleteUnusedVolumesArg arg) {
        return resultOk();
    }

    @Override
    public Volume getVolume(String name) {
        return null;
    }

    private static class ContainerHolder {
        private final DockerContainer container;
        private String name;
        private LocalDateTime started;
        private LocalDateTime stopped;
        private LocalDateTime paused;
        private HostConfig hostConfig;
        private ContainerConfig config;

        ContainerHolder(DockerContainer container) {
            this.container = container;
            this.name = this.container.getName();
        }

        synchronized void start() {
            checkPaused();
            started = LocalDateTime.now();
        }

        private void checkPaused() {
            Assert.isNull(this.paused, "Container is paused");
        }

        synchronized void stop() {
            checkPaused();
            stopped = LocalDateTime.now();
            started = null;
        }

        synchronized boolean isStarted() {
            return started != null && (stopped == null || started.isAfter(stopped));
        }

        DockerContainer asDockerContainer() {
            return container.toBuilder().name(name).build();
        }

        public String getId() {
            return container.getId();
        }

        public String getName() {
            return name;
        }

        public synchronized void setName(String name) {
            this.name = name;
        }

        public synchronized HostConfig getHostConfig() {
            return hostConfig;
        }

        public synchronized void setHostConfig(HostConfig hostConfig) {
            this.hostConfig = hostConfig;
        }

        public synchronized ContainerConfig getConfig() {
            return config;
        }

        public synchronized void setConfig(ContainerConfig config) {
            this.config = config;
        }

        public synchronized void restart() {
            stop();
            start();
        }

        public synchronized void pause() {
            if(this.paused != null) {
                throw new RuntimeException("Container is already paused.");
            }
            this.paused = LocalDateTime.now();
        }

        public synchronized void unpause() {
            if(this.paused == null) {
                throw new RuntimeException("Container is not paused.");
            }
            this.paused = null;
        }


        @Override
        public String toString() {
            return "ContainerHolder{" +
              "name='" + name + '\'' +
              '}';
        }
    }

    private static class NetworkHolder {

        private final String name;
        private final String driver;

        NetworkHolder(CreateNetworkCmd cmd) {
            this.name = cmd.getName();
            this.driver = cmd.getDriver();
        }

        public String getName() {
            return name;
        }

        public String getDriver() {
            return driver;
        }

        Network asNetwork() {
            Network.Builder nb = Network.builder();
            nb.name(name);
            return nb.build();
        }
    }

    @Data
    @Builder(builderClassName = "Builder")
    public static class ImageStub implements ImageDescriptor {
        private final String id;
        private final String name;
        private final Date created;
        private final ContainerConfig containerConfig;
        private final Map<String, String> labels;

        public ContainerConfig getContainerConfig() {
            if(containerConfig != null) {
                return containerConfig;
            }
            return ContainerConfig.builder()
              .env(Collections.emptyList())
              .labels(Collections.emptyMap())
              .build();
        }
    }
}
