# Haven Agent #

Currently system may act without agent, use old python based agent or use new agent from docker image.

Comparison:

| name        | secure usage docker API | gather node metrics | do not have extra dependencies | 
|-------------|---|---|---|
| no agent    | - | - | + |
| agent image | + | + | + |

Below problems which caused development of agent:
 - By default docker listen on unix socket, inaccessible from network. When you enable listening on 
 TCP, docker will stay insecure or require some complex configuration with certificates
 <sup>[1](https://docs.docker.com/edge/engine/reference/commandline/dockerd/#daemon-socket-option)</sup>.
 - Health of node depend from load usage of CPU & RAM and some other parameters like disk space. Docker can monitor 
 only small part of above characteristics.

## Agent image ##

New version (1.2) of system, can work without agent, but we implement new agent with proxy of docker connection. It 
agent connect with docker through unix socket and expose it on 8771 port with ssl encryption and authorization 
(credentials admin:password, see 'dm.auth.adminPassword' option of agent)

Command for run agent on node:
```docker run --name agent -d --restart=unless-stopped -p 8771:8771 -v /run/docker.sock:/run/docker.sock codeabovelab/agent:1.2.1```

Note that agent is built with self-signed certificate, cluster-manager use same certificate too. [Certificate generated on each build](https://github.com/codeabovelab/haven-platform/blob/dc38ed2ed9368fa4436b411400f4b20cd92457a2/pom.xml#L121), therefore when you use agent with cluster-manager of different verision, you will get error. It can be fixed by usage of same version, or option 'dm.ssl.check=false' to cluster-manager.


The Agent sends data in JSON format to 'http://$MASTER/discovery/nodes/$NODE_NAME' (see `com.codeabovelab.dm.cluman.ds.nodes.NodeAgentData` ). 
The [cluman](cluman.md) data is processed by `TokenDiscoveryServer.registerNodeFromAgent()`. 
Data is gathered from Docker info, optionally through `psutil`, and contains:

### Data sent by agents to the master ###

* time - local time
* name - Docker host name
* address - Docker IP address
* system
    * cpuLoad - 0.0-100.0
    * disks 
        * mount_point ->
            * used - bytes
            * total - bytes
    * labels - key-value collection of node labels (see `--label=key=value` argument of Docker daemon)
    * containers
        * ID
        * name
        * image - container image name
        * labels - key-value collection of container's labels 
