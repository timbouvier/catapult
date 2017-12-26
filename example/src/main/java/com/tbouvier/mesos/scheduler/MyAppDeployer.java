package com.tbouvier.mesos.scheduler;

import com.tbouvier.mesos.ExampleConfiguration;
import com.tbouvier.mesos.MesosAppListener;
import com.tbouvier.mesos.api.Application;

/**
 * Created by bouviti on 12/26/17.
 */
public class MyAppDeployer {

    private final MyFrameworkListener listener;
    private final ExampleConfiguration configuration;

    public MyAppDeployer(MyFrameworkListener listener, ExampleConfiguration configuration)
    {
        this.listener = listener;
        this.configuration = configuration;
    }

    public void waitForInit()
    {
        synchronized (this.listener) {

            if( this.listener.isInitialized())
            {
                return;
            }

            try {
                this.listener.wait();
            } catch (Exception e) {
                System.out.println("Exception: "+e);
                e.printStackTrace();
            }
        }
    }

    public void create(Application application){
        this.listener.create(application, this.configuration);
    }
}
