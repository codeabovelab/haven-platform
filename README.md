[![Build Status](https://travis-ci.org/codeabovelab/haven-platform.svg?branch=master)](https://travis-ci.org/codeabovelab/haven-platform) [![codebeat badge](https://codebeat.co/badges/3c0e5ad9-8f24-4c41-bddc-2221a3b05f7b)](https://codebeat.co/projects/github-com-codeabovelab-haven-platform)

# Haven: Container Management Simplified
 
## Introduction
Haven is a Docker cluster management system. The user controls the entire platform via user-friendly yet powerful UI and 
commandline tools. Built on top of Docker, Swarm, and Compose, it offers multiple clusters and image registries management.

License
-------
Copyright 2016 Code Above Lab LLC

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0) (the "License");
you may not use this file except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Index ##

For additional technical detail, see:

* [Installation](/doc/installation.md)
* [Additional Documentation](/doc/)
* [Common Application Properties](https://github.com/codeabovelab/haven-example-configuration/blob/master/cluster-manager.properties)

### Feature Overview

Haven features the following capabilities:

* [Dashboard](#dashboard)
* [Cluster Management](#cluster-management)
* [Container Management](#container-management)
* [Node Status](#node-status)
* [Image Management](#image-management)
* [Registry Management](#registry-management)
* [Jobs](#jobs)
* [Delegated Admin](#delegated-admin)
* [Subset of additional features](#subset-of-additional-features)

#### Dashboard
The dashboard gives an overview of the status of the cluster and any containers or nodes that are using excessive 
resources. 
![dashboard](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/dashboard.png)

#### Cluster Management
In the Cluster module, the user can manage the nodes and the containers, and view any errors the in the cluster event log.
![clusters](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/clusters.png)

#### Container Management
In the Containers module, the user can manage all of the active containers and their status. The user can also view logs,
moreover, configurations of each container.
![containers](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/containers.png)


#### Node Status
In the Node module, the user can see all nodes in the system and their detailed info.
![nodes](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/nodes.png)

#### Image Management
In the Image Management module, the user can see all of the images downloaded onto each cluster and which nodes the image 
resides on. Users can remove old images to save disk space.
![images](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/images.png)

#### Registry Management
Private and public Registries can be configured on the system to let the user easily select the desired image for downloading
without having to remember which registry it is on.
![registries](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/registries.png)


#### Jobs
Unattended jobs can be scheduled and results are available for auditing purposes.
![jobs](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/jobs.png)
Each cluster can have its set of jobs to update specific images. 
![update](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/update.png)

#### Delegated Admin
Users can be assigned administrative rights to specific clusters to avoid admin bottleneck.
![users](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/users.png)

#### Additional Features
1. Creating/deleting tags and ability to set filters for clusters based on the tags.  Use case would be  creating workflow: only images which were tested at QA should be visible on the prod cluster.
2. Storing containers configuration in VCS per cluster, see https://github.com/codeabovelab/haven-example-container-configuration
3. Additional policies/constraints for Swarm.
4. Group operations such as cleaning space, upgrade/rollback containers. For example,  use cases are:

       a. checking/updating all containers from specified repository every five minutes for test cluster
       
       b. one-time update specified list of containers (which use common API, etc).
       
       c. or just click update all in this cluster
5. Backups of system configuration
6. Network management:
![api](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/network.png)
7. Events:
![api](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/events.png)
8. API
![api](https://raw.githubusercontent.com/codeabovelab/haven-platform/master/doc/img/api.png)


