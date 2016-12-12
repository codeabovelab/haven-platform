# Dockmaster Agent #

Agent is [a Python script](/cluster-manager/src/main/resources/static/res/agent/node-agent.py). 
It is written in Python 3 and only has one dependency: python3-psutil >= 4.2.

use '-h' option for getting help:

```
usage: dockmaster-agent.py [-h] [-d DOCKER] [-m MASTER] [-t TIMEOUT] [-v]
                           [-f CONFIG]

DockMaster node agent.

optional arguments:
  -h, --help            shows this help message
  -d DOCKER, --docker DOCKER
                        IP and port of docker service
  -m MASTER, --master MASTER
                        IP and port of dockmaster service
  -t TIMEOUT, --timeout TIMEOUT
                        timeout in seconds between node registration updates
  -v, --verbose         logging level, -v is INFO, -vv is DEBUG
  -f CONFIG, --config CONFIG
                        path to the configuration file

Example:
  dockmaster-agent.py -d 172.31.0.11:2375 -m 172.31.0.3:8763 -t 2 -vv
Sample config:
  [main]
  docker = 172.31.0.12:2375
  master = 172.31.0.3:8762
  timeout = 10
  log_level = 2
By default find config in:
        $CWD/dm-agent.ini
        $HOME/.config/dm-agent.ini
        /etc/dm-agent.ini
```

Note that all Agent and Master instances must be accessible to each other.

## How does the Agent work? ##

The Agent sends data to MASTER on each TIMEOUT seconds. 
The Agent starts at DEBUG level and prints the transmitted data to stdout:
```
% ./dockmaster-agent.py         
2016-08-10 15:03:04,583 - INFO - Configs: /home/user/.config/dm-agent.ini
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
