#!/bin/bash

export DOCKER_HOST=unix:///run/docker.sock
export DOCKER_REPO=ni1.codeabovelab.com

function deploy
{
        echo "Try to deploy to docker: " $2
        docker build -t ${DOCKER_REPO}/$2:$3 target
        docker tag ${DOCKER_REPO}/$2:$3 ${DOCKER_REPO}/$2:latest
        docker push ${DOCKER_REPO}/$2
}

deploy $1 $2 $3
exit

