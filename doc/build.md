[![Build Status](https://travis-ci.org/codeabovelab/haven-platform.svg?branch=master)](https://travis-ci.org/codeabovelab/haven-platform)

# Building From Source #

## Requirements ##

For building Haven:

* JDK (Oracle or Open) >= 1.8
* Maven >= 3.2
* Git >= 2.0

For running Haven:

* JRE (Oracle or Open) >= 1.8
* etcd >= 2.2.5
* Swarm >= 1.2.6 (optional or docker with swarm-mode)

## Building ##

Get source code from GitHub:

    git clone https://github.com/codeabovelab/haven-platform.git

Change directory into the local repo:

    cd haven-platform

Build the backend (note that it downloads about 150MB of dependencies to '~/.m2/repository'):

    mvn -P staging -Dmaven.test.skip=true clean package

'staging' profile embed frontend into backend. 

## Running ##

For running the build, you need to install etcd and [Swarm](https://github.com/docker/swarm/blob/master/CONTRIBUTING.md). Note that if your Swarm has different binary name then you 
must specify full path to it as `--dm.swarm-exec.path=$FULL_PATH_TO_SWARM/swarm` argument.
Also see [Dockerfile](https://github.com/codeabovelab/haven-platform/blob/master/cluster-manager/Dockerfile#L11)

So on command line for running the server: 

    java -jar cluster-manager/target/cluster-manager-*-boot.jar --dm.kv.etcd.urls=http://127.0.0.1:2379

After startup, it will be available by browsing to:
* API: http://localhost:8761/swagger-ui.html
* UI (if "staging" profile was activated): http://localhost:8761/

for accessing the UI, use the credential -  

    username: admin
    password: password

