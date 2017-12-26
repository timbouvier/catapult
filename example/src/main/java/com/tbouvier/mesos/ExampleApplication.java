package com.tbouvier.mesos;

import com.tbouvier.mesos.scheduler.MyAppDeployer;
import com.tbouvier.mesos.scheduler.MyFrameworkListener;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

/**
 * Created by bouviti on 12/26/17.
 */
public class ExampleApplication extends Application<ExampleConfiguration> {

    @Override
    public void run(ExampleConfiguration configuration, Environment environment)
    {
        //start app deployer and wait for it to init
        MyAppDeployer appDeployer = new MyAppDeployer(new MyFrameworkListener(), configuration);
        appDeployer.waitForInit();
    }
}
