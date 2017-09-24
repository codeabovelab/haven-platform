# Haven Installation

Haven is comprised of Master and Agent components. The Master requires etcd and the Haven Server; both are installable as Docker containers as per instruction below.  The agent is a Python script which should be installed as a service on the other nodes. 

## Requirements (installation is described in installation steps):
 
 * Docker >= 1.10
 
 * etcd >= 2.2.5

## Installation Steps

The following installation instruction has been tested on Debian / Ubuntu / CentOS.

**Installing Docker (Skip this step if you already have Docker):**

Use the following instruction to install Docker on the different Linux/Cloud, 
and MacOS instruction:
 
 https://docs.docker.com/engine/installation/

**Step 1 (installing Etcd):** 

Define the common variables for configuration needed in the scripts and configuration files.  Feel free to set them as environment variables or replace them in the script:
 
```sh
 
 # IP of master instance
 export MASTER_IP=172.31.0.3 
 
```

*Skip step if you already have installed etcd* 

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
In addition, you can also mount a directory from your Docker engine’s host into the etcd's data directory.
```sh
-v /home/user/data/etcd/:/data/
```
https://coreos.com/etcd/docs/latest/docker_guide.html

**Step 2 (Installing server):** 

Install the Haven container by executing the following command:
 
```sh
 docker run -d --name=cluman -p 8761:8761 --restart=unless-stopped \
         -e "dm_kv_etcd_urls=http://$MASTER_IP:2379" codeabovelab/cluster-manager

```
For storing data use:
```sh
-v /home/user/data/haven:/data/

```

**At this point you should be able to login to the UI via http://<MASTER_IP>:8761/.**  The default admin credential is 
admin/password.

**Step 3 (adding nodes):** 

You have two way to add nodes: 1) use agent which is docker container or 2) use the more complex but the agent-less configuration.

**Simple way.** Run agent:
copy start string from 'Admin' -> 'Add node' -> 'Get Agent Command'

![agent](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/agent.png)
start string example:

```
docker run --name havenAgent -d -e "dm_agent_notifier_server=http://$MASTER_IP:8761"  --hostname=$(hostname) --restart=unless-stopped -p 8771:8771 -v /run/docker.sock:/run/docker.sock codeabovelab/agent:latest

```
Server should have access to 8771 port (agent port)

**Agent-less way.** If you want to run a node _without_ installing and running an agent, then you need to expose docker on a network port.

By default, Docker listens on Unix socket, so TCP socket configuration is needed. See https://docs.docker.com/v1.11/engine/admin/systemd/#custom-docker-daemon-options 
and make sure the DOCKER_OPTS has something like:
  
```sh
... -H tcp://0.0.0.0:2375
```
or 
```sh
... -H tcp://$SELF_IP:2375
```
Open in UI in 'Admin' -> 'Add node' and add node with address like 'http://$SELF_IP:2375'.

________________________________

### Optional 

Haven has the option to use Git repositories to store its settings and the containers' setting and environment variables. If
you intend to make use of this feature, we recommend that the Git repositories be setup first.  Also, you can see the example
repositories we have setup to see the structure.

* Repository for Haven configuration: https://github.com/codeabovelab/haven-example-configuration
* Repository for containner configuration: https://github.com/codeabovelab/haven-example-container-configuration

**Optional Configuration**

Haven's Master settings can also be passed directly via environment variables or, as mentioned before, they can also be stored in a Git repository and credentials passed in via an environment variable. For reading cluster-manager.properties or cluster-manager.yml from a Git repository, you must specify the Git URL, username, and password:

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

You should take a look at the example configuration repository to see all of the available settings:

https://github.com/codeabovelab/haven-example-configuration

  
## Using the Swarm (not swarm-mode) and agentless installation: ##
The docker.service file's (https://docs.docker.com/v1.11/engine/admin/systemd/#custom-docker-daemon-options)
ExecStart parameter will need to be modified to something like:
```sh
ExecStart=/usr/bin/dockerd  -H unix:///var/run/docker.sock --cluster-store=etcd://<MASTER_IP>:2379/dn --cluster-advertise=eth0:2375 -H tcp://0.0.0.0:2375
```

## Troubleshooting

![Agent Health](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/troubleshooting.png) 

*Agent Health = off*, means that server can't connect to agent, 
agent's ip and port can be checked via *Info* button:
 
![Agent Info](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/troubleshooting_2.png) 
