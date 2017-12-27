#!/bin/bash

source ../config/environment

cp ../config/deploy.json.template ./deploy.sh

#MESOS_LIBRARY_PATH
sed -i -e "s/{MESOS_LIBRARY_PATH/$MESOS_LIBRARY_PATH/g}" deploy.json

#EXECUTOR_IMAGE
sed -i -e "s/{EXECUTOR_IMAGE}/$EXECUTOR_IMAGE/g" deploy.json

#EXECUTOR_TAG
sed -i -e "s/{EXECUTOR_TAG}/$EXECUTOR_TAG/g" deploy.json

#ZOOKEEPER_ADDRESS
sed -i -e "s/{ZOOKEEPER_ADDRESS}/$ZOOKEEPER_ADDRESS/g" deploy.json

#REGISTRY_ENDPOINT
sed -i -e "s/{REGISTRY_ENDPOINT}/$REGISTRY_ENDPOINT/g" deploy.json

#deploy using marathon internal
curl -X POST http://leader.mesos:8080/v2/apps -d @deploy.json -H "Content-type: application/json"
