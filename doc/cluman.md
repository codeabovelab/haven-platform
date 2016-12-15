# Cluman #

**Documentation is still very much a work-in-progress**

Cluman is the server component of the Haven project. This components manages the clusters via communication
with the agent. The name comes from CLUster-MANager.

## What is Cluman? ##

* Cluman manages the following modules (data is stored in `etcd` (run `etcdctl ls /cluman/` for checking)):
    * `/nodes` - list of connected nodes
    * `/clusters` - list of clusters (currently only `RealCluster` stored here)
    * `/containers` - list of containers
    * `/applications` - list of compose applications - applications are made of one or more containers instantiated by a compose file 
    * `/docker-registry` - list of user-added registries and Docker Hub
    * `/pipelines` - list of pipelines (To be implemented)
* It gathers the metric information from the nodes (part of info provided by [agent](agent.md)) and the containers 
and save it in the file queues at `${dm.file.fbstorage}` (default `${java.io.tmpdir}/cluman/fbstorage` ) directory. 
* It provides an API to interact with the platform (see http://$MASTER_IP:8761/swagger-ui.html) and user interface. 

## How Does it Work? ##

### Basic Entities ###

Cluman has the follow entities:

* `NodesGroup` - a group of nodes
 
    NodesGroup has 'features' which may be used for resolving into these group types:

    * `SWARM` - nodes in this group type are grouped together by a single 'swarm' service. We consider groups with this feature as 'cluster' or 'real cluster'.
    * `FORBID_NODE_ADDITION` - this group type is a meta group created by the system (For example, "orphan" mentioned below.) No modification is allowed for these NodesGroup.

    In addition, Cluman has some pre-defined NodesGroup (all of them are them stored in `DiscoveryStorage.SYSTEM_GROUPS`):

    * `DiscoveryStorage.GROUP_ID_ALL` - this group contains all currently on-line nodes.
    * `DiscoveryStorage.GROUP_ID_ORPHANS` - this group contains all nodes that do not belongs to any `RealCluster`. 
    
* `RealCluster` - a type of NodeGroup supported as `SWARM` service. Do not confuse it with 'real cluster', because 
`RealCluster` - the type name of domain object which is represent 'real cluster' in Cluman.  
* `Node` - node of cluster, Cluman does not differ node which contains `cluster-manager` container from others.

### Starting Up ###

When Cluman is started, it 

1. Sets up a `DiscoveryStorageImpl`.
2. Reads from the etcd storage and loads list of registered `RealCluster`. 
3. For each cluster cluman, it runs `swarm` instance through `DockerServices.getOrCreateCluster()`. 

_Docker Swarm is used for managing the NodesGroups (wrapped in `DockerServiceImpl`) for `Real Cluster` 
and `VirtualDockerService`._ 

For gathering nodes' events from Docker services, Cluman connects to each node directly. These connections are
stored in `DockerServices`. The registered nodes' information are stored in `NodeStorage`. 

When a node agent sends data to `NodeStorage` through `TokenDiscoveryServer`, the storage will add additional node reference into the
Swarm part of etcd tree via `NodeStorage.updateSwarmRegistration`).

## Nodes ##

Cluman registers node through [agent](agent.md) but also use  information about node from [docker info](https://docs.docker.com/engine/reference/api/docker_remote_api_v1.21/#/display-system-wide-information).
All gathered info are saved in `NodeRegistrationImpl`. Data about node health and metrics is published as `NodeMetrics`.

Node has two main flags `health.healthy` and `on`. The difference between the two are:
   
   * `on` - It shows the online node status.  The value is true when the node agent send ack in a specified time. If the timeout is exceeded 
   then node is immediately set to off (`on=false`). See `NodeRegistrationImpl.isOn` for details. Its flag ignores the status of 
   the node's Docker service.  
   * `healthy` - Its value is derived from the Docker service status. The Docker developers can declare it as 'engine is unreachable' but 
     we may use the analysis of node metrics (for example storage space is exceeded, or hdd SMART errors).
     
Note that node.health (aka NodeMetrics) has time value based on the local node time.

## Containers ##
### Creating containers ###
Cluman uses options from different sources for creating new container:

* API
* Compose like (yml) or properties file from git, examples: [containers-configuration](https://bitbucket.org/codeabovelab/containers-configuration/src/13a21fcf8057?at=master) where dev is cluster name
    * dm.image.configuration.git.url=https://<url>.git
    * dm.image.configuration.git.username=<username>
    * dm.image.configuration.git.password=<password>
* Image labels with `arg.` prefix, example:
    * `LABEL arg.memory=512M`
    * `LABEL arg.restart=always`
    * `LABEL arg.ports=8761:8761`

## Applications ##
Application uses Docker Compose as the backend. Each application contains:

* String name
* String cluster
* File initFile
* Date creatingDate
* List containers

**TODO: More details**

## Events ##

Cluman has global instances of `MessageBus`. Each instance has a unique ID, usually its `id` can be obtain from
static field of event class: `<EventClass>.BUS`. 

List of global buses:

* bus.cluman.dockerservice - `DockerServiceEvent`, notifies `DockerServiceInfo` events. 
* bus.cluman.log.application - `ApplicationEvent`, notifies 'applications' events.
* bus.cluman.log.registry - `RegistryEvent`, notifies registry adding and deletion events. 
* bus.cluman.node - `NodeEvent`, notifies node status updates, which are derived from [haven-agent](agent.md) requests.
* bus.cluman.log.docker - `DockerLogEvent`, notifies proxy events from Docker service, see `DockerServices.convertToLogEvent`
* bus.cluman.log.nodesGroup - `NodesGroupEvent`, notifies NodesGroup creations and deletions.
* bus.cluman.erorrs - `LogEvent`, bus aggregate messages from other buses with `WithSeverity.getSeverity() >= WARNING`,
 also has history. 
* bus.cluman.pipeline - `PipelineEvent` notifies pipeline changes.
* bus.cluman.job - `JobEvent`, notifies changes of each `JobInstance` changes, contains `JobInfo`, can be caused by
   `JobInstance.send()`.

Many events has `action` field which has values described in `StandardActions` class:

* create - some object will be created, note that for example `DockerService` cann't be `create`, but can be `start` 
* update 
* delete
* start - applicable for some objects which have run state, containers, jobs, processes  
* stop
* die - unexpected `stop`; usually it mean error
* online - it and below action applicable for objects which used through network: node, Docker service and etc.
* offline

Events also have `severity` fields . It can have `INFO`, `WARNING` and `ERROR` status. 

## API ##

API is published at http://$MASTER_IP:8761/swagger-ui.html URL.
