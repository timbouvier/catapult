package com.tbouvier.mesos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by bouviti on 12/26/17.
 */
public class ExampleConfiguration extends Configuration {

    @NotEmpty
    private String zookeeperAddress;

    @NotEmpty
    private String registryEndpoint;

    @NotEmpty
    private String zookeeperRootNode;

    @NotEmpty
    private String schedulerFailoverTimeout;

    @NotEmpty
    private String myNodeExecutorImage;

    @NotEmpty
    private String mesosLibraryPath;


    @JsonProperty
    public String getZookeeperAddress()
    {
        return this.zookeeperAddress;
    }

    @JsonProperty
    public String getRegistryEndpoint()
    {
        return this.registryEndpoint;
    }

    @JsonProperty
    public String getSchedulerFailoverTimeout()
    {
        return this.schedulerFailoverTimeout;
    }

    @JsonProperty
    public String getZookeeperRootNode()
    {
        return this.zookeeperRootNode;
    }

    @JsonProperty
    public String getMyNodeExecutorImage()
    {
        return this.myNodeExecutorImage;
    }

    @JsonProperty
    public String getMesosLibraryPath()
    {
        return this.mesosLibraryPath;
    }

    @JsonProperty
    public void setMesosLibraryPath(String mesosLibraryPath)
    {
        this.mesosLibraryPath = mesosLibraryPath;
    }


    @JsonProperty
    public void setZookeeperAddress(String zookeeperAddress)
    {
        this.zookeeperAddress = zookeeperAddress;
    }

    @JsonProperty
    public void setRegistryEndpoint(String registryEndpoint)
    {
        this.registryEndpoint = registryEndpoint;
    }

    @JsonProperty
    public void setSchedulerFailoverTimeout(String schedulerFailoverTimeout)
    {
        this.schedulerFailoverTimeout = schedulerFailoverTimeout;
    }

    @JsonProperty
    public void setZookeeperRootNode(String zookeeperRootNode)
    {
        this.zookeeperRootNode = zookeeperRootNode;
    }

    @JsonProperty
    public void setMariaDBExecutorImage(String myNodeExecutorImage)
    {
        this.myNodeExecutorImage = myNodeExecutorImage;
    }

}
