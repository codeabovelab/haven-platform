FROM debian:testing

MAINTAINER docker@codeabovelab.com

RUN apt-get update && apt-get install -y --no-install-recommends \
  curl git golang openjdk-8-jdk-headless procps \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN curl -L https://github.com/docker/compose/releases/download/1.11.1/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose && \
  chmod +x /usr/local/bin/docker-compose

RUN export GOPATH=/gocode && \
  export PATH=$PATH:$GOPATH/bin && \
  go get github.com/LK4d4/vndr && \
  go get github.com/golang/lint/golint && \
  mkdir -p $GOPATH/src/github.com/docker/ && \
  cd $GOPATH/src/github.com/docker/ && \
  git clone https://github.com/docker/swarm && \
  cd swarm && \
  git checkout -b v1.2.8 v1.2.8 && \
  go install . && \
  rm -rf $GOPATH/src/*

ENV PATH $PATH:/bin:/usr/bin:/gocode/bin

LABEL service-type=system
LABEL arg.memory=512M
LABEL arg.restart=always
LABEL arg.ports=8761:8761

ENV JAVA_OPTS=" -Xms64M -Xmx512M -Xss256k -XX:+HeapDumpOnOutOfMemoryError "

EXPOSE 8761

RUN mkdir /data
WORKDIR /data
VOLUME  /data

ADD ./@artifactId@-*-boot.jar /@artifactId@-@version@.jar
ENTRYPOINT java -server -noverify $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /@artifactId@-@version@.jar \
   --spring.profiles.active=staging  --dm.swarm-exec.path=/gocode/bin/swarm
