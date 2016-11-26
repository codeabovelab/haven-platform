# Building Cluman from sources #

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

Get source code from git:

    git clone https://github.com/codeabovelab/haven-platform.git

Change dir:

    cd dockmaster-platform

Build backend (note that it downloads ~370MiB dependencies to '~/.m2/repository'):

    mvn -Dmaven.test.skip=true clean package

For embedding frontend into backend, you must run build again but with 'staging' profile, it consumes more time 
therefore disabled by default:

    mvn -P staging -Dmaven.test.skip=true clean package

## Run ##

For running you need to install etcd and swarm. Note that if your swarm has different binary name then you 
must specify full path to it as `--dm.swarm-exec.path=$FULL_PATH_TO_SWARM/swarm` argument.

So command line for running cluman: 

    java -jar cluster-manager/target/cluster-manager-*-boot.jar --dm.kv.etcd.urls=http://127.0.0.1:2379

After startup, will be available by browsing:
* API: http://localhost:8761/swagger-ui.html
* UI (if staging profile was activated): http://localhost:8761/

for accessing use admin/password credentials

