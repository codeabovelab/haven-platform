# Cluman #

The internal name of main part of Dockmaster project. This part manages the clusters.
Name of project is simply first letters of CLUster-MANager.

## What is Cluman? ##

* Cluman manages the following modules (data is stored in `etcd` (run `etcdctl ls /cluman/` for checking)):
    * `/nodes` - list of connected nodes
    * `/clusters` - list of clusters (currently only `RealCluster` stored here)
    * `/containers` - list of containers
    * `/applications` - list of compose applications 
    * `/docker-registry` - list of known registries
    * `/pipelines` - list of pipelines
* It gathers metric information from the nodes (part of info provided by [agent](agent.md)) and the containers 
and save it in file queues at `${dm.file.fbstorage}` (default `${java.io.tmpdir}/cluman/fbstorage` ) directory. 
* It provides an API to interact with the platform (see http://$MASTER_IP:8761/swagger-ui.html )

## How Does it Work? ##

Cluman has the follow entities:

* `NodesGroup` - a group of nodes. 
 
    NodesGroup has 'features' which may be used for resolving into these group types:

    * `SWARM` - nodes in this group type are grouped together by a single 'swarm' service. We consider groups with this feature as 'cluster' or 
'real cluster'.
    * `FORBID_NODE_ADDITION` - this group type is a meta group created by the system (For example, "orphan" mentioned below.) They do not allow to modify list of nodes

    Also, Cluman has some pre-defined NodesGroup (all of them are them stored in `DiscoveryStorage.SYSTEM_GROUPS`):

    * `DiscoveryStorage.GROUP_ID_ALL` - this group contains all currently on-line nodes.
    * `DiscoveryStorage.GROUP_ID_ORPHANS` - this group contains all nodes that do not belongs to any `RealCluster`. 
    
* `RealCluster` - a type of NodeGroup supported as `SWARM` service. Do not confuse it with 'real cluster', because 
`RealCluster` - the type name of domain object which is represent 'real cluster' in Cluman.  
* `Node` - node of cluster, Cluman does not differ node which contains `cluster-manager` container from others.

At starting, Cluman sets up a `DiscoveryStorageImpl`. It reads from the etcd storage and loads list of registered `RealCluster`. 
For each cluster cluman, it runs `swarm` instance through `DockerServices.getOrCreateCluster()`. 

_Management of NodesGroups done by Swarm (wrapped in `DockerServiceImpl`) for `Real Cluster` 
and `VirtualDockerService` for `NodesGroupImpl`._ 

So for gathering events from Docker services on the nodes, Cluman also connected to each node directly. Those connections are
stored in `DockerServices`. The information about the registered nodes are stored in `NodeStorage`. 

When a node agent posts data into `NodeStorage` through `TokenDiscoveryServer`, the storage will add additional node reference into the
Swarm part of etcd tree via `NodeStorage.updateSwarmRegistration`).

## Nodes ##

Cluman register node through [agent](agent.md), but also use some information about node from [docker info](https://docs.docker.com/engine/reference/api/docker_remote_api_v1.21/#/display-system-wide-information).
All gathered info saved in `NodeRegistrationImpl`, data about node health and metrics is published as `NodeMetrics`.

Node has two main flags `health.healthy` and `on`. Difference between them:
   
   * `on` - show is online node status, it true while node agent send requests in specified timeout, if timeout is exceeded 
   then node immediate stay off (`on=false`). See `NodeRegistrationImpl.isOn` for details. It flag ignore status of 
   node docker service.  
   * `healthy` - it depends from docker service status,  docker developers declare it as 'engine is unreachable', but 
     we may use analysis of node metrics (for example storage space is exceeded, or hdd SMART errors).
     
Note that node.health (aka NodeMetrics) has time value based on local node time.

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
Uses compose as backend for applications. 
Each application contains:

* String name;
* String cluster
* File initFile
* Date creatingDate
* List containers

**TODO**

## Events ##

Cluman has global instances of `MessageBus`. Each instance has unique id, usually its `id` can be obtain from
static field of event class: `<EventClass>.BUS`. 

List of global buses:

* bus.cluman.dockerservice - `DockerServiceEvent`, notify about `DockerServiceInfo` changes. 
* bus.cluman.log.application - `ApplicationEvent`, notify about 'applications'.
* bus.cluman.log.registry - `RegistryEvent`, notify about registry adding and deletion. 
* bus.cluman.node - `NodeEvent`, notify about node status updates. Causes by [dockmaster-agent](agent.md) requests.
* bus.cluman.log.docker - `DockerLogEvent`, proxy events from docker service, see `DockerServices.convertToLogEvent`
* bus.cluman.log.nodesGroup - `NodesGroupEvent`, notify about NodesGroup creations and deletions.
* bus.cluman.erorrs - `LogEvent`, bus aggregate messages from other buses with `WithSeverity.getSeverity() >= WARNING`,
 also has history. 
* bus.cluman.pipeline - `PipelineEvent` notify about pipeline changes.
* bus.cluman.job - `JobEvent`, notify about changes of each `JobInstance` changes, contains `JobInfo`, can be caused by
   `JobInstance.send()`.

Many events has `action` filed which can has values described in `StandardActions` class:

* create - meaning that some object will be created, note that for example `DockerService` cann't be `create`, but can be `start` 
* update 
* delete
* start - it and below applicable for some objects which have run state, containers, jobs, processes  
* stop
* die - unexpected `stop`, usually it mean error
* online - it and below action applicable for objects which used through network: node, docker service and etc.
* offline

Events have `severity` fields yet. It can have `INFO`, `WARNING` and `ERROR` status. 

## API ##

API is published at http://$MASTER_IP:8761/swagger-ui.html url.