FROM debian:stretch

MAINTAINER docker@codeabovelab.com

RUN apt-get update && apt-get install -y --no-install-recommends curl git procps golang openjdk-8-jdk-headless && apt-get clean

LABEL service-type=platform

LABEL arg.memory=300m
LABEL arg.restart=always
LABEL arg.port=8000
LABEL arg.publish=8000:8000

ENV JAVA_OPTS=" -Xms128M -Xmx256M -Xss256k -XX:-HeapDumpOnOutOfMemoryError -XX:+UseConcMarkSweepGC "

EXPOSE 8000
ADD ./@artifactId@-*-boot.jar /@artifactId@-@version@.jar
ENTRYPOINT java $JAVA_OPTS -Dspring.getenv.ignore=true -Djava.security.egd=file:/dev/./urandom  -jar /@artifactId@-@version@.jar