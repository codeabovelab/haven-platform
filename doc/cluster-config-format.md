# Configuration Format Description #

Compose - common format for deploying docker containers system, but it does not support some features, for example: describe multiple clusters in one file. So we were forced to create own configuration file format. It uses Compose format as the base.

Example of one cluster configuration:

```yaml
--- # see com.codeabovelab.dm.cluman.model.RootSource
version: "1.0"
clusters: # see com.codeabovelab.dm.cluman.model.ClusterSource
  first-cluster:
    title: "Cluster title"
    description: "Long description of cluster"
    config: # see com.codeabovelab.dm.cluman.cluster.docker.ClusterConfigImpl
      hosts: [] # list of existed swarm services
      maxCountOfInstances: 10 # max count scaled containers of same image
      # Maximal timeout for docker api access. In seconds. 
      # For some readonly ops, like getinfo, system use small hardcoded timeout.
      dockerTimeout: 300
      # https://docs.docker.com/swarm/scheduler/strategy/
      # DEFAULT, SPREAD, BINPACK, RANDOM
      strategy: DEFAULT
      registries:
      - "docker-registry.example.com"
    nodes:
    - "first-node"
    - "second-node"
    applications: # see com.codeabovelab.dm.cluman.model.ApplicationSource
      first-app: # name of application
        containers: # see com.codeabovelab.dm.cluman.model.ContainerSource
          app-containers: 
            hostname: "app-container"
            image: "nginx:latest"
            node: "first-node"
    containers: # see com.codeabovelab.dm.cluman.model.ContainerSource
      first-container: # name of container
        image: "docker-registry.example.com/cluster-container:latest"
        hostname: "container-host" # when not specified system will use container name 
        domainname: "example.com"
        # Name of node. If node with this name is exists then container placed on it, otherwise containers with same
        # node name placed onto same node (node choosing algorithm currently is unspecified). When node is null, the node
        # will be chose by 'swarm scheduler filters'.
        node: "second-node"
        labels:
          service-type: "system"
        ports:  #  HOST:CONTAINER format 
          80: 8080
        cpuShares: 0
        cpuQuota: 0
        blkioWeight: 0
        cpuPeriod: null
        cpusetCpus: ""
        cpusetMems: null
        restart: always
        memoryLimit: "1GiB"
        memorySwap: null
        memoryReservation: null
        kernelMemory: null
        environment:
        - "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/bin:/usr/bin:/gocode/bin"
        - "JAVA_OPTS= -Xms64M -Xmx256M -Xss256k -XX:-HeapDumpOnOutOfMemoryError -XX:+UseConcMarkSweepGC"
        - "constraints:node!=/\\\\Qfirst-node\\\\E/"
        include: [] # list of yaml files for inclusion
        # List for define container volumes, in future.
        # The format is `container-dest[:<options>]`.
        # The comma-delimited `options` are [rw|ro], [[r]shared|[r]slave|[r]private], and [nocopy].
        # https://docs.docker.com/engine/tutorials/dockervolumes/
        volumes: [] 
        # Binds of external (node paths or named volumes to container). <p/>
        # The format is `host-src:container-dest[:<options>]`.
        # The comma-delimited `options` are [rw|ro], [[r]shared|[r]slave|[r]private], and [nocopy].
        # The 'host-src' is an absolute path or a name value.<p/>
        volumeBinds: []
        volumeDriver: ""
        # List of entris like `container:['rw'|'ro']` which is used as volume source
        volumesFrom: []
        reschedule: null
        publishAllPorts: false
        links: {}
        command: []
        dns: []
        dnsSearch: []
        # Sets the networking mode for the container. 
        # Supported standard values are: `bridge`, `host`, `one`, and `container:<name|id>`.
        # Any other value is taken as a custom network’s name to which this container should connect to.
        network: "first-net"
        networks:
        - "first-net"
        # A list of hostname -> IP mappings to add to the container’s /etc/hosts file. 
        # Specified in the form "hostname:IP".
        extraHosts: []
        # A list of string values to customize labels for MLS systems, such as SELinux.
        securityOpt: []
    # filter of applicable images (see FilterFactory)
    # filters:
    #   spel-image - SpEL string which applied to images. It evaluated over object with 'tag(name)' and 
    #               'label(key, val)' functions, also it has 'r(regexp)' function which can combined 
    #               with other, like: 'tag(r(".*_dev")) or label("dev", "true")'.
    #   list - a simply list of possible values, like `list:nginx,java`
    #   labels - list of labels, like `labels:production-ready=true,tested=true`
    #   regex - regexp based filter: `regex:.+stable`
    #   pattern - pattern based filter: `pattern:*stable`
    imageFilter: null
```
