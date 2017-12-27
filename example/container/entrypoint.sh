#!/bin/bash

function configure-server(){
 
    cp /etc/catapult/server.yml.template /etc/server.yml
    
    #ZOOKEEPER_ADDRESS
    sed -i -e "s/{ZOOKEEPER_ADDRESS}/$ZOOKEEPER_ADDRESS/g" /etc/catapult/server.yml

    #REGISTRY_ENDPOINT
    sed -i -e "s/{REGISTRY_ENDPOINT}/$REGISTRY_ENDPOINT/g" /etc/catapult/server.yml

    #EXECUTOR_SERVICE
    sed -i -e "s/{EXECUTOR_SERVICE}/$EXECUTOR_SERVICE/g" /etc/catapult/server.yml

    #IMAGE_TAG
    sed -i -e "s/{IMAGE_TAG}/$IMAGE_TAG/g" /etc/catapult/server.yml

    #MESOS_LIBRARY_PATH
    sed -i -e "s/{MESOS_LIBRARY_PATH}/$MESOS_LIBRARY_PATH/g" /etc/catapult/server.yml
}

if [ $1 = "scheduler" ] ; then
    configure-server
    java -cp /usr/local/bin/sdk-example-1.0-SNAPSHOT.jar com.tbouvier.mesos.ExampleApplication /etc/catapult/server.yml
elif [ $1 = "executor" ] ; then
    java -cp /usr/local/bin/sdk-example-1.0-SNAPSHOT.jar com.tbouvier.mesos.executor.MyExecutableAppDriver
else
    echo "Unknown cmd: $1"
fi