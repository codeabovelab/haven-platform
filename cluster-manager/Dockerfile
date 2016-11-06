FROM debian:stretch

MAINTAINER docker@codeabovelab.com

RUN apt-get update && apt-get install -y --no-install-recommends curl golang openjdk-8-jdk-headless && apt-get clean
RUN curl -L https://github.com/docker/compose/releases/download/1.8.1/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose && \
  chmod +x /usr/local/bin/docker-compose

RUN export GOPATH=/gocode && \
  go get github.com/tools/godep && \
  export PATH=$PATH:$GOPATH/bin && \
  mkdir -p $GOPATH/src/github.com/docker/ && \
  cd $GOPATH/src/github.com/docker/ && \
  git clone https://github.com/docker/swarm && \
  cd swarm && \
  git checkout -b v1.2.5 v1.2.5 &&\
  godep go install .


ENV PATH $PATH:/bin:/usr/bin:/gocode/bin

LABEL service-type=system

LABEL arg.memory=512M
LABEL arg.restart=always
LABEL arg.ports=8761:8761

ENV JAVA_OPTS=" -Xms64M -Xmx512M -Xss256k -XX:-HeapDumpOnOutOfMemoryError "

EXPOSE 8761
EXPOSE 8762


RUN mkdir /data
WORKDIR /data
VOLUME  /data

ADD ./cluster-manager-*-boot.jar /${artifactId}.jar
ENTRYPOINT java -server $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=staging -jar /${artifactId}.jar \
  --https.keystore.password=dockmaster \
  --https.keystore=/server.p12 \
  --server.port=8761 \
  --https.port=8762 \
  --dm.swarm-exec.path=/gocode/bin/swarm \