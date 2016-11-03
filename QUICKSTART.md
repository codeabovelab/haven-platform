# Quickstart #
 
## What it is? ##
 
 Dockmaster - is a cluster management system. It created for simple and easy using of all docker features through user friendly and powerful UI. It built over Docker, Swarm, and Compose.
 
## Installation ##
 
 Requirements:

 
 Common:
 
 * docker >= 1.10
 * python >= 3.5
 * python3-psutil >= 4.2
 
 For master node:
 
 * etcd >= 2.2.5
 
 **Installation process for debian/ubuntu**
 
 We define common variables for configuration:
 
```sh
 
 # ip of master node
 MASTER_IP=172.31.0.3 
 
 #ip of current configured node, this ip must be accessible from master
 SELF_IP=172.31.0.12 or 172.31.0.12 
```
 
 Configure docker on each node. By default docker listen on unix socket, and we
  need to configure it for tcp socket.
  
```sh
 %cat /etc/default/docker
 DOCKER_OPTS="--cluster-store=etcd://$MASTER_IP:2379/dn --cluster-advertise=$SELF_IP:2375 \
  -H tcp://0.0.0.0:2375"
```
 
 *Master node installation:*
 
```sh
 docker run -d --name=cluster-manager -p 8761:8761 -e "kv_etcd_urls=http://$MASTER_IP:2379" ni1.codeabovelab.com/cluster-manager

```
 
**Configuration**

Any settings can be passed via env variables 
or
via git repository, for this add to git repository file cluster-manager.properties or cluster-manager.yml
and specify url to dockmaster:

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
or via git repo:
dm.kv.etcd.urls=http://$MASTER_IP:2379


 *Node installation:*
 
 On node you need only docker and [dockmaster-agent](/doc/agent.md).
 
```sh
 wget 'http://$MASTER_IP:8762/res/agent/dockmaster-agent.py'
 chmod +x dockmaster-agent.py
```
 
 Then you need to create config file for agent in one of following locations or use command line options of agent.: 
 
 * ./dm-agent.ini
 * ~/.config/dm-agent.ini
 * /etc/dm-agent.ini
 
 with content:
 
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