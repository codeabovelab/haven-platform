# Dockmaster: Container Management Simplified
 
## Introduction
 Dockmaster is a Docker cluster management system. It is created to let user have a  simple and easy to use all of Docker’s features through
 a user friendly and powerful UI and command line tools. It is built on top of Docker, Swarm, and Compose. It offer management of multiple 
 clusters and image registries from one place and is built on top of Docker, Docker Swarm, and Docker Compose.

### Requirements

Dockmaster is comprised of Master and Node components. The common requirements for both are: 
 
 * Docker >= 1.10
 * Python >= 3.5
 * python3-psutil >= 4.2
 
For master node:
 
 * etcd >= 2.2.5
## Installation

The following installation instruction has been tested on Debian / Ubuntu.

*Installation for both Master and Node*
1. Define common variables for configuration:
 
```sh
 
 # IP of master instance
 MASTER_IP=172.31.0.3 
 
 # IP of current configured node. This IP must be accessible from master instance
 SELF_IP=172.31.0.12 or 172.31.0.12 
```
 
2. Configure Docker on each instance. By default, Docker listens on Unix socket and we need to configure it for the TCP socket.
  
```sh
 %cat /etc/default/docker
 DOCKER_OPTS="--cluster-store=etcd://$MASTER_IP:2379/dn --cluster-advertise=$SELF_IP:2375 \
  -H tcp://0.0.0.0:2375"
```
 
 *For installing Master:*
 
```sh
 docker run -d --name=cluster-manager -p 8761:8761 -e "kv_etcd_urls=http://$MASTER_IP:2379" ni1.codeabovelab.com/cluster-manager

```
 
**Configuration**

Any settings can be passed via environment variables or via git repository.  For reading cluster-manager.properties or cluster-manager.yml from a Git repository, 
you must specify the URL, username, and password:

```properties
 -e "dm_config_git_enabled=true"
 -e "dm_config_git_uri=https://bitbucket.org/<git repo url>"
 -e "dm_config_git_username=username"
 -e "dm_config_git_password=password"
```

*Main settings:*
dm.kv.etcd.urls - URL of etcd storage

*Can be passed via environment variables*
example: 
```sh
 -e "dm_kv_etcd_urls=http://$MASTER_IP:2379" 
```
or via Git repository:

dm.kv.etcd.urls=http://$MASTER_IP:2379

 *For installing Node:*
 
 On the Node instances, only Docker and [dockmaster-agent](/doc/agent.md) are required.
 
```sh
 wget 'http://$MASTER_IP:8762/res/agent/dockmaster-agent.py'
 chmod +x dockmaster-agent.py
```
 
 Create the config file for agent in one of following locations or use command line options of agent: 
 
 * ./dm-agent.ini
 * ~/.config/dm-agent.ini
 * /etc/dm-agent.ini
 
with the following content:
 
```ini
 [main]
 docker = $SELF_IP:2375
 master = $MASTER_IP:8762
 # timeout between node update requests
 timeout = 10 
 # 2 - Debug, 1 - Info, 0 - Warning
 log_level = 2 
```
 
```sh
 ./dockmaster-agent.py
```

## Index ##

* [Documentation](/doc/)

## TODO ##