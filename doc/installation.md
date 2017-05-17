# Haven Installation

Haven is comprised of Master and Agent components. The Master requires etcd and the Haven Server; both are installable as Docker containers as per instruction below.  Agent is a Python script which should be installed as a service on the other nodes. 

## Requirements

 The standard requirements are: 
 
 * Docker >= 1.10
 * Python >= 3.5
 * python3-psutil >= 4.2 (optional library for retrieving system utilization information (CPU, memory, disks, network))
 
For Master node, it will also require:
 
 * etcd >= 2.2.5

### Optional 

Haven has the option to use Git repositories to store its settings and the containers' setting and environment variables. If
you intend to make use of this feature, we recommend that the Git repositories be setup first.  Also, you can see the example
repositories we have setup to see the structure.

* Repository for Haven configuration: https://github.com/codeabovelab/haven-example-configuration
* Repository for containner configuration: https://github.com/codeabovelab/haven-example-container-configuration

## Installation Steps

The following installation instruction has been tested on Debian / Ubuntu.

*Installation for both Master and Agent*

**Step 1:** Define the common variables for configuration needed in the scripts and configuration files.  Feel free to set them as 
 environment variables or replace them in the script:
 
```sh
 
 # IP of master instance
 MASTER_IP=172.31.0.3 
 
 # IP of current configured node. This IP must be accessible from master instance
 SELF_IP=172.31.0.12
```

*Skip steps 2 and 3 if you already have Docker with etcd installed on Master and Agent nodes* 

**Step 2:** Configure etcd on Master node by following this instruction:
 
https://coreos.com/etcd/docs/latest/docker_guide.html

Use the following command to start the etcd container: 
```sh
docker run -d -v /usr/share/ca-certificates/:/etc/ssl/certs \
     -p 4001:4001 -p 2380:2380 -p 2379:2379 --restart=always  \
     --name etcd0 quay.io/coreos/etcd:v2.3.7  -name etcd0  \
     -data-dir=data  -advertise-client-urls http://$MASTER_IP:2379,http://$MASTER_IP:4001 \
     -listen-client-urls http://0.0.0.0:2379,http://0.0.0.0:4001  -initial-advertise-peer-urls http://$MASTER_IP:2380 \
     -listen-peer-urls http://0.0.0.0:2380  -initial-cluster-token etcd-cluster-1 \
     -initial-cluster etcd0=http://$MASTER_IP:2380  -initial-cluster-state new

```
In addition you can also mount a directory from your Docker engine’s host into a etcd data.
```sh
-v /home/user/data/docker/etcd/:/data/
```
**Step 3:** 

 Use the following instruction to install Docker on the different Linux/Cloud, Windows, 
and MacOS instruction:
 
 https://docs.docker.com/engine/installation/

After it you have two ways, simply: use agent which is proxy docker to local network, or more complex but agent-less configuration.

_Simply way._ Run agent on each node:

```
docker run --name agent -d --restart=unless-stopped -p 8771:8771 -v /run/docker.sock:/run/docker.sock codeabovelab/agent:1.2.1
```

Then open in UI in 'Admin' -> 'Add node' and add node with address like 'http://$SELF_IP:8771'.

_Agent-less way._ If you want to run node without any agent, then you need to expose docker on newtwork port.

 By default, Docker listens on Unix socket so TCP socket configuration is needed. See the config file in /etc/default/docker 
 and make sure the DOCKER_OPTS argument matches the one listed below (with the IP variables replaced with real value):
  
```sh
 %cat /etc/default/docker
a DOCKER_OPTS="--cluster-store=etcd://$MASTER_IP:2379/dn --cluster-advertise=$SELF_IP:2375 \
  -H tcp://0.0.0.0:2375"
```
Then open in UI in 'Admin' -> 'Add node' and add node with address like 'http://$SELF_IP:2375'.

**Step 4:** Install the Haven container by executing the following command:
 
```sh
 docker run -d --name=cluman -p 8761:8761 --restart=always \
         -e "dm_kv_etcd_urls=http://$MASTER_IP:2379" codeabovelab/cluster-manager

```
For storing data use:
```sh
-v /home/user/data/docker/haven/:/data

```
 
The Haven container can be started only with etcd's URL as its environment variable. It can have other optional parameters 
passed in as environment variables to enable other features. 

**At this point you should be able to login to the UI via http://<MASTER_IP>:8761/.**  The default admin credential is 
admin/password.

**Configuration**

Haven's Master settings can also be passed directly via environment variables or alternatively, as mentioned before, they can 
also be stored in a Git repository and credentials passed in via enviroment variable. For reading cluster-manager.properties or 
cluster-manager.yml from a Git repository, you must specify the Git URL, username, and password:

```properties
 -e "dm_config_git_uri=https://github.com/codeabovelab/haven-example-configuration.git"
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

You should take a look at the example configuration repository to see all of the settings available:

https://github.com/codeabovelab/haven-example-configuration

  
## Troubleshooting ##
If you are running on the latest Linux distros where systemd is used, you will need to manually modify the Docker daemon 
options in the systemd drop-in file (See https://docs.docker.com/engine/admin/systemd/ for details). The docker.service file's 
ExecStart parameter will need to be modified to something like:

```sh
ExecStart=/usr/bin/dockerd  -H unix:///var/run/docker.sock --cluster-store=etcd://<MASTER_IP>:2379/dn --cluster-advertise=eth0:2375 -H tcp://0.0.0.0:2375
```
