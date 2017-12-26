package com.tbouvier.mesos.scheduler;

import com.tbouvier.mesos.ExampleConfiguration;
import com.tbouvier.mesos.api.Application;
import com.tbouvier.mesos.app.scheduler.AppDriver;
import com.tbouvier.mesos.app.scheduler.SchedulerApp;

/**
 * Created by bouviti on 12/26/17.
 */
public class MyApplication extends SchedulerApp{

    public final Application application;
    public final ExampleConfiguration configuration;

    public MyApplication(Application application, ExampleConfiguration configuration)
    {
        this.application = application;
        this.configuration = configuration;
    }

    @Override
    public void initialized(AppDriver appDriver, Protos.AppID appID){
        Protos.ContainerLiteInfo containerInfo = Protos.ContainerLiteInfo.newBuilder()
                .setDockerImage(this.configuration.getMyNodeExecutorImage())
                .build();

        for( int i = 0 ; i < this.application.getNumNodes() ; i++ ){
            Protos.SchedulerNodeInfo nodeInfo = Protos.SchedulerNodeInfo.newBuilder()
                    .setSchedulerContainer( Protos.SchedulerContainer.newBuilder()
                            .setName("node-"+i)
                            .setCpus(1.0)
                            .setMemory(1024)
                            .setExecutor(true)//if this is a custom executor or not
                            .setContainerLiteInfo(containerInfo)
                            .build())
                    .build();

            appDriver.launchNode(new MyNode(nodeInfo));
        }
    }

    @Override
    public void initFailed()
    {
        System.out.println("Oh no! app init failed!");
    }
}