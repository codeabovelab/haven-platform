#!/bin/bash

export DOCKER_HOST=unix:///run/docker.sock
export DOCKER_REPO=ni1.codeabovelab.com

function deploy
{
        echo "Try to deploy to docker: " $1
        docker build -t ${DOCKER_REPO}/$1:$2 target
        docker tag ${DOCKER_REPO}/$1:$2 ${DOCKER_REPO}/$2:latest
        docker push ${DOCKER_REPO}/$1
}

deploy $1 $2
exit

