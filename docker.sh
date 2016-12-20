#!/bin/bash

function deploy
{
        echo "Try to deploy to docker: " $2
        docker build -t codeabovelab/$2:$3 target
        docker push codeabovelab/$2:$3
}

docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
deploy $1 $2 $3

exit

