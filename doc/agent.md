# Haven Agent #

Currently system may act without agent, use old python based agent or use new agent from docker image.

Comparison:

| name        | secure usage docker API | gather node metrics | do not have extra dependencies | 
|-------------|---|---|---|
| python agent| - | + | - |
| no agent    | - | - | + |
| agent image | + | - | + |

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
```docker run --name agent -d --restart=unless-stopped -p 8771:8771 -v /run/docker.sock:/run/docker.sock codeabovelab/agent:1.2.0-SNAPSHOT```

Note taht agent is built with self-signed certificate, cluster-manager use same certificate too. [Certificate generated on each build](https://github.com/codeabovelab/haven-platform/blob/dc38ed2ed9368fa4436b411400f4b20cd92457a2/pom.xml#L121), therefore when you use agent with cluster-manager of different verision, you will get error. It can be fixed by usage of same version, or option 'dm.ssl.check=false' to cluster-manager.

## Python agent ##

The agent is [a Python script](/cluster-manager/src/main/resources/static/res/agent/node-agent.py). 
It is written in Python 3 and only has one dependency: python3-psutil >= 4.2.

use '-h' option for getting help:

```
usage: haven-agent.py [-h] [-d DOCKER] [-m MASTER] [-t TIMEOUT] [-v]
                           [-f CONFIG]

Haven node agent.

optional arguments:
  -h, --help            shows this help message
  -d DOCKER, --docker DOCKER
                        IP and port of docker service
  -m MASTER, --master MASTER
                        IP and port of Haven service
  -t TIMEOUT, --timeout TIMEOUT
                        timeout in seconds between node registration updates
  -v, --verbose         logging level, -v is INFO, -vv is DEBUG
  -f CONFIG, --config CONFIG
                        path to the configuration file

Example:
  haven-agent.py -d 172.31.0.11:2375 -m 172.31.0.3:8763 -t 2 -vv
Sample config:
  [main]
  docker = 172.31.0.12:2375
  master = 172.31.0.3:8762
  timeout = 10
  log_level = 2
By default find config in:
        $CWD/haven-agent.ini
        $HOME/.config/haven-agent.ini
        /etc/haven-agent.ini
```

Note that all Agent and Master instances must be accessible to each other.

## How does the Agent work? ##

The Agent sends data to MASTER on each TIMEOUT seconds. 
The Agent starts at DEBUG level and prints the transmitted data to stdout:
```
% ./haven-agent.py         
2016-08-10 15:03:04,583 - INFO - Configs: /home/user/.config/haven-agent.ini
Arguments: {'timeout': 10, 'master': '172.31.0.3:8762', 'docker': '172.31.0.12:2375', 'log_level': 2}
2016-08-10 15:03:05,607 - DEBUG - do registration with {"time": "2016-08-10T15:03:05.559807", "labels": {}, "id": "docker-exp", ...}
```

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
