# How to build Cluman from sources #

## Requirements ##

For building:

* JDK (Oracle or Open) >= 1.8
* Maven >= 3.2
* Git >= 2.0

For running:

* JRE (Oracle or Open) >= 1.8
* etcd >= 2.2.5
* swarm >= 1.2.4

## Building ##

Obtain project source code from git:

    git clone https://github.com/codeabovelab/dockmaster-platform.git

Step into project dir:

    cd dockmaster-platform

Build backend (note that it download ~370MiB dependencies to '~/.m2/repository'):

    mvn -Dmaven.test.skip=true clean install

For embedding frontend into backend, you must run build again but with 'staging' profile, it consume more time 
therefore disabled by default:

    mvn -P staging -Dmaven.test.skip=true clean install

## Run ##

For running you need installed etcd and swarm. Note that if you swarm has different binary name then you 
must specify full path to it as `--dm.swarm-exec.path=$FULL_PATH_TO_SWARM/swarm` argument.

So command line for running cluman: 

    java -jar cluster-manager/target/cluster-manager-*-SNAPSHOT-boot.jar --dm.kv.etcd.urls=http://127.0.0.1:2379

Now you can open UI at http://localhost:8761/ as user 'admin' and 'password' credentials. If you see 404 error 
then you build without 'staging' profile, and you must rebuild project.

