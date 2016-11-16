# Dockmaster: Container Management Simplified
 
## Introduction
Dockmaster is a Docker cluster management system. It let user with basic Docker understaning to have a straightforward to use its 
features through a user-friendly yet powerful UI and command line tools. It is built on top of Docker, Swarm, and Compose. It 
offers management of multiple clusters and image registries from one place and is built on top of Docker, Swarm, and Compose.

### Requirements

Dockmaster is comprised of Master and Agent nodes. The common requirements for both are: 
 
 * Docker >= 1.10
 * Python >= 3.5
 * python3-psutil >= 4.2 (optional library for retrieving system utilization information (CPU, memory, disks, network))
 
For master node:
 
 * etcd >= 2.2.5
## Installation

The following installation instruction has been tested on Debian / Ubuntu.

*Installation for both Master and Agent*
1. Define common variables for configuration:
 
```sh
 
 # IP of master instance
 MASTER_IP=172.31.0.3 
 
 # IP of current configured node. This IP must be accessible from master instance
 SELF_IP=172.31.0.12
```

*Skip steps 2 and 3 if you already use docker with etcd*

2. Configure etcd on master
https://coreos.com/etcd/docs/latest/docker_guide.html
```sh
docker run -d -v /usr/share/ca-certificates/:/etc/ssl/certs -p 4001:4001 -p 2380:2380 -p 2379:2379 \
     --name etcd quay.io/coreos/etcd:v2.3.7  -name etcd0  -advertise-client-urls http://$MASTER_IP:2379,http://$MASTER_IP:4001 \
     -listen-client-urls http://0.0.0.0:2379,http://0.0.0.0:4001  -initial-advertise-peer-urls http://$MASTER_IP:2380 \
     -listen-peer-urls http://0.0.0.0:2380  -initial-cluster-token etcd-cluster-1 \
     -initial-cluster etcd0=http://$MASTER_IP:2380  -initial-cluster-state new

```

3. Configure Docker on each instance. By default, Docker listens on Unix socket so TCP socket configuration is needed.
  
```sh
 %cat /etc/default/docker
 DOCKER_OPTS="--cluster-store=etcd://$MASTER_IP:2379/dn --cluster-advertise=$SELF_IP:2375 \
  -H tcp://0.0.0.0:2375"
```
 
4. *installing DockMaster:*
 
```sh
 docker run -d --name=cluman -p 8761:8761 -e "dm_kv_etcd_urls=http://$MASTER_IP:2379" codeabovelab/cluster-manager

```
 
**Configuration**

Any settings can be passed via environment variables or via git repository.  For reading cluster-manager.properties or cluster-manager.yml from a Git repository, 
you must specify the URL, username, and password:

```properties
 -e "dm_config_git_uri=https://github.com/codeabovelab/dockmaster-example-configuration.git"
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

 *For installing Agent [dockmaster-agent](/doc/agent.md):*
 
```sh
 wget 'http://$MASTER_IP:8762/res/agent/dockmaster-agent.py'
 chmod +x dockmaster-agent.py
```
 
 Create the config file for Agent in one of following locations or use command line options of agent: 
 
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

For additional technical detail, see:

* [Documentation](/doc/)
* [Common application properties](https://github.com/codeabovelab/dockmaster-example-configuration/blob/master/cluster-manager.properties)

## TODO ##
