package com.tbouvier.mesos.scheduler;

import com.tbouvier.mesos.AppFramework;
import com.tbouvier.mesos.ExampleConfiguration;
import com.tbouvier.mesos.MesosAppListener;
import com.tbouvier.mesos.api.Application;

/**
 * Created by bouviti on 12/26/17.
 */
public class MyFrameworkListener implements MesosAppListener{

    private boolean connected;
    private AppFramework appFramework;

    public void disconnected(AppFramework appFramework){
        System.out.println("Oh no! Disconnected!!");
    }

    public void connected(AppFramework appFramework){

        System.out.println("Framework now connected!");

        this.appFramework = appFramework;
        synchronized (this){
            this.connected = true;
            this.notify();
        }
    }

    public void applicationFailed(Protos.AppID appID){

    }

    public boolean isInitialized(){
        return this.connected;
    }

    public void create(Application application, ExampleConfiguration configuration){
        this.appFramework.register(new MyApplication(application, configuration));
    }

}
