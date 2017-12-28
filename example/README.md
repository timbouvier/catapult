# Example Application
This example uses the web framework dropwizard for api endpoints for the application deployer as well as docker for scheduler and executor component containers.

# Build
You can build the example application from the repo root or from this directory. The build will create a jar file that is packaged in a docker container. This container will be used for both the scheduler and executor components. 

#### From repo root
```shell
make example-docker
```

#### From this directory
```shell
make container
```

# Configuration
You'll need to configure a few environment variables specific to your cluster in the [environment file](./config/environment) file. A good practice is to store a file with cluster specific environment variables on each of your mesos clusters.

# Deployment
To deploy the example applicaton you can copy the source from [this directory](./) to a node in your mesos cluster and run the [deploy script](./scripts/deploy.sh).
```shell
cd scripts/
./deploy.sh
```
